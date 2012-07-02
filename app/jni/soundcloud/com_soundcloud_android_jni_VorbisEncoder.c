#include <soundcloud/com_soundcloud_android_jni_VorbisEncoder.h>
#include <ogg/ogg.h>
#include <vorbis/vorbisenc.h>
#include <config.h>
#include <oggz-chop/oggz-chop.h>
#include <oggz-validate.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>

#define DEBUG_TAG "VorbisEncoderNative"
#include <common.h>

#define READY  com_soundcloud_android_jni_VorbisEncoder_STATE_READY
#define PAUSED com_soundcloud_android_jni_VorbisEncoder_STATE_PAUSED
#define CLOSED com_soundcloud_android_jni_VorbisEncoder_STATE_CLOSED
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

static void init_stream(encoder_state *);
static void write_to_stream(encoder_state *, int length);
static void close_stream(encoder_state *);
static struct timespec diff(struct timespec, struct timespec);
static void write_og(encoder_state *state);
static void close_file(encoder_state *state);

jint Java_com_soundcloud_android_jni_VorbisEncoder_init(JNIEnv *env, jobject obj, jstring outFile, jstring fileMode, jlong channels, jlong rate, jfloat quality) {
    const char *c_outFile = (*env)->GetStringUTFChars(env, outFile, 0);
    const char *c_fileMode = (*env)->GetStringUTFChars(env, fileMode, 0);

    LOG_D("init(file=%s, mode=%s, channels=%ld, rate=%ld, quality=%f)", c_outFile, c_fileMode, (long)channels, (long)rate, quality);
    encoder_state *state = malloc(sizeof(encoder_state));
    memset(state, 0, sizeof(*state));

    state->eos = 0;
    state->e_state = READY;

    // global encoder init (expensive)
    vorbis_info_init(&state->vi);
    int ret = vorbis_encode_init_vbr(&state->vi, channels, rate, quality);
    if (ret != 0) {
      LOG_D("error initialising encoder, returned %d", ret);
      vorbis_info_clear(&state->vi);
      free(state);
      state = NULL;
      return ret;
    }


    state->file = fopen(c_outFile, c_fileMode);
    if (!state->file) {
        LOG_E("error opening file %s, error=%s", c_outFile, strerror(errno));
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

    init_stream(state);

    /* set state on instance */
    (*env)->SetIntField(env, obj, encoder_state_field, (int)state);
    return 0;
}


jint Java_com_soundcloud_android_jni_VorbisEncoder_write(JNIEnv* env, jobject obj, jobject samples, jlong length) {
    /* get state from instance */
    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
    if (!state) {
        LOG_E("write() called in wrong state");
        return -1;
    }

    if (state->file == NULL) {
        LOG_D("reopening %s", state->file_name);
        state->file = fopen(state->file_name, "a");

        if (!state->file) {
            LOG_E("error opening %s, (%s)", state->file_name, strerror(errno));
            return -1;
        }
    }

    switch (state->e_state) {
        /* if the stream was paused, reopen and reinitialise it */
        case PAUSED:
            state->e_state = READY;
            state->eos = 0;
            break;
        case CLOSED:
            state->e_state = READY;
            state->eos = 0;
            init_stream(state);
            break;
        case READY: break;
        default:
           LOG_D("write called in wrong state");
           return -1;
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
    write_to_stream(state, i);
    return 0;
}

jint Java_com_soundcloud_android_jni_VorbisEncoder_pause(JNIEnv *env, jobject obj) {
    LOG_D("pause");

    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
    if (!state || state->e_state != READY) {
        LOG_E("pause() called in wrong state");
        return -1;
    } else {
        fflush(state->file);
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
    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
    if (!state) {
       (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), "already released");
    } else {
        // make sure stream if closed before releasing
        if (state->e_state != CLOSED) {
            close_stream(state);
        }
        // free up internal data structures and clear state
       vorbis_info_clear(&state->vi);
       free(state->file_name);
       free(state);
       (*env)->SetIntField(env, obj, encoder_state_field, (int) NULL);
    }
}

jint Java_com_soundcloud_android_jni_VorbisEncoder_closeStream(JNIEnv *env, jobject obj) {
    encoder_state *state = (encoder_state*) (*env)->GetIntField(env, obj, encoder_state_field);
    if (!state) {
       (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), "already released");
    } else {
       if (state->e_state != CLOSED) {
           close_stream(state);
           state->e_state = CLOSED;
           return 0;
       } else {
           return -1;
       }
    }
}

jint Java_com_soundcloud_android_jni_VorbisEncoder_chop(JNIEnv *env, jclass klass, jstring in, jstring out, jdouble start, jdouble end) {
    OCState _state;
    OCState *state = &_state;
    memset(state, 0, sizeof(*state));

    state->infilename = (char *) (*env)->GetStringUTFChars(env, in, 0);
    state->outfilename =  (char *) (*env)->GetStringUTFChars(env, out, 0);
    state->start = start;
    state->end = end;
    state->do_skeleton = 0;
    state->verbose = 1;

    LOG_D("about to chop %s -> %s (%.2lf-%.2lf)", state->infilename, state->outfilename, state->start, state->end);
    int result = chop(state);
    LOG_D("finished (result=%d)", result);

    (*env)->ReleaseStringUTFChars(env, in, state->infilename);
    (*env)->ReleaseStringUTFChars(env, out, state->outfilename);
    return result;
}

jint Java_com_soundcloud_android_jni_VorbisEncoder_validate(JNIEnv *env, jclass klass, jstring in) {
    const char *c_in = (*env)->GetStringUTFChars(env, in, 0);
    int ret = validate_ogg(c_in);
    (*env)->ReleaseStringUTFChars(env, in, c_in);
    return ret;
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


static void init_stream(encoder_state *state) {
    LOG_D("initialising stream");

    struct timespec ts_start;
    struct timespec ts_end;
    clock_gettime(CLOCK_MONOTONIC, &ts_start);

    vorbis_analysis_init(&state->vd, &state->vi);
    vorbis_comment_init(&state->vc);
    vorbis_comment_add_tag(&state->vc, "ENCODER", ENCODER_TAG);

    clock_gettime(CLOCK_MONOTONIC, &ts_end);
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
      if (ogg_stream_flush(&state->os, &state->og) == 0) break;
      write_og(state);
    }
    struct timespec d = diff(ts_start, ts_end);

    LOG_D("took %d:%.2f msec", d.tv_sec, (double) (d.tv_nsec/ 1000000.0));
}

static void close_stream(encoder_state *state) {
    LOG_D("close_stream");

    write_to_stream(state, 0);
    close_file(state);

    // free stream specific data structures
    ogg_stream_clear(&state->os);
    vorbis_block_clear(&state->vb);
    vorbis_comment_clear(&state->vc);
    vorbis_dsp_clear(&state->vd);
}

static void write_to_stream(encoder_state *state, int length) {
    LOG_D("write_to_stream (%d)", length);

    vorbis_analysis_wrote(&state->vd, length);
    while (vorbis_analysis_blockout(&state->vd, &state->vb) == 1) {
        /* analysis, assume we want to use bitrate management */
        vorbis_analysis(&state->vb, NULL);
        vorbis_bitrate_addblock(&state->vb);

        while (vorbis_bitrate_flushpacket(&state->vd, &state->op)) {
            /* weld the packet into the bitstream */
            ogg_stream_packetin(&state->os, &state->op);

            /* write out pages (if any) */
            while (!state->eos) {
              if (ogg_stream_pageout(&state->os, &state->og) == 0) break;
              write_og(state);
              /* this could be set above, but for illustrative purposes, I do
                 it here (to show that vorbis does know where the stream ends) */
              if (ogg_page_eos(&state->og)) state->eos=1;
            }
        }
    }
}

/* produces diff between to timestamps */
static struct timespec diff(struct timespec start, struct timespec end) {
    struct timespec temp;
    if (end.tv_nsec - start.tv_nsec < 0) {
        temp.tv_sec  = end.tv_sec - start.tv_sec - 1;
        temp.tv_nsec = 1000000000 + end.tv_nsec-start.tv_nsec;
    } else {
        temp.tv_sec  = end.tv_sec - start.tv_sec;
        temp.tv_nsec = end.tv_nsec - start.tv_nsec;
    }
    return temp;
}

static void write_og(encoder_state *state) {
    if (fwrite(state->og.header, 1, state->og.header_len, state->file) != state->og.header_len) {
        LOG_W("error writing header ");
    }

    if (fwrite(state->og.body,   1, state->og.body_len, state->file) != state->og.body_len) {
        LOG_W("error writing body");
    }
}

static void close_file(encoder_state *state) {
    LOG_D("close_file");

    int ret = fclose(state->file);
    if (ret != 0) {
        LOG_W("error closing file: %d, %s", ret, strerror(errno));
    } else {
        state->file = NULL;
    }
}
