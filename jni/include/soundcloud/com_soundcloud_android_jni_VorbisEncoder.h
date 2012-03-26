/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_soundcloud_android_jni_VorbisEncoder */

#ifndef _Included_com_soundcloud_android_jni_VorbisEncoder
#define _Included_com_soundcloud_android_jni_VorbisEncoder
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    init
 * Signature: (Ljava/lang/String;JJF)I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_init
  (JNIEnv *, jobject, jstring, jlong, jlong, jfloat);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    addSamples
 * Signature: (Ljava/nio/ByteBuffer;J)I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_addSamples
  (JNIEnv *, jobject, jobject, jlong);

/*
 * Class:     com_soundcloud_android_jni_VorbisEncoder
 * Method:    finish
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_soundcloud_android_jni_VorbisEncoder_finish
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
