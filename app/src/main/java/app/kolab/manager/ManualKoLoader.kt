package app.kolab.manager

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.system.Os
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Uses fixed commands only; never accepts shell text from another app. */
object RootModuleExec {
    data class Result(val code:Int,val output:String,val timedOut:Boolean,val ms:Long){
        fun json():JSONObject=JSONObject().put("exitCode",code)
            .put("output",output.take(12000)).put("timedOut",timedOut).put("elapsedMs",ms)
    }
    private fun execute(command:String,timeout:Long):Result {
        val started=SystemClock.elapsedRealtime()
        var process:Process?=null
        try{
            process=ProcessBuilder("su","-c",command).redirectErrorStream(true).start()
            val proc=process
            val buffer=StringBuilder()
            val reader=Thread {
                runCatching {
                    proc.inputStream.bufferedReader().use { input ->
                        val chunk=CharArray(2048)
                        while(true){
                            val n=input.read(chunk)
                            if(n<0)break
                            if(buffer.length<15000)buffer.append(chunk,0,minOf(n,15000-buffer.length))
                        }
                    }
                }
            }
            reader.isDaemon=true
            reader.start()
            val finished=process.waitFor(timeout,TimeUnit.SECONDS)
            if(!finished)process.destroyForcibly()
            reader.join(900)
            return Result(if(finished)process.exitValue() else -1,buffer.toString(),
                !finished,SystemClock.elapsedRealtime()-started)
        }catch(e:Exception){
            return Result(-2,e.javaClass.simpleName+": "+e.message,false,
                SystemClock.elapsedRealtime()-started)
        }finally{runCatching{process?.destroy()}}
    }
    fun root():Result=execute("id -u",20)
    fun modules():Result=execute("cat /proc/modules",15)
    fun insmod(file:File):Result {
        val path=file.canonicalPath
        require(Regex("^/[a-zA-Z0-9_./-]+$").matches(path) && !path.contains("..") &&
            Regex("^kolab-[a-f0-9-]{36}\\.ko$").matches(file.name)){
            "内部文件路径验证失败"
        }
        return execute("/system/bin/insmod "+path,30)
    }
}

/** A one-shot, user-confirmed loader. Never called from the Binder bridge. */
object ManualKoLoader {
    private const val LIMIT=32L*1024*1024
    private const val FOLDER="kolab-once"
    data class Candidate(val file:File,val id:String,val sha:String,val name:String,
        val bytes:Long,val facts:KoInfo,val kernel:String)

    fun cleanStale(context:Context) {
        File(context.cacheDir,FOLDER).listFiles()?.forEach {
            if(it.name.matches(Regex("^kolab-[a-f0-9-]{36}\\.ko$")))it.delete()
        }
    }
    fun prepare(context:Context,uri:Uri,displayName:String):Candidate {
        require(uri.scheme=="content"){"只接受 Android 文件选择器"}
        require(displayName.lowercase().endsWith(".ko")){"只能提交 .ko 文件"}
        val id=UUID.randomUUID().toString()
        val folder=File(context.cacheDir,FOLDER).apply{mkdirs()}
        val file=File(folder,"kolab-"+id+".ko")
        val digest=MessageDigest.getInstance("SHA-256")
        var bytes=0L
        try {
            (context.contentResolver.openInputStream(uri)?:error("文件不可读")).use { input ->
                file.outputStream().use{out->
                    val chunk=ByteArray(65536)
                    while(true){
                        val n=input.read(chunk)
                        if(n<0)break
                        if(n==0)continue
                        bytes+=n
                        require(bytes<=LIMIT){"最大支持32MiB"}
                        digest.update(chunk,0,n)
                        out.write(chunk,0,n)
                    }
                }
            }
            require(bytes>=64){"不是完整 ELF 模块"}
            val kernel=Os.uname().release
            val info=KoInspector.inspect(file,kernel)
            require(info.kind=="ET_REL" && info.architecture=="AArch64"){
                "拒绝非 AArch64 ET_REL 文件："+info.notes
            }
            val sha=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
            return Candidate(file,id,sha,displayName.take(180),bytes,info,kernel)
        }catch(e:Exception){
            file.delete();throw e
        }
    }
    fun compatibilityError(c:Candidate):String? {
        val release=c.facts.vermagic?.substringBefore(" ")
        if(release.isNullOrEmpty())return "没有 vermagic，禁止尝试加载"
        if(release!=c.kernel)return "vermagic 不匹配："+release+" / "+c.kernel
        if(Os.uname().release!=c.kernel)return "内核版本变化，请重新选择模块"
        return null
    }
    private fun sha256(file:File):String{
        val hash=MessageDigest.getInstance("SHA-256")
        file.inputStream().use{it.copyTo(object:java.io.OutputStream(){
            override fun write(b:Int){hash.update(b.toByte())}
            override fun write(b:ByteArray,off:Int,len:Int){hash.update(b,off,len)}
        })}
        return hash.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
    }
    private fun moduleNames(r:RootModuleExec.Result):Set<String>?{
        if(r.code!=0 || r.timedOut)return null
        return r.output.lineSequence().map{it.substringBefore(" ").trim()}
            .filter{it.isNotEmpty()}.toSet()
    }
    fun load(c:Candidate):JSONObject{
        val out=JSONObject().apply{
            put("operationId",c.id)
            put("name",c.name);put("sha256",c.sha);put("bytes",c.bytes)
            put("kernel",c.kernel);put("vermagic",c.facts.vermagic)
            put("method","STANDARD_INSMOD")
            put("vivoBypass",false);put("functionalVerified",false)
        }
        try {
            val mismatch=compatibilityError(c)
            if(mismatch!=null)return out.put("state","BLOCKED_COMPATIBILITY").put("message",mismatch)
            val stageDir=File(c.file.parentFile.canonicalPath)
            if(stageDir.name!=FOLDER || c.file.canonicalFile.parentFile!=stageDir)
                return out.put("state","BLOCKED_PATH")
            if(sha256(c.file)!=c.sha)
                return out.put("state","BLOCKED_INTEGRITY").put("message","缓存文件SHA256发生变化")
            val root=RootModuleExec.root()
            out.put("rootProbe",root.json())
            if(root.code!=0 || root.output.trim()!="0")
                return out.put("state","ROOT_NOT_GRANTED").put("message","未获得 KernelSU Root 授权")
            val before=RootModuleExec.modules()
            val beforeNames=moduleNames(before)
            out.put("beforeModulesReadable",beforeNames!=null)
            val result=RootModuleExec.insmod(c.file)
            out.put("loadProcess",result.json())
            val after=RootModuleExec.modules()
            val afterNames=moduleNames(after)
            out.put("afterModulesReadable",afterNames!=null)
            val added=if(beforeNames!=null && afterNames!=null)
                afterNames-beforeNames else emptySet()
            out.put("observedNewModuleNames",JSONArray(added.sorted()))
            val state=when {
                result.timedOut -> "TIMEOUT_UNKNOWN"
                result.code!=0 -> "LOAD_FAILED"
                beforeNames==null || afterNames==null -> "RETURNED_ZERO_UNCONFIRMED"
                added.isEmpty() -> "RETURNED_ZERO_NO_NEW_MODULE"
                else -> "NEW_MODULE_OBSERVED_FUNCTION_UNVERIFIED"
            }
            return out.put("state",state)
                .put("message","没有运行厂商绕过、自动卸载或重复加载；请核对真实内核日志")
        }catch(e:Exception) {
            return out.put("state","EXECUTION_ERROR")
                .put("message",e.javaClass.simpleName+": "+e.message)
        }finally { c.file.delete() }
    }
}
