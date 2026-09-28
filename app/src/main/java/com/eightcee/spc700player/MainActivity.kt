package com.eightcee.spc700player
import android.os.Bundle
import android.content.Intent
import android.media.*
import android.net.Uri
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.atomic.AtomicBoolean
class MainActivity:AppCompatActivity(){
 private val engine=SpcEngine();private var audio:AudioTrack?=null;private val playing=AtomicBoolean(false);private lateinit var status:TextView;private lateinit var play:Button
 override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(36,60,36,36);gravity=Gravity.CENTER_HORIZONTAL};root.addView(TextView(this).apply{text="SPC700 Player";textSize=34f});status=TextView(this).apply{text="Open an .spc file";gravity=Gravity.CENTER};val open=Button(this).apply{text="OPEN .SPC";setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*"},7)}};play=Button(this).apply{text="▶ PLAY";isEnabled=false;setOnClickListener{if(playing.get())pause() else start()}};root.addView(status);root.addView(open);root.addView(play);setContentView(root)}
 override fun onActivityResult(r:Int,c:Int,i:Intent?){super.onActivityResult(r,c,i);if(r==7&&c==RESULT_OK)i?.data?.let{load(it)}}
 private fun load(u:Uri){pause();val d=contentResolver.openInputStream(u)?.use{it.readBytes()}?:return;val e=engine.open(d);status.text=if(e.isEmpty()) engine.info() else e;play.isEnabled=e.isEmpty()}
 private fun start(){if(playing.getAndSet(true))return;play.text="Ⅱ PAUSE";val min=AudioTrack.getMinBufferSize(44100,12,2);audio=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setChannelMask(12).setEncoding(2).build()).setBufferSizeInBytes(maxOf(min,16384)).setTransferMode(AudioTrack.MODE_STREAM).build().also{it.play()};Thread{while(playing.get()){val p=engine.render(2048);if(p.isEmpty())break;audio?.write(p,0,p.size)}}.start()}
 private fun pause(){playing.set(false);audio?.pause();audio?.flush();audio?.release();audio=null;if(::play.isInitialized)play.text="▶ PLAY"}
 override fun onDestroy(){pause();engine.close();super.onDestroy()}
}
