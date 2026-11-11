package app.kolab.clienttest

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.kolab.manager.IKoLabBridge
import org.json.JSONObject

/** Real Android Binder client running in a separate app process. */
class ClientActivity:Activity(){
    private lateinit var report:TextView
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val base=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(14,24,37))
            setPadding(24,32,24,24)
        }
        if(Build.VERSION.SDK_INT>=30){
            base.setOnApplyWindowInsetsListener{view,insets->
                val bars=insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(24,bars.top+16,24,bars.bottom+16)
                insets
            }
        }
        fun label(s:String,sp:Float)=TextView(this).apply{
            text=s;textSize=sp;setTextColor(Color.WHITE);setPadding(10,12,10,12)
        }
        base.addView(label("KoLab / BRIDGE TESTER",22f))
        base.addView(label("0.3.0 · 授权与只读诊断测试",13f))
        base.addView(Button(this).apply{
            text="查询 KoLab 服务与设备状态"
            setOnClickListener{connect(null)}
        })
        base.addView(Button(this).apply{
            text="选择文件，让 KoLab 静态分析"
            setOnClickListener{
                @Suppress("DEPRECATION")
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                    addCategory(Intent.CATEGORY_OPENABLE);type="*/*"
                },60)
            }
        })
        report=label("尚未查询。请先到 KoLab → 授权，添加 Tester。",14f)
        val scroll=ScrollView(this)
        scroll.addView(report)
        base.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(base);base.requestApplyInsets()
    }
    @Deprecated("Android framework callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==60 && resultCode==RESULT_OK)data?.data?.let{connect(it)}
    }
    private fun connect(uri:Uri?){
        report.text="正在连接 KoLab 服务…"
        val intent=Intent().setClassName("app.kolab.manager","app.kolab.manager.KoLabBridgeService")
        val conn=object:ServiceConnection {
            override fun onServiceConnected(name:ComponentName?,binder:IBinder?){
                Thread{
                    val value=runCatching{
                        val bridge=IKoLabBridge.Stub.asInterface(binder)
                        if(uri==null)JSONObject(bridge.capabilities).toString(2)
                        else{
                            var filename="module"
                            contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->
                                if(c.moveToFirst())filename=c.getString(0)?:filename
                            }
                            val fd=contentResolver.openFileDescriptor(uri,"r")?:error("无法打开选中文件")
                            try{JSONObject(bridge.inspectModule(fd,filename)).toString(2)}
                            finally{fd.close()}
                        }
                    }.getOrElse{"调用失败："+it.javaClass.simpleName+"\n"+it.message}
                    runOnUiThread{
                        report.text=value
                        runCatching{unbindService(this)}
                    }
                }.start()
            }
            override fun onServiceDisconnected(name:ComponentName?){report.text="KoLab 服务已断开"}
        }
        val ok=runCatching{bindService(intent,conn,Context.BIND_AUTO_CREATE)}.getOrDefault(false)
        if(!ok)report.text="无法绑定：请先安装 KoLab Bridge 主 APK"
    }
}
