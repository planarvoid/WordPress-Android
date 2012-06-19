LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := soundcloud_audio_encoder
LOCAL_C_INCLUDES := $(LOCAL_PATH)/vorbis/lib $(LOCAL_PATH)/vorbis/include $(LOCAL_PATH)/ogg/include $(LOCAL_PATH)/liboggz/include $(LOCAL_PATH)/include

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -DFIXED_POINT -D_ARM_ASSEM_ -D__ANDROID__ -DOPT_GENERIC -DHAVE_STRERROR
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -DFIXED_POINT -D_ARM_ASSEM_ -D__ANDROID__ -DOPT_GENERIC -DHAVE_STRERROR
LOCAL_LDLIBS := -lm -llog
LOCAL_ARM_MODE  := arm

LOCAL_SRC_FILES := ogg/src/bitwise.c \
	ogg/src/framing.c \
	vorbis/lib/analysis.c \
	vorbis/lib/bitrate.c \
	vorbis/lib/block.c \
	vorbis/lib/codebook.c \
	vorbis/lib/envelope.c \
	vorbis/lib/floor0.c \
	vorbis/lib/floor1.c \
	vorbis/lib/info.c \
	vorbis/lib/lookup.c \
	vorbis/lib/lpc.c \
	vorbis/lib/lsp.c \
	vorbis/lib/mapping0.c \
	vorbis/lib/mdct.c \
	vorbis/lib/misc.c \
	vorbis/lib/psy.c \
	vorbis/lib/registry.c \
	vorbis/lib/res0.c \
	vorbis/lib/sharedbook.c \
	vorbis/lib/smallft.c \
	vorbis/lib/synthesis.c \
	vorbis/lib/vorbisenc.c \
	vorbis/lib/vorbisfile.c \
	vorbis/lib/window.c \
	liboggz/src/liboggz/dirac.c \
	liboggz/src/liboggz/metric_internal.c \
	liboggz/src/liboggz/oggz.c \
	liboggz/src/liboggz/oggz_auto.c \
	liboggz/src/liboggz/oggz_comments.c \
	liboggz/src/liboggz/oggz_dlist.c \
	liboggz/src/liboggz/oggz_io.c \
	liboggz/src/liboggz/oggz_read.c \
	liboggz/src/liboggz/oggz_seek.c \
	liboggz/src/liboggz/oggz_stream.c \
	liboggz/src/liboggz/oggz_table.c \
	liboggz/src/liboggz/oggz_vector.c \
	liboggz/src/liboggz/oggz_write.c \
	soundcloud/com_soundcloud_android_jni_VorbisEncoder.c

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

# tremor/tremolo module
LOCAL_MODULE := soundcloud_audio_decoder
LOCAL_C_INCLUDES := $(LOCAL_PATH)/tremolo $(LOCAL_PATH)/include

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -DFIXED_POINT -D_ARM_ASSEM_ -D__ANDROID__ -DOPT_GENERIC -DHAVE_STRERROR
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -DFIXED_POINT -D_ARM_ASSEM_ -D__ANDROID__ -DOPT_GENERIC -DHAVE_STRERROR
LOCAL_LDLIBS := -lm -llog
LOCAL_ARM_MODE  := arm

LOCAL_SRC_FILES := tremolo/bitwise.c \
	tremolo/bitwiseARM.s \
	tremolo/codebook.c \
	tremolo/dpen.s \
	tremolo/dsp.c \
	tremolo/floor0.c \
	tremolo/floor1.c \
	tremolo/floor1ARM.s \
	tremolo/floor_lookup.c \
	tremolo/framing.c \
	tremolo/info.c \
	tremolo/mapping0.c \
	tremolo/mdct.c \
	tremolo/mdctARM.s \
	tremolo/misc.c \
	tremolo/res012.c \
	tremolo/speed.s \
	tremolo/vorbisfile.c \
	libwav/libwav.c \
	soundcloud/com_soundcloud_android_jni_VorbisDecoder.c

include $(BUILD_SHARED_LIBRARY)
