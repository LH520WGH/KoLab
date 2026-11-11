package app.kolab.clienttest

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.kolab.manager.IKoLabBridge

/** Test ONLY. Calls the KoLab Binder from a separate Android UID. */
class ClientActivity:Activity() {
 private lateinit var report:TextView
 override fun onCreate(state:Bundle?) {
  super.onCreate(state)
  val base=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   setPadding(24,36,24,24)
   setBackgroundColor(Color.rgb(14,24,37))
  }
  report=TextView(this).apply {
   text="初始状态：未连接。先在 KoLab 授权此测试应用。"
   textSize=16f;setTextColor(Color.WHITE);setPadding(12,24,12,24)
  }
  base.addView(TextView(this).apply{
   text="KoLab / BRIDGE TESTER"
   textSize=23f;setTextColor(Color.WHITE)
  })
  base.addView(Button(this).apply{
   text="查询 KoLab 服务状态"
   setOnClickListener{connect(null)}
  })
  base.addView(Button(this).apply{
   text="选择文件并请求 KoLab 静态分析"
   setOnClickListener{
    @Suppress("DEPRECATION")
    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
     addCategory(Intent.CATEGORY_OPENABLE);type="*/*"
    },60)
   }
  })
  val scroll=ScrollView(this)
  scroll.addView(report)
  base.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  setContentView(base)
 }
 @Deprecated("Android activity result bridge")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==60 && resultCode==RESULT_OK) data?.data?.let{connect(it)}
 }
 private fun connect(uri:Uri?){
  report.text="连接中…"
  val intent=Intent().setClassName("app.kolab.manager","app.kolab.manager.KoLabBridgeService")
  val conn=object:ServiceConnection{
   override fun onServiceConnected(name:ComponentName?,binder:IBinder?){
    Thread {
     val result=runCatching{
      val bridge=IKoLabBridge.Stub.asInterface(binder)
      if(uri==null)bridge.capabilities else {
       var label="module"
       contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->
        if(c.moveToFirst())label=c.getString(0)?:label
       }
       val fd=contentResolver.openFileDescriptor(uri,"r")?:error("文件无法打开")
       bridge.inspectModule(fd,label)
      }
     }.getOrElse{e->"调用失败：${e.javaClass.simpleName}\n${e.message}"}
     runOnUiThread{
      report.text=result
      runCatching{unbindService(this)}
     }
    }.start()
   }
   override fun onServiceDisconnected(name:ComponentName?){report.text="Bridge 已断开"}
  }
  val ok=runCatching{bindService(intent,conn,Context.BIND_AUTO_CREATE)}.getOrDefault(false)
  if(!ok)report.text="无法绑定：请先安装 KoLab Bridge 主 APK"
 }
}
