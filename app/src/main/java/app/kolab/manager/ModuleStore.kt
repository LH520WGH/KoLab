package app.kolab.manager

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.system.Os
import java.io.File
import java.security.MessageDigest
import java.util.UUID

data class SavedModule(val id:String,val name:String,val sha:String,val size:Long,
    val category:String,val arch:String,val vermagic:String?,val verdict:String,val notes:String)

class ModuleStore(private val context:Context):SQLiteOpenHelper(context,"kolab.db",null,1) {
    override fun onCreate(db:SQLiteDatabase) {
        db.execSQL("CREATE TABLE modules(id TEXT PRIMARY KEY,name TEXT NOT NULL,sha TEXT NOT NULL UNIQUE,size INTEGER,category TEXT,arch TEXT,vermagic TEXT,verdict TEXT,notes TEXT)")
    }
    override fun onUpgrade(db:SQLiteDatabase, oldVersion:Int,newVersion:Int) {}
    private val storage get()=File(context.filesDir,"modules").apply{mkdirs()}
    fun list():List<SavedModule> {
        val result=mutableListOf<SavedModule>()
        readableDatabase.rawQuery("SELECT id,name,sha,size,category,arch,vermagic,verdict,notes FROM modules ORDER BY rowid DESC",null).use { c ->
            while(c.moveToNext()) result+=SavedModule(c.getString(0),c.getString(1),c.getString(2),
                c.getLong(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8))
        }
        return result
    }
    fun add(uri:Uri):String {
        val id=UUID.randomUUID().toString()
        val temp=File(storage,"$id.part")
        val dest=File(storage,"$id.ko")
        try {
            val hash=MessageDigest.getInstance("SHA-256")
            var length=0L
            context.contentResolver.openInputStream(uri)?.use { src ->
                temp.outputStream().use { out ->
                    val buffer=ByteArray(65536)
                    while(true) {
                        val n=src.read(buffer); if(n<0)break
                        length+=n
                        if(length>128L*1024*1024) throw IllegalArgumentException("模块超过 128 MiB")
                        hash.update(buffer,0,n);out.write(buffer,0,n)
                    }
                }
            } ?: error("无法读取文件")
            val sha=hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            readableDatabase.rawQuery("SELECT id FROM modules WHERE sha=?",arrayOf(sha)).use{
                if(it.moveToFirst()) return "已存在相同文件"
            }
            val facts=KoInspector.inspect(temp,Os.uname().release)
            if(!temp.renameTo(dest))error("文件保存失败")
            val name=uri.lastPathSegment?.substringAfterLast('/')?.take(200) ?: "module.ko"
            val values=android.content.ContentValues().apply {
                put("id",id);put("name",name);put("sha",sha);put("size",length)
                put("category","未分类");put("arch",facts.architecture);put("vermagic",facts.vermagic)
                put("verdict",facts.verdict);put("notes",facts.notes)
            }
            try { writableDatabase.insertOrThrow("modules",null,values) }
            catch(e:Exception) {dest.delete();throw e}
            return "已导入 $name：${facts.verdict}"
        } finally {temp.delete()}
    }
    fun category(id:String,value:String) {
        val values=android.content.ContentValues().apply{put("category",value)}
        writableDatabase.update("modules",values,"id=?",arrayOf(id))
    }
    fun delete(item:SavedModule) {
        require(UUID.fromString(item.id).toString()==item.id)
        val path=File(storage,"${item.id}.ko")
        if(path.exists() && !path.delete()) error("无法删除模块副本")
        writableDatabase.delete("modules","id=?",arrayOf(item.id))
    }
}
