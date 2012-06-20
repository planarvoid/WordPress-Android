LOCAL_PATH:= $(call my-dir)

MY_CFLAGS := -O2 -Wall -D__ANDROID__ -DFIXED_POINT -D_ARM_ASSEM_ -DOPT_GENERIC -DHAVE_STRERROR
#
# libogg
# https://github.com/soundcloud/ogg
#
include $(CLEAR_VARS)
LOCAL_MODULE    := ogg
LOCAL_C_INCLUDES := $(LOCAL_PATH)/ogg/include $(LOCAL_PATH)/include

LOCAL_CFLAGS   := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_LDLIBS := -lm
LOCAL_SRC_FILES := ogg/src/bitwise.c ogg/src/framing.c

include $(BUILD_STATIC_LIBRARY)

#
# libvorbis
# https://github.com/soundcloud/vorbis
#
include $(CLEAR_VARS)
LOCAL_MODULE    := vorbis
LOCAL_C_INCLUDES := $(LOCAL_PATH)/vorbis/lib \
	$(LOCAL_PATH)/vorbis/include \
	$(LOCAL_PATH)/ogg/include \
	$(LOCAL_PATH)/include

LOCAL_CFLAGS   := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_LDLIBS := -lm

LOCAL_SRC_FILES := vorbis/lib/analysis.c \
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
	vorbis/lib/window.c

LOCAL_STATIC_LIBRARIES := ogg
include $(BUILD_STATIC_LIBRARY)

#
# liboggz
# https://github.com/soundcloud/liboggz
#
include $(CLEAR_VARS)
LOCAL_MODULE := oggz
LOCAL_C_INCLUDES := $(LOCAL_PATH)/vorbis/include \
	$(LOCAL_PATH)/ogg/include \
	$(LOCAL_PATH)/liboggz/include \
	$(LOCAL_PATH)/liboggz/src/tools \
	$(LOCAL_PATH)/liboggz/src/liboggz \
	$(LOCAL_PATH)/include

LOCAL_SRC_FILES := liboggz/src/liboggz/dirac.c \
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
	liboggz/src/tools/skeleton.c \
	liboggz/src/tools/mimetypes.c \
	liboggz/src/tools/oggz-chop/oggz-chop.c \
	liboggz/src/tools/oggz_tools.c \
	liboggz/src/tools/oggz-validate.c

include $(BUILD_STATIC_LIBRARY)

#
# libtremolo
# https://github.com/soundcloud/tremor/tree/tremolo
#
include $(CLEAR_VARS)
LOCAL_MODULE := tremolo
LOCAL_C_INCLUDES := $(LOCAL_PATH)/tremolo $(LOCAL_PATH)/include

LOCAL_CFLAGS   := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_LDLIBS := -lm
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
	tremolo/vorbisfile.c

include $(BUILD_STATIC_LIBRARY)

#
# tremor
# https://github.com/soundcloud/tremor/
include $(CLEAR_VARS)
LOCAL_MODULE := tremor
LOCAL_C_INCLUDES := $(LOCAL_PATH)/tremor $(LOCAL_PATH)/include $(LOCAL_PATH)/ogg/include

LOCAL_CFLAGS   := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) $(MY_CFLAGS)
LOCAL_LDLIBS := -lm
LOCAL_ARM_MODE  := arm

LOCAL_SRC_FILES := tremor/block.c \
	tremor/codebook.c \
	tremor/floor0.c \
	tremor/floor1.c \
	tremor/info.c \
	tremor/mapping0.c \
	tremor/mdct.c \
	tremor/registry.c \
	tremor/res012.c \
	tremor/sharedbook.c \
	tremor/synthesis.c \
	tremor/vorbisfile.c \
	tremor/window.c

LOCAL_STATIC_LIBRARIES := ogg

include $(BUILD_STATIC_LIBRARY)

#
# libwav
#
include $(CLEAR_VARS)
LOCAL_MODULE := wav
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SRC_FILES := libwav/libwav.c
include $(BUILD_STATIC_LIBRARY)

#
# libsoundcloud_vorbis_decoder
#
include $(CLEAR_VARS)
LOCAL_MODULE := soundcloud_vorbis_decoder
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SRC_FILES := soundcloud/com_soundcloud_android_jni_VorbisDecoder.c
LOCAL_LDLIBS := -llog

ifeq (armeabi, $(TARGET_ARCH_ABI))
    # use faster tremolo decoder on ARM architectures without fp
    LOCAL_CFLAGS += -DTREMOR
    LOCAL_C_INCLUDES +=  $(LOCAL_PATH)/tremor $(LOCAL_PATH)/ogg/include
    LOCAL_STATIC_LIBRARIES := tremor wav
else
    # fallback to standard vorbis decoder
    LOCAL_C_INCLUDES += $(LOCAL_PATH)/vorbis/include $(LOCAL_PATH)/ogg/include
    LOCAL_STATIC_LIBRARIES := vorbis wav
endif

include $(BUILD_SHARED_LIBRARY)

#
# libsoundcloud_vorbis_encoder
#
include $(CLEAR_VARS)
LOCAL_MODULE := soundcloud_vorbis_encoder
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include \
	$(LOCAL_PATH)/vorbis/include \
	$(LOCAL_PATH)/liboggz/include \
	$(LOCAL_PATH)/ogg/include \
	$(LOCAL_PATH)/liboggz/src/tools
LOCAL_SRC_FILES := soundcloud/com_soundcloud_android_jni_VorbisEncoder.c
LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES := vorbis oggz

include $(BUILD_SHARED_LIBRARY)
