package app.kolab.manager

import android.content.Context
import android.os.ParcelFileDescriptor
import android.system.Os
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID

/** File inspection is read-only and never invokes su, insmod or a shell. */
object ModuleProbe {
    const val MAX_BYTES = 32L * 1024 * 1024

    fun inspect(context: Context, fd: ParcelFileDescriptor, name: String): String {
        val temp = File(context.cacheDir, "probe-" + UUID.randomUUID().toString())
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        try {
            ParcelFileDescriptor.AutoCloseInputStream(fd).use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (n == 0) continue
                        count += n
                        require(count <= MAX_BYTES) { "只读检查上限为 32 MiB" }
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                    }
                }
            }
            require(count > 0) { "文件为空" }
            val magic = ByteArray(4)
            FileInputStream(temp).use { it.read(magic) }
            val isElf = magic.contentEquals(byteArrayOf(127,69,76,70))
            val kind: String
            val state: String
            val detail: String
            if (isElf) {
                val info = KoInspector.inspect(temp, Os.uname().release)
                kind = info.kind
                state = info.verdict
                detail = info.notes + "; vermagic=" + (info.vermagic ?: "未知")
            } else {
                kind = if (magic[0] == '#'.code.toByte() && magic[1] == '!'.code.toByte())
                    "Shell 脚本 / 自解压入口" else
                    if (magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte())
                        "ZIP 归档" else "非 ELF 文件"
                state = "不是可直接载入的内核模块"
                detail = "本服务仅静态分析；不会执行、解包或调用第三方脚本"
            }
            val sha = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            return JSONObject().apply {
                put("protocol", 1)
                put("name", name.take(180))
                put("size", count)
                put("sha256", sha)
                put("kind", kind)
                put("verdict", state)
                put("notes", detail)
                put("loaded", false)
            }.toString()
        } finally {
            temp.delete()
        }
    }
}
