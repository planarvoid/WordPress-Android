/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_soundcloud_android_jni_VorbisEncoder */

#ifndef _Included_com_soundcloud_android_jni_VorbisEncoder
#define _Included_com_soundcloud_android_jni_VorbisEncoder
#ifdef __cplusplus
extern "C" {
#endif
#undef com_soundcloud_android_jni_VorbisEncoder_STATE_READY
#define com_soundcloud_android_jni_VorbisEncoder_STATE_READY 0L
#undef com_soundcloud_android_jni_VorbisEncoder_STATE_PAUSED
#define com_soundcloud_android_jni_VorbisEncoder_STATE_PAUSED 1L
#undef com_soundcloud_android_jni_VorbisEncoder_STATE_CLOSED
#define com_soundcloud_android_jni_VorbisEncoder_STATE_CLOSED 2L
/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    init
 * Signature: (Ljava/lang/String;Ljava/lang/String;JJF)I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_init
  (JNIEnv *, jobject, jstring, jstring, jlong, jlong, jfloat);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    write
 * Signature: (Ljava/nio/ByteBuffer;J)I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_write
  (JNIEnv *, jobject, jobject, jlong);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    closeStream
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_closeStream
  (JNIEnv *, jobject);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    pause
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_pause
  (JNIEnv *, jobject);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    release
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_release
  (JNIEnv *, jobject);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    getState
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_getState
  (JNIEnv *, jobject);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    chop
 * Signature: (Ljava/lang/String;Ljava/lang/String;DD)I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_chop
  (JNIEnv *, jclass, jstring, jstring, jdouble, jdouble);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    validate
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_validate
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
