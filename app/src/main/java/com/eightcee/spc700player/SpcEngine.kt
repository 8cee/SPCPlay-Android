package com.eightcee.spc700player
class SpcEngine {
 companion object { init { System.loadLibrary("spcplayer") } }
 external fun open(data:ByteArray):String
 external fun render(frames:Int):ShortArray
 external fun seek(ms:Int)
 external fun mute(voice:Int, muted:Boolean)
 external fun info():String
 external fun close()
}