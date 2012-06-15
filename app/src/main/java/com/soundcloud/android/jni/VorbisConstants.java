package com.soundcloud.android.jni;

/**
 * @see vorbis/codec.h
 */
public enum VorbisConstants {

    OV_FALSE(-1),
    OV_EOF(-2),
    OV_HOLE(-3),
    OV_EREAD(-128),
    OV_EFAULT(-129),
    OV_EIMPL(-130),
    OV_EINVAL(-131),
    OV_ENOTVORBIS(-132),
    OV_EBADHEADER(-133),
    OV_EVERSION(-134),
    OV_ENOTAUDIO(-135),
    OV_EBADPACKET(-136),
    OV_EBADLINK(-137),
    OV_ENOSEEK(-138);

    private final int code;

    VorbisConstants(int code) {
        this.code = code;
    }

    public static String getString(int code) {
        for (VorbisConstants c : values()) {
            if (c.code == code) return c.name();
        }
        return null;
    }
}
