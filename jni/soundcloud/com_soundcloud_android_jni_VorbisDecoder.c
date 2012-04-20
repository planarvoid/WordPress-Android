#include <soundcloud/com_soundcloud_android_jni_VorbisDecoder.h>

#include <ivorbiscodec.h>
#include <ivorbisfile.h>
#include <stdio.h>
#include <stdlib.h>
#include <libwav/libwav.h>
#include <android/log.h>

#define DEBUG_TAG "VorbisDecoderNative"
#include <soundcloud/common.h>

char pcmout[4096]; /* take 4k out of the data segment, not the stack */

typedef struct {
    FILE *file;
    OggVorbis_File vf;
    vorbis_info *vi;
    long numSamples;

} decoder_state;

static jfieldID decoder_state_field;

jint Java_com_soundcloud_android_jni_VorbisDecoder_init(JNIEnv *env, jobject obj, jobject path) {
    const char *cPath = (*env)->GetStringUTFChars(env, path, 0);
    LOG_D("init(%s)", cPath);

    decoder_state *state = malloc(sizeof(decoder_state));
    state->file = fopen(cPath, "r");

    (*env)->ReleaseStringUTFChars(env, path, cPath);
    int ret = ov_open(state->file, &state->vf, NULL, 0);
    if (ret < 0) {
      LOG_E("Error opening stream: %d", ret);
      return ret;
    }
    state->vi = ov_info(&state->vf, -1);
    state->numSamples = (long)ov_pcm_total(&state->vf,-1);

    (*env)->SetIntField(env, obj, decoder_state_field, (int)state);
    return 0;
}

jint Java_com_soundcloud_android_jni_VorbisDecoder_decodeToFile(JNIEnv* env, jobject obj, jstring out) {
    decoder_state *state = (decoder_state*) (*env)->GetIntField(env, obj, decoder_state_field);
    int eof = 0;
    int current_section;
    FILE *outFile;

    const char *cOut = (*env)->GetStringUTFChars(env, out, 0);
    LOG_D("decode(%s)", cOut);

    outFile = fopen(cOut, "w+");
    (*env)->ReleaseStringUTFChars(env, out, cOut);

    writeWavHeader(outFile, state->numSamples, state->vi->channels, state->vi->rate, 16);
    while (!eof) {
      long ret = ov_read(&state->vf, pcmout, sizeof(pcmout), &current_section);
      if (ret == 0) {
        eof = 1;
      } else if (ret < 0) {
        /* error in the stream. */
      } else {
        /* we don't bother dealing with sample rate changes, etc, but you'll have to */
        fwrite(pcmout, 1, ret, outFile);
      }
    }

    fclose(outFile);
    return 0;
}

jobject Java_com_soundcloud_android_jni_VorbisDecoder_getInfo(JNIEnv *env, jobject obj) {
    jclass infoCls = (*env)->FindClass(env, "com/soundcloud/android/jni/Info");
    jmethodID ctor = (*env)->GetMethodID(env, infoCls, "<init>", "()V");
    jobject info = (*env)->NewObject(env, infoCls, ctor);
    decoder_state *state = (decoder_state*) (*env)->GetIntField(env, obj, decoder_state_field);

    jfieldID channels = (*env)->GetFieldID(env, infoCls,   "channels",   "I");
    jfieldID sampleRate = (*env)->GetFieldID(env, infoCls, "sampleRate", "I");
    jfieldID numSamples = (*env)->GetFieldID(env, infoCls, "numSamples", "J");

    (*env)->SetIntField(env,  info, channels,   state->vi->channels);
    (*env)->SetIntField(env,  info, sampleRate, state->vi->rate);
    (*env)->SetLongField(env, info, numSamples, state->numSamples);

    return info;
}

jint Java_com_soundcloud_android_jni_VorbisDecoder_pcmSeek(JNIEnv *env, jobject obj, jlong pos, jboolean align) {
    LOG_D("pcmSeek(%ld, %d)", (long)pos, align);
    decoder_state *state = (decoder_state*) (*env)->GetIntField(env, obj, decoder_state_field);
    return align ? ov_pcm_seek_page(&state->vf, pos) : ov_pcm_seek(&state->vf, pos);
}

jint Java_com_soundcloud_android_jni_VorbisDecoder_decode(JNIEnv *env, jobject obj, jobject buffer, jint length) {
    decoder_state *state = (decoder_state*) (*env)->GetIntField(env, obj, decoder_state_field);
    int current_section;
    jbyte* bbuffer = (jbyte*) (*env)->GetDirectBufferAddress(env, buffer);
    int ret = ov_read(&state->vf, bbuffer, length, &current_section);
    return ret;
}

void Java_com_soundcloud_android_jni_VorbisDecoder_release(JNIEnv *env, jobject obj) {
    decoder_state *state = (decoder_state*) (*env)->GetIntField(env, obj, decoder_state_field);
    if (state) {
        ov_clear(&state->vf);
        vorbis_info_clear(state->vi);
        fclose(state->file);
        free(state);
        state = NULL;
    }
}

jint Java_com_soundcloud_android_jni_VorbisDecoder_getState(JNIEnv *env, jobject obj) {
    decoder_state *state = (decoder_state*) (*env)->GetIntField(env, obj, decoder_state_field);
    return (state == NULL ? -1 : 0);
}


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if ((*vm)->GetEnv( vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
      LOG_E("GetEnv() != JNI_OK");
      return -1;
    } else {
      /* initialise class references etc */
       LOG_D("JNI_OnLoad()");

       jclass decoderCls = (*env)->FindClass(env, "com/soundcloud/android/jni/VorbisDecoder");
       if (!decoderCls) {
          LOG_E("JNI_OnLoad: could not get decoder class");
          return -1;
       }
       decoder_state_field = (*env)->GetFieldID(env, decoderCls, "decoder_state", "I");

       if (!decoder_state_field) {
          LOG_E("JNI_OnLoad: could not get state field");
          return -1;
       }
       return JNI_VERSION_1_6;
    }
}
