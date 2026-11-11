package app.kolab.manager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.system.Os
import org.json.JSONObject

/** Publicly bindable, but ALL methods are enforced against signed client grants. */
class KoLabBridgeService : Service() {
    private val bridge = object : IKoLabBridge.Stub() {
        override fun getCapabilities(): String {
            BridgeSecurity.enforce(this@KoLabBridgeService)
            return JSONObject().apply {
                put("protocol", 1)
                put("package", packageName)
                put("kernel", Os.uname().release)
                put("rootMode", "NOT_REQUESTED")
                put("gloomyPort", "NOT_VERIFIED")
                put("loadModuleSupported", false)
                put("inspectSupported", true)
                put("status", "BRIDGE_READY_READ_ONLY")
            }.toString()
        }

        override fun inspectModule(moduleFd: ParcelFileDescriptor?, displayName: String?): String {
            BridgeSecurity.enforce(this@KoLabBridgeService)
            requireNotNull(moduleFd) { "必须提供文件描述符" }
            return ModuleProbe.inspect(this@KoLabBridgeService, moduleFd, displayName ?: "module")
        }
    }

    override fun onBind(intent: Intent?): IBinder = bridge
}
