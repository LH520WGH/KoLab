package app.kolab.manager

import android.os.Build
import android.system.Os
import android.system.OsConstants
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Read-only compatibility snapshot. Does not request Root, execute code,
 * write sysfs, change SELinux or attempt module loading.
 */
object DeviceDiagnostics {
    private fun readNode(path: String): JSONObject {
        val item=JSONObject().put("path",path)
        val file=File(path)
        if(!file.exists()) return item.put("state","NOT_PRESENT")
        return try {
            val bytes=ByteArray(256)
            val len=FileInputStream(file).use { it.read(bytes) }
            if(len<0) item.put("state","EMPTY")
            else {
                val s=String(bytes,0,len,Charsets.UTF_8).trim().take(180)
                item.put("state","READABLE").put("value",s)
            }
        }catch(e:SecurityException) {
            item.put("state","ACCESS_DENIED")
        }catch(e:Exception){
            item.put("state","UNREADABLE").put("reason",e.javaClass.simpleName)
        }
    }

    fun capture():JSONObject {
        val gate=readNode("/sys/kernel/bsp_security/insko_dfd_on")
        val count=readNode("/sys/kernel/bsp_security/khs_num")
        val disabled=readNode("/proc/sys/kernel/modules_disabled")
        val selinux=readNode("/sys/fs/selinux/enforce")
        val gateState=when {
            gate.optString("state")!="READABLE" -> "UNOBSERVABLE"
            gate.optString("value")=="0" -> "ZERO_OBSERVED_NOT_LOAD_PROOF"
            else -> "NONZERO_OR_UNEXPECTED"
        }
        val present=readNode("/proc/modules")
        return JSONObject().apply{
            put("schema","kolab-device-diagnostics-v1")
            put("device",Build.MANUFACTURER+" "+Build.MODEL)
            put("android",Build.VERSION.RELEASE)
            put("sdk",Build.VERSION.SDK_INT)
            put("kernelRelease",Os.uname().release)
            put("abis",Build.SUPPORTED_ABIS.joinToString(","))
            put("pageSize",runCatching{Os.sysconf(OsConstants._SC_PAGESIZE)}.getOrDefault(-1L))
            put("observation",JSONObject().apply {
                put("vendorGate",gate)
                put("vendorCount",count)
                put("modulesDisabled",disabled)
                put("selinuxEnforce",selinux)
                put("moduleListState",present.optString("state"))
            })
            put("vendorGateInterpretation",gateState)
            put("rootRequested",false)
            put("kernelModuleLoadSupported",false)
            put("vendorMethodVerified",false)
            put("state","DIAGNOSTICS_ONLY")
            put("warning","只读观察结果不是内核加载许可或实际加载成功证明")
        }
    }
}
