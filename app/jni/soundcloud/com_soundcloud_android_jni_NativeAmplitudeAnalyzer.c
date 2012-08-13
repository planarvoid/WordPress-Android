#include <soundcloud/com_soundcloud_android_jni_NativeAmplitudeAnalyzer.h>
#include <math.h>

#define DEBUG_TAG "NativeAmplitudeAnalyzer"
#include <common.h>

static int getMax(jbyte *buf, jint length);
static jfieldID last_max, current_adjusted_max_amplitude;

static float amp_max;

jfloat Java_com_soundcloud_android_jni_NativeAmplitudeAnalyzer_frameAmplitude(JNIEnv *env,
jobject obj, jobject buffer, jint length) {
    const int lmax = (*env)->GetIntField(env, obj, last_max);
    int cadjmax = (*env)->GetIntField(env, obj, current_adjusted_max_amplitude);

    jbyte *buf = (jbyte*) (*env)->GetDirectBufferAddress(env, buffer);

    int max = getMax(buf, length);

    if (max == 0) {
        max = lmax;
    } else {
        (*env)->SetIntField(env, obj, last_max, max);
    }

    // Simple peak follower, cf. http://www.musicdsp.org/showone.php?id=19
    if (max >= cadjmax) {
        /* When we hit a peak, ride the peak to the top. */
        cadjmax = max;
    } else {
        /*  decay of output when signal is low. */
        cadjmax *= 0.6;
    }

    (*env)->SetIntField(env, obj, current_adjusted_max_amplitude, cadjmax);

    const float amp = sqrt(sqrt(cadjmax)) / amp_max;
    return amp < 0.1 ? 0.1 : amp;
}

static int getMax(jbyte *buf, jint length) {
    // TODO: get channel config from fields
    int i, j;
    const int bytesPerSample = 2;
    const int channels = 1;
    int max = 0;
    for (i=0; i < length; i += bytesPerSample) {
        int value = buf[i+1]<<8 | (0x00ff & (int)buf[i]);
        if (value > max) {
            max = value;
        }
    }
    return max;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if ((*vm)->GetEnv( vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
      LOG_E("GetEnv() != JNI_OK");
      return -1;
    } else {
      /* initialise class references etc */
       LOG_D("JNI_OnLoad()");

       jclass cls = (*env)->FindClass(env, "com/soundcloud/android/jni/NativeAmplitudeAnalyzer");

       if (!cls) {
          LOG_E("JNI_OnLoad: could not get class");
          return -1;
       }

       last_max = (*env)->GetFieldID(env, cls, "last_max", "I");
       current_adjusted_max_amplitude = (*env)->GetFieldID(env, cls, "current_adjusted_max_amplitude", "I");

       if (!last_max || !current_adjusted_max_amplitude) {
          LOG_E("JNI_OnLoad: could not initialize fields");
          return -1;
       }

       amp_max = sqrt(sqrt(32768));
       return JNI_VERSION_1_6;
    }
}

