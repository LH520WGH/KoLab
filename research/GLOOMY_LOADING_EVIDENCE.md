# GLOOMY_LOADING_EVIDENCE

## 0. 结论与研究边界

用途：为 KoLab 的设备加载后端提供证据；不涉及 Android 工程、Binder、客户端授权或 GitHub Actions 的实现改动。

**【已证实·静态】Gloomy 1.1 有两条必须分开的链：**

1. **KernelSU 引导链**：`Main.doBoot()` → `exploit-vivo fake` → stage → Native whitelist 或外部 ksud late-load，目标主要是 KernelSU、Root 与其用户态组件。
2. **用户驱动链**：文档 URI／内置 asset → `Main.loadKo()` → 已有 su Root 与厂商门值检查 → BSP 摘要写入 → shell `insmod` → 末尾额外尝试外部 Kairos loader。

**【未知】现有资料没有闭合“用户选中的具体模块字节 → 某次加载调用 → 对应内核接受事件 → 新模块及功能可用”的因果链。Root 成功、源码出现 insmod、快照存在 KAIROS，均不等于加载方法已经复现。**

本轮只读取既有 APK、证据包和日志，交叉核验 DEX／ELF；未运行 exploit-vivo、ksud、su、rootsh、外部 loader 或自解压脚本，未修改 Vivo 安全状态，未加载或卸载模块。只新建离线研究产物。

标签：

- **【已证实·静态】**：由本样本代码、DEX 或 ELF 控制流确认，非成功运行证明。
- **【已证实·历史】**：既有快照或日志内容，非本轮实时设备状态。
- **【推测】**：与资料一致，但尚缺直接因果证据。
- **【未知】**：不足以判断，禁止按成功处理。

## 1. 输入、完整性与复核方法

| 输入 | 本机真实路径 | SHA-256 |
|---|---|---|
| Gloomy 1.1 APK | [gloomy_1.1.apk](</sdcard/Download/gloomy_1.1.apk>) | `a66051081924a9e43776f4b0b6a10817b877fb237c2461c117c327a66da3be7a` |
| round2 归档 | [gloomy-round2.zip](</sdcard/Download/gloomy-round2.zip>) | `a4d02d6bd62a786a55b1c29d6a95a6066e81461a4aaa3a3a84f4d111144d1c1d` |
| round3 归档 | [gloomy-round3.zip](</sdcard/Download/gloomy-round3.zip>) | `5533bc62f4297c53bd8ba0f369ec81aa090c90e76d9e1c5bb0a030d84ed9a911` |

【已证实·静态】APK 为 `com.gloomy.blue`，versionName `1.1`、versionCode `2`，566,721 字节。其哈希与第二轮从设备读取的原 APK 相同；原始安装路径见 [r2/acquisition.json:1–10](</root/workspace/kolab-gloomy-research/evidence/r2/acquisition.json#L1-L10>)：

```text
/data/app/~~8yhGOU6iKdyXcvhL6r8tMA==/com.gloomy.blue-vLQoEpzJ_FGLZGKW0YNivA==/base.apk
```

【已证实·静态】round2、round3 自带清单分别核验 45、27 项，无缺项或哈希不匹配。清单只证明这些归档内文件的一致性，不是日志不可篡改证明。

【已证实·静态】本轮从该 APK 再次执行离线 `objdump -d`：exploit-vivo、su_daemon 的指令文本与归档一致，仅排除输出中的样本路径标题行。再次使用 apktool 解码，Main.smali 与归档逐字节一致，SHA-256 为 `e7d00ed0c66e2019fc975c660884d2717bb5d1a7f827313d70ea5044f07a1bb9`。

验证记录：[EVIDENCE_INPUTS.json](</root/workspace/kolab-gloomy-research/EVIDENCE_INPUTS.json>)、[STATIC_VERIFICATION.json](</root/workspace/kolab-gloomy-research/STATIC_VERIFICATION.json>)、[DEX_VERIFICATION.json](</root/workspace/kolab-gloomy-research/DEX_VERIFICATION.json>)。

**反编译限制**：JADX 对 `sha8`、`copyUri`、`asset`、`shizukuRun` 的异常处理／循环重构存在明显错误。涉及循环语义时以 smali 为准，不把反编译文本中的提前 return／throw 当成真实缺陷。

## 2. 证据路径索引

归档已复制到独立研究目录，原归档未修改。下列行号均为文本物理行号；Native 地址是 ELF VA，不是运行时内核地址。

| ID | 文件与定位方式 |
|---|---|
| J | [Main.java](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java>)；原 ZIP 成员 `gloomy-round2/java/sources/com/gloomy/blue/Main.java` |
| S | [Main.smali](</root/workspace/kolab-gloomy-research/evidence/r2/smali/smali/com/gloomy/blue/Main.smali>)；原 ZIP 成员 `gloomy-round2/smali/smali/com/gloomy/blue/Main.smali` |
| E | [原始 exploit-vivo 反汇编](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt>)；原 ZIP 成员 `gloomy-round2/elf/exploit-vivo.disassembly.txt` |
| EA | [辅助字符串注释版](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.annotated.txt>)；注释只作定位，原始指令以 E 为准 |
| AS | [APK assets stage](</root/workspace/kolab-gloomy-research/evidence/r2/apk/assets/drivers/ksu_stage.sh>) |
| ES | [ELF 内嵌 KSU stage](</root/workspace/kolab-gloomy-research/evidence/r2/elf/embedded_ksu_stage.txt>) |
| RS | [第三轮采集的现存 stage](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/ksu_stage.sh>)；不是本轮运行的脚本 |

单文件合并到仓库时，上述绝对路径仅供本机定位；ZIP 成员名、哈希及下文原始节选构成可移植索引。原始压缩包应与报告一并保留。

## 3. 按钮与入口：权限引导不等于驱动加载

【已证实·静态】类为 `com.gloomy.blue.Main`：

| 按钮／入口 | 方法链 | 证据 |
|---|---|---|
| 临时 ROOT | build 回调 → `doBoot()` → `m7lambda$doBoot$9…()` | [J:279–281](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L279-L281>)、[J:648–745](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L648-L745>) |
| 导入驱动 | `pickFile()` → `onActivityResult()`，添加列表，不立即加载 | [J:508–549](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L508-L549>) |
| 选择驱动 | `showPicker()` → `m15lambda$showPicker$8…()` → `loadKo(Ko)` | [J:592–645](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L592-L645>) |
| 执行命令 | `runCmd()` → `sh()` | [J:963–975](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L963-L975>) |

【已证实·静态】内置列表含 Kairos 和 TwT；导入项保存 `uri`、显示名和 `remote` 文件名。实际加载统一进入 `loadKo`，并不存在已确认的“导入任意 ko 后直接传给 exploit-vivo”的 Java 分支。

## 4. KernelSU 引导链

### 4.1 Java 的准备与停止条件

【已证实·静态】`doBoot` 工作方法：

1. 先包含删除若干旧 stage 日志／slide 文件的语句，再检测已有 su Root；已有 Root 时跳过引导。
2. 若看到 kernelsu 的 sysfs 条目但 su 不可用，提示授权后返回。
3. 无 su 且无 Shizuku 授权时返回。
4. `kernelMatched()` 不匹配时只打印警告，没有紧接着返回。
5. 暂存 exploit-vivo、kernelsu.ko、su_daemon 和 assets stage；准备外部 KernelSU 安装中的 ksud；调用 fake 分支。

证据：[J:658–735](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L658-L735>)。

关键原文：

```java
String[] strArr = {"exploit-vivo", "kernelsu.ko", "su_daemon"};
// J:693
log(sh("cd /data/local/tmp && ./exploit-vivo fake 2>&1; echo 退出码=$?"));
// J:735
```

这些是样本中的调用，不是本轮执行记录。

### 4.2 Native 的角色

【已证实·静态】`main`，VA `0xb4d0`：whitelist 参数分派进入 worker；fake 分支设置 `g_fake_stage`。worker 检查 real UID 和 effective UID 均为 0，并依赖保存的 slide 等数据；随后存在厂商防护控制字处理及 allowlist 加载路径。

证据：[E:129–142](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L129-L142>)、[E:231–248](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L231-L248>)、[E:580–615](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L580-L615>)、[E:2729–2763](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L2729-L2763>)。

【未知】控制字与 Vivo 防护实现的精确语义、全局影响及完整恢复行为，缺少相应内核端源码和加载前后证据。本报告不提供或执行该控制字修改方法。

### 4.3 stage 版本必须区分

| 版本 | 已证实的静态行为 | 证据 |
|---|---|---|
| AS：APK 资源脚本 | 先 whitelist；随后仍尝试 ksud install／late-load；最后尝试启动 su_daemon；日志使用 ksu_stage.out | [AS:14–44](</root/workspace/kolab-gloomy-research/evidence/r2/apk/assets/drivers/ksu_stage.sh#L14-L44>) |
| ES：ELF 普通 KSU 内嵌脚本 | direct whitelist 成功时可提前退出；否则 ksud late-load；日志使用 ksu.log；包含 SELinux 修改语句，末尾没有完整恢复流程 | [ES:2–58](</root/workspace/kolab-gloomy-research/evidence/r2/elf/embedded_ksu_stage.txt#L2-L58>) |
| RS：第三轮现存文件 | 内容与前两者均不同；使用 ksu2.out；寻找 ksud；含 kptr_restrict 写入尝试及结尾 Enforcing 写入语句 | [RS:1–60](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/ksu_stage.sh#L1-L60>) |

【已证实·静态】fake 的可达分支会调用 `write_ksu_script()`，VA `0xeb7c`，截断／重写同一个 stage 路径。因此 Java 复制了 AS，不等于后面执行的仍是 AS。另有 mu667 内嵌脚本包含 daemon 启动，不能把它与普通 fake 模式混为一谈。

证据：[E:1475–1482](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L1475-L1482>)、[E:3594–3638](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L3594-L3638>)、[stage-comparison.json](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/stage-comparison.json>)。

【未知】尚未确定哪一份 stage 对应当前 KAIROS／KernelSU 的最初加载事件。脚本存在、mtime 与代码内容均不是执行证明。

## 5. 用户自选第三方 .ko 的实际 Java 路径

### 5.1 控制流

【已证实·静态】`Main.loadKo(Ko)` 在工作队列中进入 `m11lambda$loadKo$11…()`：

| 步骤 | 原样本行为 | 已有证据的限度 |
|---|---|---|
| 权限检查 | `suOk()` 必须通过；否则返回 | `suId()` 仅以输出含 `uid=0` 判定；不是模块加载证明 |
| 厂商门值 | 读取 insko_dfd_on，必须严格等于字符串 `0` | 只是前置门值检查，不在这里主动打开门 |
| 文件取得 | 内置 asset 或文档 URI → stageDir 文件 | URI 名称不是模块内部名称；需独立检查模块字节 |
| 暂存路径 | 复制到 `/data/local/tmp/<文件名>` 并 chmod | 复制返回结果未严格核验；“已推送”日志不等于复制成功 |
| 原模块处理 | 尝试卸载 KAIROS／TwT／linyu | 带有真实副作用，不是纯加载探测；本轮未运行 |
| 厂商准备 | 写 khs_num；计算 SHA-256 前8字节；写 write_hash | Java 没有严密核验每次写入结果，内核端含义尚未完全确认 |
| 实际尝试 | 先无参数 insmod；非成功文本时带样本参数重试 | 没有取得本次目标调用的原始退出记录和内核因果日志 |
| 事后观察 | 固定 grep kairos／cap_K | 不是任意导入模块的通用成功判定 |
| 外部附加动作 | 末尾尝试外部 Kairos loader | 可混淆实际加载贡献；脚本正文目前缺失 |

完整证据：[J:758–840](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L758-L840>)；DEX 核对位置：[S:4649–5249](</root/workspace/kolab-gloomy-research/evidence/r2/smali/smali/com/gloomy/blue/Main.smali#L4649-L5249>)。

### 5.2 门值、摘要和加载语句原文

```java
if (!"0".equals(strTrim)) {
    log("[!] 门未打开（值为 " + strTrim + "），先点「临时 ROOT」");
    return;
}
// J:782–785
```

```java
sh("echo 1 > /sys/kernel/bsp_security/khs_num");
// J:809
String strOctal = octal(strSha8);
log("sha256[0:8] = " + strSha8);
sh("printf '" + strOctal + "' > /sys/kernel/bsp_security/write_hash");
String strSh = sh("insmod '" + str2 + "'; echo 退出码=$?");
// J:820–823
```

【已证实·静态】`sha8(File)` 对整个暂存文件做 SHA-256，只取摘要前8字节，输出16个十六进制字符；`octal()` 转为8个二进制字节的 shell 转义表示。它不是完整32字节摘要，也不是模块数字签名。

JADX 的哈希循环重构有误；正确循环和8字节截取由 [S:3122–3179](</root/workspace/kolab-gloomy-research/evidence/r2/smali/smali/com/gloomy/blue/Main.smali#L3122-L3179>) 确认。转换逻辑见 [J:1181–1189](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L1181-L1189>)。

【未知】写入后是否生效、允许列表生命周期／消费规则、影响范围、是否需要其他前置状态，均缺内核端与同次加载日志证明。不能把上述静态写法直接声明为 KoLab 已验证的设备后端。

【已证实·静态】本地摘要针对 stage 文件，加载读取的是临时目录中的另一个路径；缺少复制成功及两端字节同一性检查。现有源码也不足以保证加载瞬间文件没有被替换。

### 5.3 重试与结果判断的缺口

原文：[J:828–838](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L828-L838>)。

```java
if (!strSh.contains("退出码=0")) {
    String strSh2 = sh("insmod '" + str2
        + "' hide=0 hide_proc=0; echo 退出码=$?");
    log(strSh2);
    explain(strSh2);
    log("— 内核日志（真实原因）—");
    log(sh("dmesg | tail -25"));
}
```

【已证实·静态】这组重试参数是原样本的模块参数，不是通用模块加载选项；TwT 的 modinfo 使用另一组参数名，任意导入模块也未必接受它们。

【已证实·静态】`sh()/exec()` 合并输出、等待进程，却只返回字符串，不返回结构化 exit code／errno。追加的 shell 末尾状态也不是前面 insmod 的进程状态；只能从额外文本取值。[J:1054–1129](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L1054-L1129>)。

【已证实·静态】`explain()` 将若干错误字符串直接解释为厂商门禁或 ABI 问题，是提示逻辑，不是内核诊断证据；相同 EPERM／EACCES 还可能来自身份、capabilities、SELinux 或其他检查。[J:940–960](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L940-L960>)。

## 6. 外部 Kairos loader：独立且尚未闭合的分支

【已证实·静态】进入 loadKo 的主流程后，不论前述 insmod 成败，结尾仍会尝试：

```java
log(sh("cd /data/time 2>/dev/null && printf 'y\\n1\\n1\\n' | timeout 60 ./Kairos_Driver_Loader_Release_2.2.3.sh 2>&1 | tail -25"));
// J:838
```

条件是 `cd` 成功；它不是无条件成功执行脚本。目录错误被抑制，管道又只保留末尾输出，没有独立采集 loader 的可靠退出状态。脚本名称和输入序列也不证明它内部做了什么。

【已证实·历史】第三轮查询时，目录和脚本均不存在：

```text
ls: /data/time: No such file or directory
ls: /data/time/Kairos_Driver_Loader_Release_2.2.3.sh: No such file or directory
__STAT_RC__:1
```

来源：[r3/loader/metadata.txt:1–6](</root/workspace/kolab-gloomy-research/evidence/r3/loader/metadata.txt#L1-L6>)。

【未知】脚本是否历史存在、实际内容、引用组件、文件变换、权限要求及成功条件。缺失不证明从未运行，也不能把它的贡献自动归给 Java insmod。不得执行来源不明的同名脚本来补证据。

## 7. exploit-vivo 的模块加载函数：与 Java 路径的联系与区别

| 函数／ELF VA | 已证实的静态行为与证据 |
|---|---|
| `kw_bsp_read`／`0x10f60` | 拼接厂商 sysfs 路径、读取并去除换行。[E:5913–5959](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L5913-L5959>) |
| `kw_insmod_allowlist`／`0x11018` | 摘要自检、准备模块镜像、摘要计算、门值检查、BSP 写入、落盘构建镜像、调用 kw_load_now。Native 检查写入长度，与 Java 忽略写入返回不同。[E:5961–6191](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L5961-L6191>) |
| `kw_load_ko_image`／`0x116e8` | 默认读取 kernelsu.ko，有 KSU_KO_PATH 环境覆盖点；使用保存的 slide 和内置符号表处理 ELF 未定义符号，产生修改后的镜像。[E:6403–6647](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L6403-L6647>) |
| `kw_load_now`／`0x11ab8` | 实际尝试 init_module、finit_module；目标成功检查硬编码 kernelsu。[E:6649–6912](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L6649-L6912>) |
| `kw_module_loaded`／`0x11ed4` | 用已有 kernelsu 模块记录协助判断，而非追踪任意用户驱动。[E:6914–6959](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L6914-L6959>) |
| `kw_bsp_write`／`0x12d90` | 打开 BSP 节点写入并比较实际写入字节数。[E:7865–7920](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L7865-L7920>) |

【已证实·静态】Native 门值条件与 Java 不同：只在读取字符串恰好为 `1` 时阻断，不等价于 Java 的严格“必须为0”；读取失败也没有相同的严格状态要求。[E:6035–6078](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L6035-L6078>)。

【已证实·静态】在 AArch64 下，Native 先尝试 syscall 105（init_module）；失败后尝试 syscall 273（finit_module），先 flags=0，再 flags=2。前两次参数含 `preinit=0 allow_shell=1`，最后一次空参数重试。[E:6695–6747](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L6695-L6747>)、[参数常量表:21–27](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exec-argv-tables.txt#L21-L27>)。

**flags=2 是忽略 vermagic 的请求，不是关闭模块数字签名校验，也不保证内核接受。**

【已证实·静态】Java 用户驱动路径没有把 URI／目标路径传给这些 Native 函数，也没有设置 KSU_KO_PATH 的已确认调用。二者共享“厂商节点与模块加载”概念，不等于同一套输入与执行流程。Native 的镜像符号处理和 ignore-vermagic 重试，不能自动算到 Java 第三方 insmod 上。

【未知】KSU_KO_PATH 覆盖点理论上可换文件，但这不是 Gloomy 已实现或实测的通用第三方加载通道；也没有证明它适合任意模块。

### DIRECT_WHITELIST_LOADED 不足以证明什么

【已证实·静态】至少两个输出位置：

- allowlist 函数在 kw_load_now 返回0之后输出：[E:6153–6167](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L6153-L6167>)。
- hooks_defend 未找到而回落普通加载时，也输出同样文本：[E:1273–1324](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L1273-L1324>)。

同时，kw_load_now 即使系统调用非零，也可因 kernelsu 已存在而返回0；证据为 [E:6796–6827](</root/workspace/kolab-gloomy-research/evidence/r2/elf/exploit-vivo.disassembly.txt#L6796-L6827>)。

因此该文本不唯一证明白名单被改、不证明新加载了此前不存在的模块，更不证明用户选中的 KAIROS 已加载。归档中没有取得本次目标加载对应的这个运行标记。

## 8. 设备与 ABI 条件

【已证实·静态】`kernelMatched()` 只检查 `contains("6.6.127")`；doBoot 不匹配时仅警告；`payloadDir()` 取 assets 下第一个目录。已审查 Java 入口没有基于 V2408A 完整型号、build、KMI、页大小、导出符号及签名策略进行完整匹配。

证据：[J:683–692](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L683-L692>)、[J:990–992](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L990-L992>)、[J:1312–1321](</root/workspace/kolab-gloomy-research/evidence/r2/java/sources/com/gloomy/blue/Main.java#L1312-L1321>)。

模块的真实 modinfo：

| APK 模块 | 内部名称 | vermagic 的 release 部分 |
|---|---|---|
| 内置 Kairos | KAIROS | `6.6.127-4k-g46a034eca005-dirty` |
| 内置 TwT | TwT | `6.6.127-4k-g46a034eca005-dirty` |
| 内置 KernelSU | kernelsu | `6.6.127-android15-8-g3b723d0e8507-abogki558077879-4k` |

来源：[Kairos modinfo](</root/workspace/kolab-gloomy-research/evidence/r2/elf/cap_K_KAIROS.ko.modinfo.txt>)、[TwT modinfo](</root/workspace/kolab-gloomy-research/evidence/r2/elf/twt.ko.modinfo.txt>)、[KernelSU modinfo](</root/workspace/kolab-gloomy-research/evidence/r2/elf/kernelsu.ko.modinfo.txt>)。

【已证实·静态】三份 APK 模块尾部均未见标准 Linux appended-signature 魔数；这不排除其它厂商验证格式，也不证明设备接受了这些模块。vermagic、modversions／符号 ABI、标准签名及 BSP 摘要检查应分别评估。

【未知】当前内存中的 KAIROS 是否就是该内置文件、是否经过外部 loader 修改，以及 V2408A 本次内核实际使用的验证组合。现有证据不支持推广为“所有 Vivo／所有6.6.127均可加载”。

## 9. 模块结果与历史日志的对应关系

### 9.1 已有快照证明的是存在，不是来源

【已证实·历史】round2 模块快照：

```text
KAIROS 151552 0 - Live 0x0000000000000000
kernelsu 180224 4 - Live 0x0000000000000000 (O)
```

来源：[r2/runtime/proc_modules.txt:1–2](</root/workspace/kolab-gloomy-research/evidence/r2/runtime/proc_modules.txt#L1-L2>)。采样时间为 2026-11-11 18:53:43 CST 附近，见 [collection.json:10–40](</root/workspace/kolab-gloomy-research/evidence/r2/runtime/collection.json#L10-L40>)。

【已证实·历史】round3 仍含两个模块，同时出现 kernelsu 的 sysfs 路径未找到而模块列表存在的视图差异：[module-summary.txt](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/module-summary.txt>)、[current-state.txt:19–27](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/current-state.txt#L19-L27>)。不据此单独认定隐藏、卸载或加载来源。

**这些快照不是加载前后对照。模块名相同、Live、引用计数或运行时大小，都不足以绑定到一个 APK 内文件哈希。**

【已证实·历史】状态快照曾见 Root helper 的 UID0／ksu 域、Enforcing 接口结果、门值0、khs_num=1、modules_disabled=0。Root helper 的身份不是 Gloomy 进程 `suId()` 成功的原始回显，更不是 KoLab 将来执行身份的证明。[r2/state.raw.txt:1–9](</root/workspace/kolab-gloomy-research/evidence/r2/runtime/state.raw.txt#L1-L9>)。

### 9.2 保存日志中的真实命中

【已证实·历史日志内容】round3 保存的目标相关片段包括：

```text
26763:<6>[  134.777603] KernelSU: late load mode, skipping kprobe hooks
26764:<6>[  134.777604] KernelSU: SELinux permissive or disabled, apply rules!
26783:<6>[  134.875843] KernelSU: backup sepolicy success! latest_granting=1
37681:<6>[  135.305487] KernelSU: Permissive SELinux, enforcing
40339:<6>[  147.466418] KernelSU: selinux_hide: set to 1
```

来源：[policy-history-excerpts.txt:6–17](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/policy-history-excerpts.txt#L6-L17>)。行首数字为原日志行号，方括号为相对启动时间。

另有一条部分 logcat 输出：

```text
869:11-09 19:35:56.217  1674  1674 I write_khs: write_hash ok: 442 hashes
```

来源：[logcat.log.matches.txt:1](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/logcat.log.matches.txt#L1>)。

【已证实·历史】保存文件 mtime 在2026-11-08/09附近，与11-11采样不是自动闭合的时间线。缺少同次 boot 标识、调用者与模块哈希。不能从442与当前计数1推导同一次操作的先后变化。

【已证实·取证限制】两份保存 dmesg 的筛选有完成标记；logcat 的全量筛选未取得结束标记，只有部分输出。不能将其当成完整零命中。见 [log-review.md:3–9](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/log-review.md#L3-L9>)。

### 9.3 尚未取得的关键运行证据

- 【未知】用户选择的具体第三方模块完整 SHA-256 与加载瞬间文件同一性。
- 【未知】对应的 insmod 原始 stdout、stderr、真实退出状态及内核 errno。
- 【未知】同一加载事件的 BSP 接受／阻断记录与模块初始化事件。
- 【未知】调用前模块不存在、调用后新增及功能验证的连续证据。
- 【未知】外部 loader 是否在该事件中执行及贡献了哪些文件／状态变化。

已有 stage 日志缺失记录见 [r2/verification.txt:1–2](</root/workspace/kolab-gloomy-research/evidence/r2/runtime/verification.txt#L1-L2>) 及 [r3/additional-metadata.txt:14–15](</root/workspace/kolab-gloomy-research/evidence/r3/runtime/additional-metadata.txt#L14-L15>)。日志缺失不证明成功或失败，也不允许通过重新运行利用程序制造历史证据。

## 10. 安全状态解释：不要把当前值当成完整恢复证明

【已证实·静态】样本存在全局 SELinux／厂商防护状态相关写入，以及 BSP 摘要写入。已审查的用户驱动路径未形成保存旧值并完整恢复的闭环。

【已证实·历史日志内容】保存日志包含策略操作、fake status 和 selinux_hide 相关记录；这些是组件报告，不是独立的内核真值证明。

【推测】状态接口的可观察结果可能受模块功能和执行环境影响；Enforcing 读数、门值0、Root身份、模块可见性，均应分别记录。

【未知】实际策略是否完整恢复、所有受影响保护是否恢复、标准模块签名强制状态是否改变。Native flags=2、BSP 摘要写入或 policydb 符号名，都不能单独证明这些变化。

KoLab 不应把“为了加载，自动关闭 SELinux／重跑引导 exploit／执行未知外部脚本”作为默认后端行为。

## 11. 对 KoLab 后端的直接约束

以下为【设计建议】，不是已完成实现或设备实测结果。

### 11.1 四层能力应分开

1. **授权执行层**：确认实际 Android UID、SELinux 域与执行上下文；Binder 请求成功或 shell 文本含 uid=0，不等于执行器已具备目标设备加载能力。
2. **设备准备层**：识别厂商节点、内核 build／ABI、当前门值和所需准备条件；未知条件返回“不支持／尚未验证”，不伪装成可加载。
3. **加载层**：绑定确定的模块字节与确定的调用，保留真实错误；不静默重试未知 loader 或不同模块参数。
4. **结果证明层**：区分已存在、新加载、调用失败和功能通过；进程结果、内核事件与模块功能分别取证。

原生 Android KoLab 与 DSH 的 PRoot 不是同一执行环境。不能将 DSH 的 Root 通道成功或原生 su 兼容性问题直接外推给 KoLab。

### 11.2 最低证据账本

建议每个未来获批准的加载操作至少绑定：

- 唯一操作 ID、boot 标识、墙钟与单调时间。
- 请求方身份、执行方真实 UID／域、调用路径。
- 完整内核 release、架构、页大小、关键验证能力。
- 用户选择来源、不可变模块副本／FD、完整 SHA-256、内部模块名、vermagic、参数。
- 准备前后的安全状态；每步真实返回值，不以 Root helper 或管道尾命令的0代替。
- 加载调用的 stdout／stderr／errno，与相应内核事件。
- 调用前后模块差异；已存在须单独返回 ALREADY_PRESENT，而非新加载成功。
- 独立的模块功能证据，不能仅查名字。

可用状态示例：`ROOT_ONLY`、`PREPARE_UNVERIFIED`、`LOAD_ATTEMPTED`、`ALREADY_PRESENT`、`LOAD_FAILED`、`LOADED_NEW`、`FUNCTIONAL_VERIFIED`、`UNKNOWN`。

### 11.3 当前可交付与尚未交付

【已证实·静态】现在可以交付 Gloomy 的入口、参数传递、门值条件、摘要格式、Native 加载函数及失败归因缺口。

【未知】现在尚不能把“厂商准备步骤已独立复现”或“KoLab 已有 V2408A 可用加载后端”标为完成。普通 Root insmod 界面至多覆盖上述第三层的一部分。

## 12. 优先补证清单与验收门槛

下一阶段优先收集已有材料，不执行未知脚本：

1. 外部 Kairos loader 的可信原件、哈希、引用文件及内容；仅静态解析自解压部分。
2. 与目标设备 build 对应的 BSP／保护实现资料，确认门值、计数和摘要接口的真实语义及生命周期。
3. 真正被选中／被加载的模块文件及完整哈希，而非仅同名 APK asset。
4. 原始用户操作日志、真实退出码、同次启动内核事件与前后快照。
5. 若后续另行批准动态验证，单次只验证一条明确路径，避免末尾外部 loader 或既有模块混淆因果；本轮不进行这一步。

**设备加载后端的验收至少应同时满足：授权身份可靠、模块字节绑定、设备条件满足、真实调用结果、内核接受事件、加载前后变化及功能验证。任何一项缺失，应保留未验证状态。**

## 13. GitHub 交付状态

目标仓库：`https://github.com/LH520WGH/KoLab`；拟保存分支：`research/gloomy-dsh`。

本轮 GitHub API 返回404；本机未安装 gh，Git 在禁止交互登录的读取中返回：

```text
fatal: could not read Username for 'https://github.com': terminal prompts disabled
```

这表示当前会话没有可用的该仓库读取凭据，不证明仓库不存在。未尝试其它账号登录，未创建远端分支、commit 或 push，未修改 main。

本报告按单文件方式交付，由工程负责人核对后合并到研究分支；不包含执行载荷、设备策略补丁或 Android 工程改动。