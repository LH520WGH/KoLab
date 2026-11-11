package app.kolab.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.system.Os
import android.view.Gravity
import android.widget.*
import java.util.concurrent.Executors

/** Offline only: no shell, Root, native executable or network. */
class MainActivity:Activity() {
    private val executor=Executors.newSingleThreadExecutor()
    private lateinit var store:ModuleStore
    private lateinit var root:LinearLayout
    private var tab=0
    private var selected:String?=null
    private var items=listOf<SavedModule>()
    private val bg=Color.rgb(16,24,35)
    private val fg=Color.rgb(235,242,250)
    private val pale=Color.rgb(162,190,216)
    private val blue=Color.rgb(106,190,250)
    private fun text(value:String,sp:Float=15f,color:Int=fg)=TextView(this).apply{
        text=value;textSize=sp;setTextColor(color);setPadding(8,12,8,12)
    }
    private fun btn(value:String,action:()->Unit)=Button(this).apply{
        text=value;setOnClickListener{action()}
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=bg;window.navigationBarColor=bg
        store=ModuleStore(this)
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(16,16,16,16)}
        setContentView(root);refresh()
    }
    private fun refresh()=executor.execute{
        val data=runCatching{store.list()}.getOrDefault(emptyList())
        runOnUiThread{items=data;render()}
    }
    private fun render(){
        root.removeAllViews()
        root.addView(text("KoLab  ·  LOCAL",25f,blue))
        root.addView(text("KERNEL MODULE LABORATORY",11f,pale))
        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        when(tab){
            0 -> {
                body.addView(text("本地模块仓库：${items.size} 个",20f))
                body.addView(btn("＋ 导入 .ko 文件"){importFiles()})
                if(items.isEmpty())body.addView(text("还没有模块。导入不会执行或加载文件。",14f,pale))
                items.forEach{item->
                    body.addView(btn("${item.name}\n${item.category} · ${item.arch} · ${item.verdict}"){
                        selected=item.id;tab=1;render()
                    })
                }
            }
            1 -> {
                body.addView(text("静态兼容性分析",20f))
                val item=items.find{it.id==selected} ?: items.firstOrNull()
                if(item==null) body.addView(text("请先导入模块。"))
                else {
                    body.addView(text("文件：${item.name}\nSHA256：${item.sha}\n大小：${item.size} 字节\n架构：${item.arch}\nvermagic：${item.vermagic ?: "未知"}\n预检：${item.verdict}\n说明：${item.notes}",14f))
                    body.addView(btn("更改分类"){
                        val opts=arrayOf("未分类","触摸","传感器","网络","实验","其他")
                        AlertDialog.Builder(this).setItems(opts){_,i->
                            executor.execute{store.category(item.id,opts[i]);refresh()}
                        }.show()
                    })
                    body.addView(btn("删除仓库副本"){
                        AlertDialog.Builder(this).setTitle("删除模块副本？")
                            .setMessage("仅删除 KoLab 的副本，不会删除原文件或卸载内核模块。")
                            .setPositiveButton("删除"){_,_->
                                executor.execute{runCatching{store.delete(item)};refresh()}
                            }.setNegativeButton("取消",null).show()
                    })
                }
            }
            2 -> {
                body.addView(text("设备信息",20f))
                body.addView(text("型号：${Build.MODEL}\nAndroid：${Build.VERSION.RELEASE}\n内核：${Os.uname().release}\n架构：${Build.SUPPORTED_ABIS.joinToString()}",14f))
                body.addView(text("普通权限读取，不访问 KernelSU 或系统受限节点。",13f,pale))
            }
            else -> {
                body.addView(text("KoLab V1 · 离线模式",20f))
                body.addView(text("独立包名 app.kolab.manager\n无 INTERNET 权限\n不请求 Root、不执行 .ko\n文件副本保存于应用私有空间\n卸载 KoLab 会清除私有仓库，请保留原始 .ko。",14f))
                body.addView(btn("导出 JSON 清单"){exportJson()})
            }
        }
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        listOf("仓库","分析","设备","设置").forEachIndexed{i,name->
            nav.addView(btn(name){tab=i;render()},LinearLayout.LayoutParams(0,-2,1f))
        }
        root.addView(nav)
    }
    private fun importFiles(){
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="*/*";putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        },31)
    }
    private fun exportJson(){
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="application/json"
            putExtra(Intent.EXTRA_TITLE,"kolab-manifest.json")
        },32)
    }
    @Deprecated("Framework compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK || data==null)return
        if(requestCode==31){
            val uris=linkedSetOf<Uri>()
            data.data?.let{uris.add(it)}
            data.clipData?.let{c->for(i in 0 until c.itemCount)uris.add(c.getItemAt(i).uri)}
            executor.execute{
                val messages=uris.take(50).map{runCatching{store.add(it)}.getOrElse{e->"失败：${e.message}"}}
                runOnUiThread{
                    AlertDialog.Builder(this).setTitle("导入结果").setMessage(messages.joinToString("\n"))
                        .setPositiveButton("确定",null).show()
                }
                refresh()
            }
        } else if(requestCode==32){
            val uri=data.data ?:return
            executor.execute{
                runCatching{
                    val arr=org.json.JSONArray()
                    store.list().forEach{m->arr.put(org.json.JSONObject().apply{
                        put("name",m.name);put("sha256",m.sha);put("size",m.size)
                        put("category",m.category);put("vermagic",m.vermagic);put("verdict",m.verdict)
                    })}
                    val content=org.json.JSONObject().put("format","kolab-v1").put("modules",arr).toString(2)
                    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(content)}
                }.onFailure{e->runOnUiThread{Toast.makeText(this,"导出失败：${e.message}",Toast.LENGTH_LONG).show()}}
            }
        }
    }
    override fun onDestroy(){executor.shutdown();super.onDestroy()}
}
