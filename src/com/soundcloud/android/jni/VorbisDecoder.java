package com.soundcloud.android.jni;


import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;

public class VorbisDecoder {
    private static final String TAG = "VorbisDecoder";
    public final File file;

    private static final boolean ALIGN_SEEK_ON_PAGE = false;

    @SuppressWarnings("UnusedDeclaration") // used in JNI code
    private int decoder_state;

    public VorbisDecoder(File file) {
        this.file = file;
        init(file.getAbsolutePath());
    }

    public native Info getInfo();
    public native void release();

    public void decodeToFile(File out) throws DecoderException {
        int ret = decodeToFile(out.getAbsolutePath());
        if (ret < 0) {
            throw new DecoderException("decode return < 0", ret);
        }
    }

    /**
     * This is the main function used to decode a Vorbis file within a loop.
     * t returns up to the specified number of bytes of decoded PCM audio in the requested endianness,
     * signedness, and word size. If the audio is multichannel, the channels are interleaved in the
     * output buffer. If the passed in buffer is large, ov_read() will not fill it; the passed in
     * buffer size is treated as a limit and not a request.
     *
     * @param out    A pointer to an output buffer. The decoded output is inserted into this buffer.
     * @param length Number of bytes to be read into the buffer. Should be the same size as the buffer. A typical value is 4096.
     * @return
     * <ul>
     * <li> {@link VorbisConstants.OV_HOLE} indicates there was an interruption in the data. (one of: garbage between pages, loss of sync followed by recapture, or a corrupt page)
     * <li> {@link VorbisConstants.OV_EBADLINK} indicates that an invalid stream section was supplied to libvorbisfile, or the requested link is corrupt.
     * <li> {@link VorbisConstants.OV_EINVAL} indicates the initial file headers couldn't be read or are corrupt, or that the initial open call for vf failed.
     * <li> 0 indicates EOF
     * <li> n indicates actual number of bytes read. ov_read() will decode at most one vorbis packet per invocation, so the value returned will generally be less than length.
     * </ul>
     * @see <a href="http://xiph.org/vorbis/doc/vorbisfile/ov_read.html"ov_read></a>
     */
    public native int decode(ByteBuffer out, int length);

    /**
     * @param pos position in pcm samples to seek to in the bitstream
     * @param alignOnPage if yes, find closest sample on page boundary (faster)
     * @return nonzero indicates failure, described by several error codes:
     * <ul>
     *     <li>{@link VorbisConstants.OV_ENOSEEK} - Bitstream is not seekable.</li>
     *     <li>{@link VorbisConstants.OV_EINVAL} - Invalid argument value; possibly called with an OggVorbis_File structure that isn't open.</li>
     *     <li>{@link VorbisConstants.OV_EREAD} - A read from media returned an error.</li>
     *     <li>{@link VorbisConstants.OV_EFAULT} - Internal logic fault; indicates a bug or heap/stack corruption.</li>
     *     <li>{@link VorbisConstants.OV_EBADLINK} - Invalid stream section supplied to libvorbisfile, or the requested link is corrupt.</li>
     * </ul>
     * @see <a href="http://xiph.org/vorbis/doc/vorbisfile/ov_pcm_seek.html">ov_pcm_seek</a>
     */
    public native int pcmSeek(long pos, boolean alignOnPage);

    public int pcmSeek(long pos) {
        return pcmSeek(pos, ALIGN_SEEK_ON_PAGE);
    }

    // private methods
    private native int init(String file);
    private native int decodeToFile(String out);

    /**
     * @return the current state (0 = ready to decode, < 0 uninitialised)
     */
    public native int getState();

    @Override protected void finalize() throws Throwable {
        super.finalize();
        if (getState() >= 0) {
            Log.w(TAG, "unreleased decoder in finalize() - call release() when done with encoder");
            release();
        }
    }

    static {
        try {
            System.loadLibrary("soundcloud_audio_tremor");
        } catch (UnsatisfiedLinkError e) {
            // only ignore exception in non-android env
            if ("Dalvik".equals(System.getProperty("java.vm.name"))) throw e;
        }
    }
}
