#include <jni.h>
#include <vector>
#include <string>
#include <mutex>
#include "gme/gme.h"
static Music_Emu* emu=nullptr;static std::mutex m;static jstring js(JNIEnv*e,const char*s){return e->NewStringUTF(s?s:"");}
extern "C" JNIEXPORT jstring JNICALL Java_com_eightcee_spc700player_SpcEngine_open(JNIEnv*e,jobject,jbyteArray a){std::lock_guard<std::mutex>l(m);if(emu){gme_delete(emu);emu=nullptr;}auto n=e->GetArrayLength(a);std::vector<jbyte>b(n);e->GetByteArrayRegion(a,0,n,b.data());auto er=gme_open_data(b.data(),n,&emu,44100);if(er)return js(e,er);er=gme_start_track(emu,0);return js(e,er?er:"");}
extern "C" JNIEXPORT jshortArray JNICALL Java_com_eightcee_spc700player_SpcEngine_render(JNIEnv*e,jobject,jint frames){std::lock_guard<std::mutex>l(m);if(!emu)return e->NewShortArray(0);std::vector<short>b(frames*2);if(gme_play(emu,b.size(),b.data()))return e->NewShortArray(0);auto a=e->NewShortArray(b.size());e->SetShortArrayRegion(a,0,b.size(),b.data());return a;}
extern "C" JNIEXPORT void JNICALL Java_com_eightcee_spc700player_SpcEngine_mute(JNIEnv*,jobject,jint voice,jboolean muted){std::lock_guard<std::mutex>l(m);if(emu)gme_mute_voice(emu,voice,muted?1:0);}
extern "C" JNIEXPORT void JNICALL Java_com_eightcee_spc700player_SpcEngine_seek(JNIEnv*,jobject,jint ms){std::lock_guard<std::mutex>l(m);if(emu)gme_seek(emu,ms);}
extern "C" JNIEXPORT jstring JNICALL Java_com_eightcee_spc700player_SpcEngine_info(JNIEnv*e,jobject){std::lock_guard<std::mutex>l(m);if(!emu)return js(e,"");gme_info_t*i=nullptr;auto er=gme_track_info(emu,&i,0);if(er)return js(e,er);std::string s=std::string(i->song)+"\n"+i->game+"\n"+i->author;gme_free_info(i);return js(e,s.c_str());}
extern "C" JNIEXPORT void JNICALL Java_com_eightcee_spc700player_SpcEngine_close(JNIEnv*,jobject){std::lock_guard<std::mutex>l(m);if(emu){gme_delete(emu);emu=nullptr;}}
