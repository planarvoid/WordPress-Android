#include <android/log.h>

#define LOG(level, args, ...) __android_log_print(level, DEBUG_TAG, args,  ##__VA_ARGS__)
#define LOG_W(args, ...) LOG(ANDROID_LOG_WARN, args, ##__VA_ARGS__)
#define LOG_D(args, ...) LOG(ANDROID_LOG_DEBUG, args, ##__VA_ARGS__)
#define LOG_E(args, ...) LOG(ANDROID_LOG_ERROR, args, ##__VA_ARGS__)