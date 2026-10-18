package com.wordnest.nativeapp

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var root: LinearLayout
    private lateinit var tts: TextToSpeech
    private val words = mutableListOf<JSONObject>()
    private val day = 86400000L
    private val bg = Color.rgb(13,22,40)
    private val fg = Color.WHITE
    private val accent = Color.rgb(161,235,199)
    private val backupCreate = 100
    private val backupOpen = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg; window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        try { val a=JSONArray(getPreferences(MODE_PRIVATE).getString("words","[]")); for(i in 0 until a.length()) words.add(a.getJSONObject(i)) } catch (_:Exception) {}
        home()
    }
    override fun onInit(status:Int) { if(status==TextToSpeech.SUCCESS) tts.language=Locale.UK }
    override fun onDestroy(){tts.stop();tts.shutdown();super.onDestroy()}
    private fun persist(){getPreferences(MODE_PRIVATE).edit().putString("words",JSONArray(words).toString()).apply()}
    private fun label(s:String, size:Float=17f):TextView = TextView(this).apply {text=s;textSize=size;setTextColor(fg);setPadding(4,12,4,12)}
    private fun button(s:String, action:()->Unit):Button = Button(this).apply {text=s;setOnClickListener{action()}}
    private fun field(hint:String):EditText = EditText(this).apply {this.hint=hint;setSingleLine(false);setTextColor(fg);setHintTextColor(Color.LTGRAY)}
    private fun add(v:View){root.addView(v)}
    private fun page(title:String){
        val scroll=ScrollView(this).apply {setBackgroundColor(bg)}
        root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(22,22,22,36)}
        scroll.addView(root);setContentView(scroll)
        add(label("✦ WordNest",27f));add(label(title,20f))
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf("Add","Words","Review","Backup").forEachIndexed{idx,s->nav.addView(button(s){when(idx){0->home();1->library();2->review();else->backup()}},LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))}
        add(nav)
    }
    private fun toast(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
    private fun say(s:String){tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"wordnest")}
    private fun due(w:JSONObject)=w.optLong("next",0)<=System.currentTimeMillis()
    private fun home(){
        page("Your personal vocabulary")
        add(label("${words.size} saved  •  ${words.count{due(it)}} due today",16f))
        val q=field("English word, e.g. resilient");add(q)
        val result=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};add(result)
        add(button("Find definition"){
            val word=q.text.toString().trim().lowercase(Locale.ROOT)
            if(!word.matches(Regex("[a-z][a-z '-]{0,59}"))){toast("Enter an English word");return@button}
            result.removeAllViews();result.addView(label("Looking up…"))
            thread {
                try {
                    val connection=URL("https://api.dictionaryapi.dev/api/v2/entries/en/"+java.net.URLEncoder.encode(word,"UTF-8")).openConnection() as HttpURLConnection
                    connection.connectTimeout=10000;connection.readTimeout=10000
                    val data=JSONArray(connection.inputStream.bufferedReader().use{it.readText()}).getJSONObject(0)
                    connection.disconnect()
                    val meanings=data.getJSONArray("meanings");val m=meanings.getJSONObject(0)
                    val defs=m.getJSONArray("definitions");val d=defs.getJSONObject(0)
                    val obj=JSONObject().put("word",data.optString("word",word)).put("definition",d.getString("definition"))
                        .put("example",d.optString("example", "")).put("phonetic",data.optString("phonetic",""))
                        .put("part",m.optString("partOfSpeech",""))
                    runOnUiThread{result.removeAllViews();showCandidate(result,obj)}
                }catch(_:Exception){runOnUiThread{result.removeAllViews();result.addView(label("Dictionary unavailable. Add your own definition below."));manual(result,word)}}
            }
        })
        add(button("Add manually") {val word=q.text.toString().trim();if(word.isBlank())toast("Enter a word first") else {result.removeAllViews();manual(result,word)}})
    }
    private fun manual(target:LinearLayout,word:String){
        val def=field("Definition (required)");val ex=field("Example sentence (optional)");target.addView(def);target.addView(ex)
        target.addView(button("Save word"){
            if(def.text.isBlank()){toast("Definition required");return@button}
            saveWord(JSONObject().put("word",word).put("definition",def.text.toString()).put("example",ex.text.toString()))
        })
    }
    private fun showCandidate(target:LinearLayout,w:JSONObject){
        target.addView(label(w.optString("word"),26f));target.addView(label(w.optString("phonetic")+"  "+w.optString("part")))
        target.addView(label(w.optString("definition")));if(w.optString("example").isNotEmpty())target.addView(label("Example: "+w.optString("example")))
        target.addView(button("🔊 Listen"){say(w.optString("word"))});target.addView(button("＋ Save word"){saveWord(w)})
    }
    private fun saveWord(w:JSONObject){
        if(words.any{it.optString("word").equals(w.optString("word"),true)}){toast("Already saved");return}
        w.put("next",System.currentTimeMillis()).put("interval",0).put("reps",0)
        words.add(0,w);persist();toast("Saved!");home()
    }
    private fun library(){
        page("My words (${words.size})")
        val search=field("Search your vocabulary");add(search)
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};add(list)
        fun draw(q:String){list.removeAllViews();words.filter{it.optString("word").contains(q,true)||it.optString("definition").contains(q,true)}.forEach{w->
            list.addView(label("${w.optString("word")}  ${w.optString("phonetic")}",22f));list.addView(label(w.optString("definition")))
            if(w.optString("example").isNotEmpty())list.addView(label(w.optString("example")))
            list.addView(button("🔊 Listen"){say(w.optString("word"))})
            list.addView(button("Delete") {android.app.AlertDialog.Builder(this).setMessage("Delete ${w.optString("word") }?").setPositiveButton("Delete"){_,_->words.remove(w);persist();draw(q)}.setNegativeButton("Cancel",null).show()})
        }}
        search.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){draw(s.toString())};override fun afterTextChanged(e:android.text.Editable?) {}})
        draw("")
    }
    private fun review(){
        page("Spaced repetition")
        val w=words.filter{due(it)}.minByOrNull{it.optLong("next",0)}
        if(w==null){add(label("🎉 All caught up. No reviews due!"));return}
        add(label("${words.count{due(it)}} due today"));add(label(w.optString("word"),32f));add(button("🔊 Pronounce"){say(w.optString("word"))})
        val answer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};add(answer)
        add(button("Reveal meaning"){
            answer.removeAllViews();answer.addView(label(w.optString("definition"),21f));if(w.optString("example").isNotEmpty())answer.addView(label(w.optString("example")))
            listOf("Again","Hard","Good").forEach{grade->answer.addView(button(grade){
                val old=w.optInt("interval",0)
                val next=when(grade){"Again"->0;"Hard"->maxOf(1,(maxOf(1,old)*1.2).toInt());else->if(old==0)1 else if(old==1)3 else (old*2.2).toInt()}
                w.put("interval",next).put("reps",w.optInt("reps",0)+1).put("next",System.currentTimeMillis()+if(grade=="Again")600000L else next*day)
                persist();review()
            })}
        })
    }
    private fun backup(){page("Backup & restore");add(label("Your words stay on this device. Export regularly before reinstalling."))
        add(button("Export JSON backup"){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/json";putExtra(Intent.EXTRA_TITLE,"wordnest-backup.json")},backupCreate)})
        add(button("Import JSON backup"){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/json"},backupOpen)})
    }
    @Deprecated("Legacy activity result for minimum dependency build")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data)
        val uri=data?.data?:return;if(resultCode!=RESULT_OK)return
        try{if(requestCode==backupCreate){contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(JSONObject().put("app","WordNest").put("version",1).put("words",JSONArray(words)).toString(2))};toast("Backup exported")}
        else if(requestCode==backupOpen){val txt=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:return
            val json=JSONObject(txt);val a=json.getJSONArray("words");var count=0
            for(i in 0 until a.length()){val w=a.getJSONObject(i);if(w.optString("word").isNotBlank()&&w.optString("definition").isNotBlank()&&words.none{it.optString("word").equals(w.optString("word"),true)}){words.add(w);count++}}
            persist();toast("Imported $count words");backup()
        }}catch(_:Exception){toast("Could not read/write backup")}
    }
}
