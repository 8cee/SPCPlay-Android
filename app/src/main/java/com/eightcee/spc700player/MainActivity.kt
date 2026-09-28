package com.eightcee.spc700player
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.*
import android.net.Uri
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.atomic.AtomicBoolean
class MainActivity:AppCompatActivity(){
 private val engine=SpcEngine();private var audio:AudioTrack?=null;private val playing=AtomicBoolean(false)
 private lateinit var info:TextView;private lateinit var play:Button;private lateinit var time:TextView;private lateinit var seek:SeekBar;private lateinit var playlist:ListView;private val items=ArrayList<Uri>();private var volume=1f;private var speed=1f
 private var positionMs=0;private var loaded=false;private val muted=BooleanArray(8);private val mono=Typeface.MONOSPACE
 private fun label(t:String,s:Float=12f)=TextView(this).apply{text=t;textSize=s;typeface=mono;setTextColor(Color.rgb(25,25,25));setPadding(6,3,6,3)}
 private fun btn(t:String)=Button(this).apply{text=t;textSize=10f;typeface=mono;minHeight=0;minimumHeight=0;setPadding(2,2,2,2)}
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(212,208,200));setPadding(8,8,8,8)}
  root.addView(label("SNES SPC700 PLAYER",18f).apply{typeface=Typeface.create(mono,Typeface.BOLD);gravity=Gravity.CENTER})
  val menu=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("File","Settings","Playlist").forEach{n->menu.addView(btn(n),LinearLayout.LayoutParams(0,-2,1f))};root.addView(menu)
  info=label("No SPC loaded\n\nTITLE :\nGAME  :\nARTIST:",13f).apply{setBackgroundColor(Color.BLACK);setTextColor(Color.rgb(80,255,80));setPadding(12,10,12,10)}
  val body=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};left.addView(info,LinearLayout.LayoutParams(-1,0,1f));val meters=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.BOTTOM};meters.addView(label("MIXER",11f),LinearLayout.LayoutParams(0,-1,1f));for(v in 1..8)meters.addView(label(v.toString()+" E",11f).apply{gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL},LinearLayout.LayoutParams(0,70,1f));left.addView(meters);body.addView(left,LinearLayout.LayoutParams(0,-1,1.25f));playlist=ListView(this);playlist.adapter=ArrayAdapter<String>(this,android.R.layout.simple_list_item_activated_1,ArrayList());playlist.choiceMode=ListView.CHOICE_MODE_SINGLE;playlist.setOnItemClickListener{_,_,p,_->load(items[p])};body.addView(playlist,LinearLayout.LayoutParams(0,-1,1f));root.addView(body,LinearLayout.LayoutParams(-1,0,1f))
  seek=SeekBar(this).apply{max=300000;setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
   override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){if(u){positionMs=p;time.text=clock(p)}}
   override fun onStartTrackingTouch(s:SeekBar?){}
   override fun onStopTrackingTouch(s:SeekBar?){if(loaded)engine.seek(s?.progress?:0)}
  })};root.addView(seek)
  val tr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
  val open=btn("OPEN").apply{setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*"},7)}}
  val save=btn("SAVE").apply{setOnClickListener{Toast.makeText(this@MainActivity,"SPC is already a snapshot file",Toast.LENGTH_SHORT).show()}};tr.addView(save,LinearLayout.LayoutParams(0,-2,1f));val rew=btn("REW").apply{setOnClickListener{jump(-5000)}};play=btn("PLAY").apply{isEnabled=false;setOnClickListener{if(playing.get())pause()else start()}}
  val ff=btn("FF").apply{setOnClickListener{jump(5000)}};val stop=btn("STOP").apply{setOnClickListener{pause();if(loaded){positionMs=0;engine.seek(0);seek.progress=0;time.text=clock(0)}}}
  listOf(open,rew,play,ff,stop).forEach{tr.addView(it,LinearLayout.LayoutParams(0,-2,1f))};root.addView(tr)
  time=label("00:00.000",18f).apply{gravity=Gravity.CENTER;typeface=Typeface.create(mono,Typeface.BOLD)};root.addView(time)
  root.addView(label("CHANNELS / VOICES",10f).apply{gravity=Gravity.CENTER})
  val ch=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  for(v in 0..7){val cb=btn((v+1).toString()+"\nON");cb.setOnClickListener{muted[v]=!muted[v];engine.mute(v,muted[v]);cb.text=(v+1).toString()+"\n"+if(muted[v])"MUTE" else "ON"};ch.addView(cb,LinearLayout.LayoutParams(0,-2,1f))}
  root.addView(ch);val opts=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val vm=btn("VL-").apply{setOnClickListener{volume=(volume-.1f).coerceAtLeast(0f);audio?.setVolume(volume)}};val vp=btn("VL+").apply{setOnClickListener{volume=(volume+.1f).coerceAtMost(1f);audio?.setVolume(volume)}};val sm=btn("SP-").apply{setOnClickListener{speed=(speed-.05f).coerceAtLeast(.5f);setSpeed()}};val sp=btn("SP+").apply{setOnClickListener{speed=(speed+.05f).coerceAtMost(2f);setSpeed()}};listOf(vm,vp,sm,sp).forEach{opts.addView(it,LinearLayout.LayoutParams(0,-2,1f))};root.addView(opts);val pl=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val append=btn("APPEND").apply{setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="*/*"},8)}};val remove=btn("REMOVE").apply{setOnClickListener{val p=playlist.checkedItemPosition;if(p>=0){items.removeAt(p);refreshList()}}};val clear=btn("CLEAR").apply{setOnClickListener{items.clear();refreshList()}};val up=btn("UP").apply{setOnClickListener{move(-1)}};val dn=btn("DN").apply{setOnClickListener{move(1)}};listOf(append,remove,clear,up,dn).forEach{pl.addView(it,LinearLayout.LayoutParams(0,-2,1f))};root.addView(pl);setContentView(root)
 }
 private fun clock(ms:Int)=String.format("%02d:%02d.%03d",ms/60000,(ms/1000)%60,ms%1000)
 private fun jump(d:Int){if(loaded){positionMs=(positionMs+d).coerceIn(0,seek.max);engine.seek(positionMs);seek.progress=positionMs;time.text=clock(positionMs)}}
 private fun setSpeed(){audio?.playbackParams=PlaybackParams().setSpeed(speed)}
 private fun refreshList(){playlist.adapter=ArrayAdapter<String>(this,android.R.layout.simple_list_item_activated_1,items.map{it.lastPathSegment?:"SPC"})}
 private fun move(d:Int){val p=playlist.checkedItemPosition;val q=p+d;if(p>=0&&q in items.indices){val x=items.removeAt(p);items.add(q,x);refreshList();playlist.setItemChecked(q,true)}}
 override fun onActivityResult(r:Int,c:Int,i:Intent?){super.onActivityResult(r,c,i);if(c==RESULT_OK)i?.data?.let{u->if(r==7){if(!items.contains(u))items.add(u);refreshList();load(u)}else if(r==8){items.add(u);refreshList()}}}
 private fun load(u:Uri){pause();val d=contentResolver.openInputStream(u)?.use{it.readBytes()}?:return;val e=engine.open(d);loaded=e.isEmpty();positionMs=0;seek.progress=0;info.text=if(loaded)"ID666 INFORMATION\n\n"+engine.info() else "LOAD ERROR\n"+e;play.isEnabled=loaded;time.text=clock(0)}
 private fun start(){if(!loaded||playing.getAndSet(true))return;play.text="PAUSE";val min=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);audio=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(maxOf(min,16384)).setTransferMode(AudioTrack.MODE_STREAM).build().also{it.play();it.setVolume(volume);it.playbackParams=PlaybackParams().setSpeed(speed)};Thread{while(playing.get()){val p=engine.render(2048);if(p.isEmpty())break;audio?.write(p,0,p.size);positionMs+=2048*1000/44100;runOnUiThread{time.text=clock(positionMs);seek.progress=positionMs.coerceAtMost(seek.max)}}}.start()}
 private fun pause(){playing.set(false);audio?.pause();audio?.flush();audio?.release();audio=null;if(::play.isInitialized)play.text="PLAY"}
 override fun onDestroy(){pause();engine.close();super.onDestroy()}
}