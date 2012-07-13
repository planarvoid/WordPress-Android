package com.soundcloud.android.jni;

/**
 * Error return codes possible from libvorbis and libvorbisfile:
 * <p/>
 * All 'failure' style returns are <0; this either indicates a generic
 * 'false' value (eg, ready?  T or F) or an error condition.  Code can
 * safely just test for < 0, or look at the specific return code for more
 * detail.
 *
 * @see jni/vorbis/include/vorbis/codec.h
 */
public enum VorbisConstants {

    /**
     * The call returned a 'false' status (eg, ov_bitrate_instant
     * can return OV_FALSE if playback is not in progress, and thus
     * there is no instantaneous bitrate information to report.
     */
    OV_FALSE(-1),

    /** End of file */
    OV_EOF(-2),

    /**
     * libvorbis/libvorbisfile is alerting the application that
     * there was an interruption in the data (one of: garbage
     * between pages, loss of sync followed by recapture, or a
     * corrupt page)
     */
    OV_HOLE(-3),


    /** A read from media returned an error. */
    OV_EREAD(-128),

    /**
     * Internal logic fault; indicates a bug or heap/stack corruption.
     */
    OV_EFAULT(-129),

    /**
     * The bitstream makes use of a feature not implemented in this library version.
     */
    OV_EIMPL(-130),

    /** Invalid argument value. */
    OV_EINVAL(-131),

    /** Bitstream/page/packet is not Vorbis data. */
    OV_ENOTVORBIS(-132),

    /** Invalid Vorbis bitstream header. */
    OV_EBADHEADER(-133),

    /** Vorbis version mismatch. */
    OV_EVERSION(-134),

    /** Packet data submitted to vorbis_synthesis is not audio data. */
    OV_ENOTAUDIO(-135),

    /** Invalid packet submitted to vorbis_synthesis. */
    OV_EBADPACKET(-136),

    /**
     * Invalid stream section supplied to libvorbis/libvorbisfile,
     * or the requested link is corrupt.
     */
    OV_EBADLINK(-137),

    /** Bitstream is not seekable. */
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
