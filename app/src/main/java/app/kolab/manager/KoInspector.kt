package app.kolab.manager

import android.os.Build
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

data class KoInfo(val architecture: String, val kind: String, val vermagic: String?,
                  val license: String?, val depends: String?, val verdict: String, val notes: String)

object KoInspector {
    private fun validRange(length: Long, offset: Long, size: Long): Boolean =
        offset >= 0 && size >= 0 && offset <= length && size <= length - offset

    fun inspect(file: File, release: String): KoInfo {
        val reject = { message: String -> KoInfo("未知","未知",null,null,null,"拒绝",message) }
        if (!file.isFile || file.length() !in 64..134217728) return reject("文件过小、过大或不存在")
        return try {
            RandomAccessFile(file, "r").use { raf ->
                fun readAt(offset: Long, size: Int): ByteArray? {
                    if (size > 1048576 || !validRange(raf.length(),offset,size.toLong())) return null
                    return ByteArray(size).also { raf.seek(offset); raf.readFully(it) }
                }
                val header = readAt(0,64) ?: return reject("ELF 头不完整")
                if (!(header[0]==127.toByte() && header[1]==69.toByte() &&
                            header[2]==76.toByte() && header[3]==70.toByte())) return reject("不是 ELF 文件")
                if (header[4]!=2.toByte() || header[5]!=1.toByte()) return reject("仅支持 ELF64 小端")
                val h=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                val kind=h.getShort(16).toInt() and 65535
                val arch=h.getShort(18).toInt() and 65535
                val offset=h.getLong(40)
                val entsize=h.getShort(58).toInt() and 65535
                val count=h.getShort(60).toInt() and 65535
                val strIndex=h.getShort(62).toInt() and 65535
                var mod: ByteArray?=null
                if (entsize==64 && count in 1..4096 && strIndex<count &&
                    validRange(raf.length(),offset,count.toLong()*64)) {
                    val sections=(0 until count).map { i ->
                        val s=ByteBuffer.wrap(readAt(offset+i*64L,64)!!).order(ByteOrder.LITTLE_ENDIAN)
                        longArrayOf(s.getInt(0).toLong() and 0xffffffffL,s.getLong(24),s.getLong(32))
                    }
                    val st=sections[strIndex]
                    val names=if (st[2] in 1..1048576) readAt(st[1],st[2].toInt()) else null
                    if (names!=null) for(s in sections) {
                        val k=s[0].toInt()
                        if (k<0 || k>=names.size) continue
                        val end=(k until names.size).firstOrNull{names[it]==0.toByte()} ?: continue
                        val name=String(names,k,end-k,StandardCharsets.US_ASCII)
                        if (name==".modinfo" && s[2] in 1..524288) { mod=readAt(s[1],s[2].toInt()); break }
                    }
                }
                val data=mutableMapOf<String,String>()
                mod?.let { bytes ->
                    for(part in String(bytes,StandardCharsets.UTF_8).split('\u0000')) {
                        val sep=part.indexOf('=')
                        if (sep in 1..48) data.putIfAbsent(part.substring(0,sep),part.substring(sep+1).take(2048))
                    }
                }
                val vermagic=data["vermagic"]
                val mismatch=vermagic?.substringBefore(" ")?.let{it!=release} ?: false
                val blocked=kind!=1 || arch!=183 || mismatch
                val notes=buildList {
                    if(kind!=1) add("ELF 不是 ET_REL")
                    if(arch!=183) add("不是 AArch64")
                    if(mismatch) add("vermagic 与内核 release 不匹配")
                    if(vermagic==null) add("缺失 vermagic")
                    add("未验证签名、符号 CRC、KMI、页面大小、依赖与初始化安全性")
                }.joinToString("；")
                KoInfo(if(arch==183)"AArch64" else "e_machine=$arch",
                    if(kind==1)"ET_REL" else "e_type=$kind",vermagic,data["license"],data["depends"],
                    if(blocked)"拒绝" else "需人工复核",notes)
            }
        } catch(e:Exception) { reject("解析异常：${e.javaClass.simpleName}") }
    }
}
