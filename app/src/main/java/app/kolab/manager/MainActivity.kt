package app.kolab.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

/** Main UI: user-confirmed standard Root module loading; external Binder stays read-only. */
class MainActivity:Activity(){
    private val pool=Executors.newSingleThreadExecutor()
    private val bg=Color.rgb(13,20,31)
    private val fg=Color.rgb(239,244,251)
    private val sub=Color.rgb(163,181,201)
    private val accent=Color.rgb(113,186,248)
    private lateinit var root:LinearLayout
    private var tab=0
    private var lastReport=""
    private var lastLoadReport=""
    private val density get()=resources.displayMetrics.density
    private fun dp(n:Int)=(n*density).toInt()
    private fun text(s:String,size:Float=15f,color:Int=fg)=TextView(this).apply{
        text=s;textSize=size;setTextColor(color);setPadding(dp(5),dp(9),dp(5),dp(9))
    }
    private fun button(s:String,action:()->Unit)=Button(this).apply {
        text=s;isAllCaps=false;textSize=14f;setTextColor(fg)
        background=GradientDrawable().apply {
            setColor(Color.rgb(28,43,62));cornerRadius=dp(11).toFloat()
        }
        setPadding(dp(9),dp(13),dp(9),dp(13));setOnClickListener{action()}
    }
    private fun add(body:LinearLayout,view:View){
        body.addView(view,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.statusBarColor=bg;window.navigationBarColor=bg
        root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)
            setPadding(dp(15),dp(16),dp(15),dp(10))
        }
        if(Build.VERSION.SDK_INT>=30){
            root.setOnApplyWindowInsetsListener{view,insets->
                val bars=insets.getInsets(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                view.setPadding(dp(15),bars.top+dp(12),dp(15),bars.bottom+dp(10))
                insets
            }
        }
        setContentView(root);root.requestApplyInsets()
        ManualKoLoader.cleanStale(this)
        render()
    }
    private fun render(){
        root.removeAllViews()
        root.addView(text("KoLab  /  BRIDGE",23f,accent))
        root.addView(text("0.4.0  ·  STANDARD ROOT TEST",11f,sub))
        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        scroll.addView(body)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        when(tab){
            0->{
                add(body,text("连接状态",21f))
                add(body,text("Binder 跨应用接口：就绪（只读）\n本机 Root 加载：可手动测试标准 insmod\nVivo 安全绕过：未集成\nGloomy 设备专用加载：仍未验证",15f))
                add(body,text("只有你在主程序中二次确认后才会尝试一次标准 insmod。不会运行 own127、修改 BSP/SELinux 或把 Root 操作开放给其他 App。",13f,sub))
                add(body,button("检查 KernelSU Root 是否可用"){checkRootAccess()})
                add(body,button("选择 .ko → 校验 → 手动尝试标准加载"){pickKoForLoad()})
                add(body,button("选择文件：只读 ELF / modinfo 分析"){pickFile()})
                if(lastLoadReport.isNotBlank()){
                    add(body,button("导出上次加载结果 JSON"){exportLoadResult()})
                    add(body,text("上次加载测试：\n"+lastLoadReport,12f))
                }
                add(body,button("查看 Binder 能力信息"){showCapabilities()})
                add(body,button("查看设备只读诊断"){tab=1;render();captureDiagnostic()})
            }
            1->{
                add(body,text("设备诊断",21f))
                add(body,text("分别记录内核、Android、页面大小及普通应用能否读取的 Vivo 状态节点。无权限读取会标记为不可观察，不能推断为安全门已关闭。",13f,sub))
                add(body,button("刷新诊断数据（不需要 Root）"){captureDiagnostic()})
                add(body,button("导出诊断 JSON"){exportReport()})
                add(body,text(if(lastReport.isBlank())"尚未获取诊断快照" else lastReport,13f))
            }
            2->{
                add(body,text("受信任应用",21f))
                add(body,text("其他应用可以调用 KoLab 的状态查询与静态检查。授权仅控制这些 IPC 能力，当前没有驱动加载接口。",13f,sub))
                add(body,button("＋ 授权另一款应用"){chooseClient()})
                val items=BridgeSecurity.granted(this).sorted()
                if(items.isEmpty())add(body,text("暂无已授权应用",14f,sub))
                items.forEach{entry->
                    add(body,button(entry.substringBefore(":")+
                        "\n签名 SHA-256："+entry.substringAfter(":").take(24)+"…\n点击撤销"){
                        AlertDialog.Builder(this).setTitle("撤销客户端授权？")
                            .setMessage(entry.substringBefore(":"))
                            .setPositiveButton("撤销"){_,_->BridgeSecurity.revoke(this,entry);render()}
                            .setNegativeButton("取消",null).show()
                    })
                }
            }
            else->{
                add(body,text("机制研究",21f))
                add(body,text("Gloomy 1.1 的资料显示，KernelSU 引导与用户 .ko 加载为两条链路。酷安作者的描述和 Java 代码中的设备前置检查、摘要处理及 insmod 尝试相吻合，但仍缺乏同一次操作的内核成功证据。",15f))
                add(body,text("注意：SHA-256 前 8 字节 = 64 bit = 16 个十六进制字符。摘要前缀不等于模块签名。",14f,sub))
                add(body,text("当前结论：厂商准备机制未验证；不能将门值显示 0、Root 成功或模块已存在，当作新加载成功。",14f))
                add(body,text("独立后端：NOT_VERIFIED\n默认不加载未知模块\n不自动卸载系统已有模块\n不执行未知 .sh 或外部 Loader",13f,sub))
            }
        }
        val navigation=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        listOf("服务","诊断","授权","研究").forEachIndexed{i,label->
            navigation.addView(button(label){tab=i;render()},LinearLayout.LayoutParams(0,dp(50),1f).apply{
                marginEnd=dp(4)
            })
        }
        root.addView(navigation)
    }
    private fun showCapabilities(){
        pool.execute{
            val details=JSONObject().apply {
                put("protocol",1)
                put("kernel",android.system.Os.uname().release)
                put("inspectSupported",true)
                put("loadModuleSupported",false)
                put("localManualStandardRootTestSupported",true)
                put("state",LoaderBackend.status().state.name)
                put("notes",LoaderBackend.status().reason)
            }.toString(2)
            runOnUiThread{if(!isFinishing)AlertDialog.Builder(this).setTitle("服务信息")
                .setMessage(details).setPositiveButton("确定",null).show()}
        }
    }
    private fun captureDiagnostic(){
        pool.execute{
            val value=runCatching{DeviceDiagnostics.capture().toString(2)}
                .getOrElse{"只读检查失败："+it.javaClass.simpleName}
            runOnUiThread{if(!isFinishing){lastReport=value;if(tab==1)render()}}
        }
    }
    private fun exportReport(){
        if(lastReport.isBlank()){
            Toast.makeText(this,"请先刷新诊断数据",Toast.LENGTH_SHORT).show();return
        }
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            type="application/json"
            putExtra(Intent.EXTRA_TITLE,"kolab-device-diagnostics.json")
        },52)
    }
    private fun checkRootAccess(){
        pool.execute{
            val result=RootModuleExec.root()
            val message="退出码："+result.code+"\n输出："+result.output+
                "\n\n仅查询 Root 身份，本操作不会加载模块。"
            runOnUiThread{if(!isFinishing)AlertDialog.Builder(this)
                .setTitle("KernelSU Root 检查").setMessage(message)
                .setPositiveButton("确定",null).show()}
        }
    }
    private fun pickKoForLoad(){
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="*/*"
        },53)
    }
    private fun exportLoadResult(){
        if(lastLoadReport.isBlank())return
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="application/json"
            putExtra(Intent.EXTRA_TITLE,"kolab-load-result.json")
        },54)
    }
    private fun handleLoadSelection(uri:android.net.Uri){
        pool.execute {
            val result=runCatching {
                var filename="selected.ko"
                contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->
                    if(c.moveToFirst())filename=c.getString(0)?:filename
                }
                ManualKoLoader.prepare(this,uri,filename)
            }
            runOnUiThread{
                if(isFinishing){
                    result.getOrNull()?.file?.delete();return@runOnUiThread
                }
                result.onFailure{e->
                    AlertDialog.Builder(this).setTitle("无法准备加载测试")
                        .setMessage(e.message?:"未知文件错误")
                        .setPositiveButton("确定",null).show()
                }
                result.onSuccess{candidate->
                    val rejected=ManualKoLoader.compatibilityError(candidate)
                    val details="文件："+candidate.name+
                        "\nSHA-256："+candidate.sha+
                        "\n文件大小："+candidate.bytes+
                        "\nELF："+candidate.facts.kind+" / "+candidate.facts.architecture+
                        "\nvermagic："+(candidate.facts.vermagic?:"缺失")+
                        "\n当前内核："+candidate.kernel+
                        "\n\n"+(rejected?:"测试仅调用标准 insmod 一次；加载内核模块可能导致崩溃或重启，请确认已经有数据备份。")
                    val dialog=AlertDialog.Builder(this).setTitle(
                        if(rejected!=null)"未通过兼容性校验" else "确认标准 Root 加载测试？")
                        .setMessage(details)
                        .setNegativeButton("取消"){_,_->candidate.file.delete()}
                        .setOnCancelListener{candidate.file.delete()}
                    if(rejected==null){
                        dialog.setPositiveButton("我确认，仅加载一次"){_,_->
                            pool.execute {
                                val summary=ManualKoLoader.load(candidate).toString(2)
                                runOnUiThread{if(!isFinishing){
                                    lastLoadReport=summary
                                    tab=0;render()
                                    AlertDialog.Builder(this).setTitle("真实加载尝试结果")
                                        .setMessage(summary).setPositiveButton("确定",null).show()
                                }}
                            }
                        }
                    }else dialog.setPositiveButton("知道了"){_,_->candidate.file.delete()}
                    dialog.show()
                }
            }
        }
    }
    private fun chooseClient(){
        @Suppress("DEPRECATION")
        val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply{
            addCategory(Intent.CATEGORY_LAUNCHER)
        },0).map{it.activityInfo.packageName to it.loadLabel(packageManager).toString()}
            .distinctBy{it.first}.filter{it.first!=packageName}.sortedBy{it.second}
        if(apps.isEmpty()){
            AlertDialog.Builder(this).setMessage("没有可选择的应用")
                .setPositiveButton("确定",null).show();return
        }
        AlertDialog.Builder(this).setTitle("选择受信任客户端")
            .setItems(apps.map{it.second+"\n"+it.first}.toTypedArray()){_,idx->
                val pkg=apps[idx].first
                val cert=BridgeSecurity.fingerprint(this,pkg)
                if(cert==null){
                    AlertDialog.Builder(this).setMessage("无法验证客户端签名")
                        .setPositiveButton("确定",null).show()
                }else{
                    AlertDialog.Builder(this).setTitle("确认跨应用授权")
                        .setMessage("应用："+pkg+"\n签名："+cert+
                            "\n\n仅允许查询和静态文件检查；不提供 Root 或驱动加载。")
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
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="*/*"
        },51)
    }
    @Deprecated("Android Activity result")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK)return
        val uri=data?.data?:return
        when(requestCode){
            51->pool.execute{
                val report=runCatching{
                    var name="selected-file"
                    contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->
                        if(c.moveToFirst())name=c.getString(0)?:name
                    }
                    val fd=contentResolver.openFileDescriptor(uri,"r")?:error("文件无法读取")
                    val result=ModuleProbe.inspect(this,fd,name)
                    JSONObject(result).toString(2)
                }.getOrElse{"静态检查失败："+it.message}
                runOnUiThread{if(!isFinishing)AlertDialog.Builder(this)
                    .setTitle("文件检查结果（不加载）")
                    .setMessage(report).setPositiveButton("关闭",null).show()}
            }
            53->handleLoadSelection(uri)
            52,54->pool.execute{
                val failure=runCatching{
                    val out=contentResolver.openOutputStream(uri)?:error("无法创建文件")
                    val dataToSave=if(requestCode==52)lastReport else lastLoadReport
                    out.bufferedWriter().use{it.write(dataToSave)}
                }.exceptionOrNull()
                runOnUiThread{
                    Toast.makeText(this,if(failure==null)"诊断报告已保存" else
                        "导出失败："+failure.message,Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onDestroy(){pool.shutdown();super.onDestroy()}
}
