#include <soundcloud/com_soundcloud_android_jni_VorbisEncoder.h>
#include <ogg/ogg.h>
#include <vorbis/vorbisenc.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <android/log.h>

#define DEBUG_TAG "VorbisEncoderNative"

vorbis_info      vi; /* struct that stores all the static vorbis bitstream settings */
vorbis_comment   vc; /* struct that stores all the user comments */
vorbis_dsp_state vd; /* central working state for the packet->PCM decoder */
vorbis_block     vb; /* local working space for packet->PCM decode */
ogg_stream_state os; /* take physical pages, weld into a logical stream of packets */
ogg_page         og; /* one Ogg bitstream page.  Vorbis packets are inside */
ogg_packet       op; /* one raw packet of data for decode */

int eos;
FILE* file;

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    init
 * Signature: (JJF)Z
 */
jint Java_com_soundcloud_android_jni_VorbisEncoder_init(JNIEnv *env, jobject obj, jstring outFile, jlong channels, jlong rate, jfloat quality) {
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "init(channels=%ld, rate=%ld, quality=%f)", (long)channels, (long)rate, quality);
    vorbis_info_init(&vi);
    int ret = vorbis_encode_init_vbr(&vi, channels, rate, quality);
    if (ret != 0) {
      __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "error initialising encoder, returned %d", ret);
      return ret;
    }

    const char *cOutFile = (*env)->GetStringUTFChars(env, outFile, 0);
    file = fopen(cOutFile, "w+");
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "writing to %s", cOutFile);
    (*env)->ReleaseStringUTFChars(env, outFile, cOutFile);

    vorbis_comment_init(&vc);
    vorbis_comment_add_tag(&vc, "ENCODER", DEBUG_TAG);

    vorbis_analysis_init(&vd, &vi);
    vorbis_block_init(&vd, &vb);

    srand(time(NULL));
    ogg_stream_init(&os,rand());

    ogg_packet header;
    ogg_packet header_comm;
    ogg_packet header_code;

    vorbis_analysis_headerout(&vd, &vc, &header, &header_comm, &header_code);
    ogg_stream_packetin(&os, &header);
    ogg_stream_packetin(&os, &header_comm);
    ogg_stream_packetin(&os, &header_code);

    while (!eos) {
      int result=ogg_stream_flush(&os, &og);
      if (result==0) break;
      fwrite(og.header,1,og.header_len, file);
      fwrite(og.body,1,og.body_len, file);
    }
    return 0;
}

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    addSamples
 * Signature: ([SII)I
 */
jint Java_com_soundcloud_android_jni_VorbisEncoder_addSamples(JNIEnv* env, jobject obj, jobject samples, jlong length) {
    jbyte* bbuf_in = (jbyte*) (*env)->GetDirectBufferAddress(env, samples);
    float **buffer = vorbis_analysis_buffer(&vd, length);
    int bytesPerSample = vi.channels * 2;
    int i, j;
    for (i=0; i<length / bytesPerSample; i++) {
        for(j=0; j<vi.channels; j++) {
            buffer[j][i] =
                (bbuf_in[i*bytesPerSample+(j*2)+1]<<8 | (0x00ff & (int)bbuf_in[i*bytesPerSample+j*2]))/ 32768.f;
        }
    }
    vorbis_analysis_wrote(&vd, i);
    while (vorbis_analysis_blockout(&vd, &vb) == 1) {
        /* analysis, assume we want to use bitrate management */
        vorbis_analysis(&vb, NULL);
        vorbis_bitrate_addblock(&vb);

        while (vorbis_bitrate_flushpacket(&vd, &op)) {
            /* weld the packet into the bitstream */
            ogg_stream_packetin(&os, &op);

            /* write out pages (if any) */
            while (!eos) {
              int result = ogg_stream_pageout(&os, &og);
              if (result == 0) break;
              fwrite(og.header, 1, og.header_len, file);
              fwrite(og.body, 1, og.body_len, file);

              /* this could be set above, but for illustrative purposes, I do
                 it here (to show that vorbis does know where the stream ends) */
              if (ogg_page_eos(&og)) eos=1;
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

   vorbis_analysis_wrote(&vd, 0);
   ogg_stream_clear(&os);
   vorbis_block_clear(&vb);
   vorbis_dsp_clear(&vd);
   vorbis_comment_clear(&vc);
   vorbis_info_clear(&vi);
   fclose(file);
   return 0;
}
