#include <soundcloud/com_soundcloud_android_jni_VorbisEncoder.h>
#include <ogg/ogg.h>
#include <vorbis/vorbisenc.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>
#include <android/log.h>

#define DEBUG_TAG "VorbisEncoderNative"
#define ENCODER_TAG "SoundCloud for Android"

#define LOG(level, args, ...) __android_log_print(level, DEBUG_TAG, args,  ##__VA_ARGS__)
#define LOG_W(args, ...) LOG(ANDROID_LOG_WARN, args, ##__VA_ARGS__)
#define LOG_D(args, ...) LOG(ANDROID_LOG_DEBUG, args, ##__VA_ARGS__)
#define LOG_E(args, ...) LOG(ANDROID_LOG_ERROR, args, ##__VA_ARGS__)

typedef struct {
    vorbis_info      vi; /* struct that stores all the static vorbis bitstream settings */
    vorbis_comment   vc; /* struct that stores all the user comments */
    vorbis_dsp_state vd; /* central working state for the packet->PCM decoder */
    vorbis_block     vb; /* local working space for packet->PCM decode */
    ogg_stream_state os; /* take physical pages, weld into a logical stream of packets */
    ogg_page         og; /* one Ogg bitstream page.  Vorbis packets are inside */
    ogg_packet       op; /* one raw packet of data for decode */

    FILE *file;
    char *file_name;
    int eos;
} encoder_state;


static jfieldID encoder_state_field;

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    init
 * Signature: (JJF)Z
 */
jint Java_com_soundcloud_android_jni_VorbisEncoder_init(JNIEnv *env, jobject obj, jstring outFile, jstring fileMode, jlong channels, jlong rate, jfloat quality) {
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "init(channels=%ld, rate=%ld, quality=%f)", (long)channels, (long)rate, quality);
    encoder_state *state = malloc(sizeof(encoder_state));
    state->eos = 0;

    vorbis_info_init(&state->vi);
    int ret = vorbis_encode_init_vbr(&state->vi, channels, rate, quality);
    if (ret != 0) {
      LOG_D("error initialising encoder, returned %d", ret);
      return ret;
    }

    const char *c_outFile = (*env)->GetStringUTFChars(env, outFile, 0);
    const char *c_fileMode = (*env)->GetStringUTFChars(env, fileMode, 0);

    state->file = fopen(c_outFile, c_fileMode);
    if (!state->file) {
        LOG_E("error opening file %s, errno=%d", c_outFile, errno);
        return -1;
    }

    LOG_D("writing to %s (mode=%s)", c_outFile, c_fileMode);
    state->file_name = malloc(strlen(c_outFile)+1);
    strcpy(state->file_name, c_outFile);
    (*env)->ReleaseStringUTFChars(env, outFile, c_outFile);
    (*env)->ReleaseStringUTFChars(env, fileMode, c_fileMode);

    vorbis_comment_init(&state->vc);
    vorbis_comment_add_tag(&state->vc, "ENCODER", ENCODER_TAG);

    vorbis_analysis_init(&state->vd, &state->vi);
    vorbis_block_init(&state->vd, &state->vb);

    srand(time(NULL));
    ogg_stream_init(&state->os, rand());

    ogg_packet header;
    ogg_packet header_comm;
    ogg_packet header_code;

    vorbis_analysis_headerout(&state->vd, &state->vc, &header, &header_comm, &header_code);
    ogg_stream_packetin(&state->os, &header);
    ogg_stream_packetin(&state->os, &header_comm);
    ogg_stream_packetin(&state->os, &header_code);

    while (!state->eos) {
      int result=ogg_stream_flush(&state->os, &state->og);
      if (result==0) break;
      fwrite(state->og.header, 1, state->og.header_len, state->file);
      fwrite(state->og.body, 1, state->og.body_len, state->file);
    }

    /* set state on instance */
    (*env)->SetIntField(env, obj, encoder_state_field, (int)state);
    return 0;
}

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    addSamples
 * Signature: ([SII)I
 */
jint Java_com_soundcloud_android_jni_VorbisEncoder_addSamples(JNIEnv* env, jobject obj, jobject samples, jlong length) {
    /* get state from instance */
    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);

    jbyte* bbuf_in = (jbyte*) (*env)->GetDirectBufferAddress(env, samples);
    float **buffer = vorbis_analysis_buffer(&state->vd, length);
    int bytesPerSample = state->vi.channels * 2;
    int i, j;
    for (i=0; i<length / bytesPerSample; i++) {
        for(j=0; j < state->vi.channels; j++) {
            buffer[j][i] =
                (bbuf_in[i*bytesPerSample+(j*2)+1]<<8 | (0x00ff & (int)bbuf_in[i*bytesPerSample+j*2]))/ 32768.f;
        }
    }
    vorbis_analysis_wrote(&state->vd, i);
    while (vorbis_analysis_blockout(&state->vd, &state->vb) == 1) {
        /* analysis, assume we want to use bitrate management */
        vorbis_analysis(&state->vb, NULL);
        vorbis_bitrate_addblock(&state->vb);

        while (vorbis_bitrate_flushpacket(&state->vd, &state->op)) {
            /* weld the packet into the bitstream */
            ogg_stream_packetin(&state->os, &state->op);

            /* write out pages (if any) */
            while (!state->eos) {
              int result = ogg_stream_pageout(&state->os, &state->og);
              if (result == 0) break;
              fwrite(state->og.header, 1, state->og.header_len, state->file);
              fwrite(state->og.body, 1, state->og.body_len, state->file);

              /* this could be set above, but for illustrative purposes, I do
                 it here (to show that vorbis does know where the stream ends) */
              if (ogg_page_eos(&state->og)) state->eos=1;
            }
        }
    }
    return 0;
}

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    finish
 * Signature: ()Z
 */
jint Java_com_soundcloud_android_jni_VorbisEncoder_finish(JNIEnv *env, jobject obj) {
   __android_log_write(ANDROID_LOG_DEBUG, DEBUG_TAG, "finish");

   /* get state from instance */
   encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);

   vorbis_analysis_wrote(&state->vd, 0);
   ogg_stream_clear(&state->os);
   vorbis_block_clear(&state->vb);
   vorbis_dsp_clear(&state->vd);
   vorbis_comment_clear(&state->vc);
   vorbis_info_clear(&state->vi);
   fclose(state->file);
   free(state->file_name);
   free(state);
   return 0;
}


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if ((*vm)->GetEnv( vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
      LOG_E("GetEnv() != JNI_OK");
      return -1;
    } else {
      /* initialise class references etc */
       LOG_D("JNI_OnLoad()");

       jclass encoderCls = (*env)->FindClass(env, "com/soundcloud/android/jni/VorbisEncoder");

       if (!encoderCls) {
          LOG_E("JNI_OnLoad: could not get encoder class");
          return -1;
       }
       encoder_state_field = (*env)->GetFieldID(env, encoderCls, "encoder_state", "I");

       if (!encoder_state_field) {
          LOG_E("JNI_OnLoad: could not get state field");
          return -1;
       }
       return JNI_VERSION_1_6;
    }
}
