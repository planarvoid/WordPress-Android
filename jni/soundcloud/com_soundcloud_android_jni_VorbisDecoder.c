#include <soundcloud/com_soundcloud_android_jni_VorbisDecoder.h>

#include <ivorbiscodec.h>
#include <ivorbisfile.h>
#include <stdio.h>
#include <stdlib.h>
#include <libwav/libwav.h>
#include <android/log.h>

#define DEBUG_TAG "VorbisDecoderNative"

char pcmout[4096]; /* take 4k out of the data segment, not the stack */

jint Java_com_soundcloud_android_jni_VorbisDecoder_decode(JNIEnv* env, jobject obj, jstring in, jstring out) {
    OggVorbis_File vf;
    int eof = 0;
    int current_section;
    FILE *inFile, *outFile;

    const char *cIn = (*env)->GetStringUTFChars(env, in, 0);
    const char *cOut = (*env)->GetStringUTFChars(env, out, 0);

    inFile = fopen(cIn, "r");
    (*env)->ReleaseStringUTFChars(env, in, cIn);

    int ret = ov_open(inFile, &vf, NULL, 0);
    if (ret < 0) {
      __android_log_print(ANDROID_LOG_ERROR, DEBUG_TAG, "Error opening stream: %d", ret);
      return ret;
    }

    outFile = fopen(cOut, "w+");
    (*env)->ReleaseStringUTFChars(env, out, cOut);

    vorbis_info *vi = ov_info(&vf, -1);
    long numSamples = (long)ov_pcm_total(&vf,-1);
    writeWavHeader(outFile, numSamples, vi->channels, vi->rate, 16);

    while (!eof) {
      long ret = ov_read(&vf, pcmout, sizeof(pcmout), &current_section);
      if (ret == 0) {
        eof = 1;
      } else if (ret < 0) {
        /* error in the stream. */
      } else {
        /* we don't bother dealing with sample rate changes, etc, but you'll have to */
        fwrite(pcmout, 1, ret, outFile);
      }
    }

    /* cleanup */
    ov_clear(&vf);
    fclose(outFile);
    return 0;
}