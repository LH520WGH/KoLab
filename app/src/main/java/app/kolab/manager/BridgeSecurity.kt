package app.kolab.manager

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Process
import java.security.MessageDigest

/** A client is identified by its Binder UID and Android signing certificate. */
object BridgeSecurity {
    private const val PREF = "kolab_bridge_trust"
    private const val KEY = "trusted_clients"

    fun fingerprint(context: Context, packageName: String): String? = runCatching {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(packageName,
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= 28)
            info.signingInfo?.apkContentsSigners else info.signatures
        if (signatures == null || signatures.size != 1) return null
        MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }.getOrNull()

    fun granted(context: Context): Set<String> =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun isGranted(context: Context, packageName: String): Boolean {
        val hash = fingerprint(context, packageName) ?: return false
        return granted(context).contains("$packageName:$hash")
    }

    fun grant(context: Context, packageName: String) {
        val hash = fingerprint(context, packageName)
            ?: throw SecurityException("无法验证此应用签名")
        val values = granted(context).toMutableSet()
        values.add("$packageName:$hash")
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, values).apply()
    }

    fun revoke(context: Context, entry: String) {
        val values = granted(context).toMutableSet()
        values.remove(entry)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, values).apply()
    }

    /** Called inside EACH Binder transaction, not only at bind time. */
    fun enforce(context: Context) {
        val uid = Binder.getCallingUid()
        if (uid == Process.myUid()) return
        val packages = context.packageManager.getPackagesForUid(uid)
            ?: throw SecurityException("无法识别调用者 UID")
        // Avoid ambiguity of shared UID; clients with this setup are not supported.
        if (packages.size != 1 || !isGranted(context, packages[0])) {
            throw SecurityException("调用应用未获 KoLab 授权或其签名已更改")
        }
    }
}
