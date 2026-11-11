# DSH Go 同步任务：Gloomy 加载链路证据整理（只读）

**目标**：从现有 Gloomy 反编译包和既有诊断日志中整理可审计的加载调用链，为 KoLab 的后端开发建立可信依据。

1. 不修改内核，不加载/卸载 .ko，不执行 exploit-vivo，不运行自解压 .sh，不改变 SELinux、KernelSU 或 BSP 设置。
2. 读取 gloomy-round2.zip 中 Java Main.java 的选文件、loadKo 调用路径；建立入口、条件、外部命令、返回码的对照索引，标注源码行号。
3. 对照 exploit-vivo 汇编/字符串，区分“准备 KernelSU 引导”与“用户指定驱动加载”；没有证据不作功能归因。
4. 从 gloomy-round3.zip 现有日志解释已知失败/成功状态是否存在可核对的调用链；缺乏可靠时间戳就明确写未知。
5. 对比目标机内核、Gloomy 内置 .ko 与 hello.ko 的 vermagic/ABI 信息，列出仅影响预检的信息。
6. 以 Markdown 输出 Evidence Matrix：已证实、推测、未知；每条附文件名/行号/日志时间戳；不输出绕过安全控制的利用步骤。
7. 明确指出常驻进程/服务存在，与设备内核接受特定 .ko 是两回事。

最终交付：一份 GLOOMY_LOADING_EVIDENCE.md，附具体文件和证据引用，并告诉 KoLab 哪些操作能安全复现、哪些尚无足够数据。
