#include <soundcloud/com_soundcloud_android_jni_VorbisEncoder.h>
#include <ogg/ogg.h>
#include <vorbis/vorbisenc.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>

#define DEBUG_TAG "VorbisEncoderNative"
#include <common.h>

#define READY  0
#define PAUSED 1
#define ENCODER_TAG "SoundCloud for Android"

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
    int e_state;     /* READY / PAUSED */
} encoder_state;


static jfieldID encoder_state_field;


jint Java_com_soundcloud_android_jni_VorbisEncoder_init(JNIEnv *env, jobject obj, jstring outFile, jstring fileMode, jlong channels, jlong rate, jfloat quality) {
    LOG_D("init(channels=%ld, rate=%ld, quality=%f)", (long)channels, (long)rate, quality);
    encoder_state *state = malloc(sizeof(encoder_state));
    state->eos = 0;
    state->e_state = READY;

    vorbis_info_init(&state->vi);
    int ret = vorbis_encode_init_vbr(&state->vi, channels, rate, quality);
    if (ret != 0) {
      LOG_D("error initialising encoder, returned %d", ret);
      vorbis_info_clear(&state->vi);
      free(state);
      state = NULL;
      return ret;
    }

    const char *c_outFile = (*env)->GetStringUTFChars(env, outFile, 0);
    const char *c_fileMode = (*env)->GetStringUTFChars(env, fileMode, 0);

    state->file = fopen(c_outFile, c_fileMode);
    if (!state->file) {
        LOG_E("error opening file %s, errno=%d", c_outFile, errno);
        (*env)->ReleaseStringUTFChars(env, outFile, c_outFile);
        (*env)->ReleaseStringUTFChars(env, fileMode, c_fileMode);
        vorbis_info_clear(&state->vi);
        free(state);
        state = NULL;
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

jint Java_com_soundcloud_android_jni_VorbisEncoder_addSamples(JNIEnv* env, jobject obj, jobject samples, jlong length) {
    /* get state from instance */
    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
    if (!state) {
        LOG_E("addSamples() called in wrong state");
        return -1;
    }

    if (state->e_state == PAUSED) {
        state->file = fopen(state->file_name, "a");
        state->e_state = READY;
        state->eos = 0;
    }

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

jint Java_com_soundcloud_android_jni_VorbisEncoder_pause(JNIEnv *env, jobject obj) {
    LOG_D("pause");

    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
    if (!state || state->e_state != READY) {
        LOG_E("pause() called in wrong state");
        return -1;
    } else {
        fclose(state->file);
        state->e_state = PAUSED;
        return 0;
    }
}

jint Java_com_soundcloud_android_jni_VorbisEncoder_getState(JNIEnv *env, jobject obj) {
    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
    return state == NULL ? -1 : state->e_state;
}

void Java_com_soundcloud_android_jni_VorbisEncoder_release(JNIEnv *env, jobject obj) {
   LOG_D("release");
   /* get state from instance */
   encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
   if (!state) {
       (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), "already released");
   } else {
       vorbis_analysis_wrote(&state->vd, 0);
       ogg_stream_clear(&state->os);
       vorbis_block_clear(&state->vb);
       vorbis_dsp_clear(&state->vd);
       vorbis_comment_clear(&state->vc);
       vorbis_info_clear(&state->vi);
       fclose(state->file);
       free(state->file_name);
       free(state);
       (*env)->SetIntField(env, obj, encoder_state_field, (int) NULL);
   }
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
