# KoLab Bridge alpha — Android 客户端接入

**目前只实现真实 IPC 和只读预检，不具备 Gloomy 的加载能力。**

在客户端 app/src/main/aidl/app/kolab/manager/IKoLabBridge.aidl 放入与仓库同名同内容的 AIDL 文件；客户端须与 KoLab 约定协议版本。外部 App 必须由用户在 KoLab「授权」界面选择，签名哈希需与当前安装 APK 一致。

客户端 AndroidManifest.xml 可声明：
```xml
<queries><package android:name="app.kolab.manager" /></queries>
```

显式绑定：
```kotlin
val intent = Intent().setClassName(
  "app.kolab.manager",
  "app.kolab.manager.KoLabBridgeService"
)
bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
```

`ServiceConnection.onServiceConnected` 内：
```kotlin
val bridge = IKoLabBridge.Stub.asInterface(service)
val capabilitiesJson = bridge.capabilities
// 为静态检查可传入从用户授权的 content:// URI 获得的 ParcelFileDescriptor：
val detailsJson = bridge.inspectModule(fd, "hello.ko")
```

除非 KoLab 已授权客户端，服务端会抛出 SecurityException。此 alpha 的 AIDL 没有 load 或任意 Shell API；任何“加载成功”的承诺均属于误报。

验收：授权与撤销生效、跨 App Binder 状态查询成功、未授权请求被拒绝、文件检查不改动系统；设备加载后端暂不可用。
