package app.kolab.manager

/** Device-specific backend is deliberately disconnected until verified. */
enum class LoaderBackendState {
    NOT_VERIFIED,
    UNAVAILABLE,
    READ_ONLY_DIAGNOSTICS,
    ALREADY_PRESENT,
    LOAD_FAILED,
    LOADED_NEW,
    FUNCTIONAL_VERIFIED
}

data class BackendResult(
    val state:LoaderBackendState,
    val reason:String,
    val moduleSha256:String?=null,
    val kernelEventId:String?=null
)

/** Does not contain a privileged execution path. */
object LoaderBackend {
    fun status()=BackendResult(
        LoaderBackendState.NOT_VERIFIED,
        "Vivo 的第三方模块加载通路尚缺少可验证的执行证据；不允许声称或模拟加载成功"
    )
}
