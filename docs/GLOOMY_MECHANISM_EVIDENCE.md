# Gloomy 1.1：已核对的加载链路与 KoLab 移植边界

来源：用户提供的 `gloomy_1.1.apk`、`gloomy-round2.zip`、`gloomy-round3.zip`，仅静态逆向与既有设备快照。此文档**不是**已完成移植的证明。

## 两条必须区分的功能链路

**A. Root / KernelSU 引导链**
- `Main.doBoot` 触发 `exploit-vivo fake`；Native stage / `whitelist` 分支与 `ksud late-load` 路由负责准备或建立特权环境。
- ELF `kw_insmod_allowlist` 的镜像处理路径在既有逆向结果中主要针对 `kernelsu.ko`；`kw_load_now` 通过内核模块系统调用进行加载，并结合模块存在性作成功判断。
- 因此，不能把 `exploit-vivo whitelist` 简化成“用户第三方文件直接传给该 ELF 就能加载”。

**B. 用户自选第三方 .ko 的加载链**
- `Main.pickFile/onActivityResult` 只导入保存 URI；`showPicker` 后才调用 `Main.loadKo`。
- `Main.loadKo` 要求 `suOk()`，并检查 Vivo BSP 的 `insko_dfd_on` 读取值严格为 `0`。
- 在源文件保存到临时目录后，Java 路径处理模块元信息、尝试普通模块加载、失败时重试；该版本还会先尝试卸载部分已加载模块，并在结尾调用一处硬编码的外部 Loader。
- 当前运行时快照有 KAIROS、KernelSU 模块，`insko_dfd_on=0`，但是**缺少一次成功加载的完整时间线**，不能唯一证明是 Java 加载还是旧外部 Loader / 更早操作完成。

## 对 KoLab 的直接工程含义

1. **移植目标不是复制一个 `insmod` 按钮**，也不是只做外部应用 Binder 服务。
2. 需先确认：当前设备上使常规模块加载可用的有效环境前提究竟是什么，特别是厂商接口状态与 Root/KernelSU 特权建立之间的关系。
3. 要实现用户所说的“常驻”：拆成服务常驻（随时接收调用）与能力持续（加载条件在不同进程乃至系统重启后的有效性）两个验收项；不能混为一谈。
4. 第三方已 Root 客户端的定位是**调用统一加载功能**，不要求 KoLab 为它们再获取 Root；但依然要防止无意将此服务变成未认证的整机 Shell。
5. 默认不卸载其他内核模块、不自动重试不同参数、不执行自解压 `.sh`。
6. 编译成功、API 可调用、Root 返回 `uid=0`，**都不等于 Gloomy 真实加载能力已移植成功**。

## M0 的验收证据

- 得到 Gloomy 真实加载路径的函数调用图（Java + ELF + stage），标明现有 Root 分支与需要初始化特权的分支；
- 提供与现有手机内核状态相一致的实测证据，能解释为何同设备同一模块常规途径失败而 Gloomy 成功（排除硬编码外部 Loader 的历史干扰）；
- 明确哪个步骤属于操作持久性、哪些只在本次内核生命周期有效；
- 在此之前，KoLab 后端必须明确显示“设备加载能力未验证”，不能宣称任意 `.ko` 都能刷入或对其他应用提供实际有效的高权限加载服务。
