package app.kolab.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.*
import java.util.concurrent.Executors

/** KoLab Bridge 0.2: a real, signed-client IPC prototype, not a working loader. */
class MainActivity : Activity() {
    private val pool = Executors.newSingleThreadExecutor()
    private val bg = Color.rgb(13,20,31)
    private val fg = Color.rgb(239,244,251)
    private val sub = Color.rgb(160,180,201)
    private val accent = Color.rgb(113,186,248)
    private lateinit var root: LinearLayout
    private var tab = 0
    private val density get() = resources.displayMetrics.density
    private fun dp(n:Int)=(n*density).toInt()
    private fun txt(s:String,size:Float=15f, color:Int=fg) = TextView(this).apply {
        text=s;textSize=size;setTextColor(color);setPadding(dp(4),dp(9),dp(4),dp(9))
    }
    private fun button(s:String,action:()->Unit)=Button(this).apply{
        text=s;textSize=14f;isAllCaps=false;setTextColor(fg)
        background=GradientDrawable().apply{
            setColor(Color.rgb(29,42,59));cornerRadius=dp(10).toFloat()
        }
        setPadding(dp(12),dp(12),dp(12),dp(12))
        setOnClickListener{action()}
    }
    private fun add(body:LinearLayout, child:android.view.View) {
        body.addView(child,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
    }
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        window.statusBarColor=bg
        window.navigationBarColor=bg
        root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)
            setPadding(dp(14),dp(14),dp(14),dp(14))
        }
        setContentView(root);render()
    }
    private fun render(){
        root.removeAllViews()
        root.addView(txt("KoLab  /  BRIDGE",24f,accent))
        root.addView(txt("SERVICE PROTOTYPE · 0.2.0",11f,sub))
        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        scroll.addView(body)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        when(tab){
            0 -> {
                add(body,txt("服务状态",21f))
                add(body,txt("跨应用 Binder 服务：已安装\n协议：IKoLabBridge v1\n模块只读预检：可用\nGloomy 加载机制：尚未移植\n真实驱动加载：不可用",15f))
                add(body,txt("此版本是可调用的 IPC 原型，不是能刷入驱动的版本。不会访问 Root、修改 /data/adb 或加载未知 .ko。",13f,sub))
                add(body,button("选择文件进行临时静态检查"){pickFile()})
                add(body,button("检查服务接口"){localStatus()})
                add(body,txt("阶段目标：验证已授权 App → KoLab IPC → 文件预检 → 结果反馈的完整链路。",13f,sub))
            }
            1 -> {
                add(body,txt("受信任应用",21f))
                add(body,txt("授权在本机保存且绑定 APK 签名。第三方必须主动适配 KoLab 的 Binder 协议。此版本仅开放查询和静态检查。",13f,sub))
                add(body,button("＋ 选择要授权的应用"){chooseClient()})
                val granted=BridgeSecurity.granted(this).sorted()
                if(granted.isEmpty()) add(body,txt("当前没有外部应用获授权",15f,sub))
                granted.forEach { entry ->
                    add(body,button(entry.substringBefore(":")+"\n签名 SHA256："+entry.substringAfter(":").take(18)+"…\n点击撤销") {
                        AlertDialog.Builder(this).setTitle("撤销授权？")
                            .setMessage(entry.substringBefore(":"))
                            .setPositiveButton("撤销"){_,_->
                                BridgeSecurity.revoke(this,entry);render()
                            }.setNegativeButton("取消",null).show()
                    })
                }
            }
            2 -> {
                add(body,txt("设备与验证边界",21f))
                add(body,txt("设备："+Build.MANUFACTURER+" "+Build.MODEL+
                    "\nAndroid："+Build.VERSION.RELEASE+
                    "\n内核："+Os.uname().release+
                    "\nCPU ABI："+Build.SUPPORTED_ABIS.joinToString(),14f))
                add(body,txt("Root 状态：未请求\n模块签名：未测试\nVivo 设备专用加载条件：未归因",14f,sub))
                add(body,txt("下一阶段：分析 Gloomy 实际调用链，再开发设备兼容适配器。",14f,sub))
            }
        }
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        listOf("服务","授权","设备").forEachIndexed{idx,title->
            nav.addView(button(title){tab=idx;render()},LinearLayout.LayoutParams(0,dp(48),1f).apply {
                marginEnd=dp(4)
            })
        }
        root.addView(nav)
    }
    private fun localStatus(){
        val status="协议 v1 就绪\n已授权客户端数："+BridgeSecurity.granted(this).size+
            "\n加载后端：NOT_VERIFIED（尚不可加载 .ko）"
        AlertDialog.Builder(this).setTitle("Bridge 状态").setMessage(status)
            .setPositiveButton("确定",null).show()
    }
    private fun chooseClient(){
        @Suppress("DEPRECATION")
        val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply{
            addCategory(Intent.CATEGORY_LAUNCHER)
        },0).map{it.activityInfo.packageName to it.loadLabel(packageManager).toString()}
            .distinctBy{it.first}.filter{it.first!=packageName}.sortedBy{it.second}
        if(apps.isEmpty()){
            AlertDialog.Builder(this).setMessage("系统没有返回可选择的应用。")
                .setPositiveButton("确定",null).show();return
        }
        val labels=apps.map{it.second+"\n"+it.first}.toTypedArray()
        AlertDialog.Builder(this).setTitle("选择可信客户端")
            .setItems(labels){_,index->
                val pkg=apps[index].first
                val fingerprint=BridgeSecurity.fingerprint(this,pkg)
                if(fingerprint==null){
                    AlertDialog.Builder(this).setMessage("此应用的签名无法单独验证，不能授权。")
                        .setPositiveButton("确定",null).show()
                }else {
                    AlertDialog.Builder(this).setTitle("确认授权？")
                        .setMessage("应用："+pkg+"\n签名 SHA-256："+fingerprint+
                            "\n\n仅允许查询 Bridge 状态和提交文件静态检查；无加载权限。")
                        .setPositiveButton("授权"){_,_->
                            runCatching{BridgeSecurity.grant(this,pkg)}
                                .onFailure{Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()}
                            render()
                        }.setNegativeButton("取消",null).show()
                }
            }.setNegativeButton("取消",null).show()
    }
    private fun pickFile(){
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE);type="*/*"
        },51)
    }
    @Deprecated("Android activity result bridge")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=51 || resultCode!=RESULT_OK)return
        val uri=data?.data?:return
        pool.execute {
            val result=runCatching {
                var name="selected-file"
                contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->
                    if(c.moveToFirst())name=c.getString(0)?:name
                }
                val fd=contentResolver.openFileDescriptor(uri,"r")?:error("无法打开文件")
                ModuleProbe.inspect(this,fd,name)
            }.getOrElse { "检查失败："+it.message }
            runOnUiThread{
                if(!isFinishing)AlertDialog.Builder(this)
                    .setTitle("静态检查（不加载）").setMessage(result)
                    .setPositiveButton("关闭",null).show()
            }
        }
    }
    override fun onDestroy(){pool.shutdown();super.onDestroy()}
}
