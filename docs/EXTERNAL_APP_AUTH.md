# KoLab — 外部应用授权中心（设计冻结草案）

## 目标
允许用户自己信任的 Android App 请求 KoLab 对选定的内核模块进行**导入、静态分析、加载申请**。只有经过独立审批的 KoLab 特权执行组件才可能执行加载；本设计不将 KernelSU Root 或任意 Shell 转交给调用方。

## 版本和状态
- V1 当前发布版：不提供跨 App 特权 API，不请求 Root，不加载驱动。
- V1.1：通过 Android 文件分享（ACTION_SEND / ACTION_SEND_MULTIPLE）进入待确认导入流程，展示真实文件名和静态诊断。
- V2：引入基于 Binder 的受控申请 API、签名绑定的可信应用注册表、请求队列与审计日志。默认关闭所有外部请求。
- V3：在合法已有的 KernelSU 权限与内核模块加载策略允许前提下接入最小化特权执行器。逐次批准已导入、经过兼容性检查的单个 SHA-256 文件；不自动跳过签名/策略限制、不调用旧的厂商漏洞程序。

## 授权模型
记录 {packageName, certificateSha256, allowedScopes, moduleHashes, enabled, grantedAt, lastUsedAt}；不能单靠包名或随意传入的 UID/Intent 字段。应用更新后必须重新检验签名。包重装或签名变化时拒绝请求。

Scopes:
- MODULE_LIST: 仅列出获授权的模块元信息
- REQUEST_IMPORT: 提交系统提供的 content:// 文件 URI，由 KoLab 复制到私有仓库并静态检查
- REQUEST_LOAD: 仅创建加载申请；不直接执行
- (reserved) PREAPPROVED_LOAD: 仅允许经用户单独批准的 moduleSha256 + 设备构建 + 固定操作；默认不启用

## 外部 IPC 规则
- 使用明确导出的绑定 Service / Binder 或其他经过认证的跨进程接口；没有实现鉴权前不导出。
- 必须在 **每次 Binder IPC 调用内**获取真实 Binder.getCallingUid，匹配 PackageManager.getPackagesForUid() 和应用签名证书；对不匹配的请求默认拒绝。
- 不在 onBind()、Intent extra 中信任调用者自报的包名。
- 首选 Android 签名级权限用于同开发者应用；第三方不同签名 App 采用证书指纹白名单和显式用户同意，不误用 normal/dangerous 权限代替身份验证。
- 在外部 IPC 的 Binder 线程上完成身份验证；不因异步排队丢失调用者身份上下文。
- 限制并发、请求大小、操作速率，防止恶意请求与资源耗尽。
- 不接受调用方提供的任意绝对路径、命令行、shell 参数、绕过内核安全配置的标志。
- 内容传输只接受用户可控制且可验证的 content URI，读取时限制大小、时长，复制到 KoLab 自己的私有目录，计算 SHA-256 并在提交操作前再次核对。
- 文件类型明确区分 .ko(ELF ET_REL)、归档、.sh、自解压启动器；**绝不自动执行脚本或嵌入 ELF**。

## UI
设置 > 授权中心
- 默认全部关闭
- 授权应用列表：图标/名称/真实包名/签名 SHA-256/允许权限/最后使用时间
- 新授权请求：显示发起 App、完整模块 SHA-256、解析内核构建、潜在风险与可选操作
- 允许本次 / 拒绝 / 进入 KoLab 检查；撤销权限、移除申请、清除队列
- 所有批准和拒绝写入本地审计日志，敏感操作默认要求每次确认

## 禁止设计
- 无认证端口服务或 TCP Root shell
- 任何 App 均可执行的导出 Service
- 任意命令执行 / 任意写入 /data/adb / 自动执行用户上传 .sh
- 仅凭包名就赋予高权限
- 不检查 ABI/vermagic/依赖/签名策略就尝试 insmod
- 自动卸载已有模块作为默认加载前步骤

## 交付标准
[ ] 普通 App 无法伪造可信调用方身份
[ ] 非白名单 App 的任何请求均会被拒绝
[ ] 白名单 App 也无法触发未获批准的模块加载
[ ] 受信任 App 提交的 .ko 或 .sh 不会在导入阶段执行
[ ] 用户撤销后后续请求立即失败
[ ] 审计日志能追溯请求源、文件哈希、审批结果
[ ] Root 缺失或内核不兼容时安全拒绝，不重试、不修改安全策略
