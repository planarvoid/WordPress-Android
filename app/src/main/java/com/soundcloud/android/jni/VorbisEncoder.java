package com.soundcloud.android.jni;

import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class VorbisEncoder {
    public static final String TAG = "VorbisEncoder";

    public final long channels;
    public final long rate;
    public final float quality;
    public final File file;

    public static final int STATE_READY  = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_CLOSED = 2;

    @SuppressWarnings("UnusedDeclaration") // used in JNI code
    private int encoder_state;

    /**
     * Initialises the encoder for usage.
     * @param file      output file
     * @param mode      the file mode ('w', 'a', ...)
     * @param channels  number of channels
     * @param rate      sampleRate (44100)
     * @param quality   desired encoding quality (0-1.0)
     */
    public VorbisEncoder(File file, String mode, long channels, long rate, float quality) throws EncoderException {
        final int ret = init(file.getAbsolutePath(), mode, channels, rate, quality);
        if (ret != 0) {
            throw new EncoderException("Error initialising encoder", ret);
        }
        this.file = file;
        this.channels = channels;
        this.rate = rate;
        this.quality = quality;
    }

    /**
     * Convenience method to add samples from an inputstream
     * @param is inputstream containing samples
     * @throws IOException
     */
    public void encodeStream(InputStream is) throws IOException {
        encodeStream(is, -1, null);
    }

    /**
     * Convenience method to add samples from an inputstream
     * @param is inputstream containing samples
     * @param length total length of data
     * @param listener a progress listener
     * @throws IOException
     */
    public void encodeStream(InputStream is, long length, @Nullable ProgressListener listener) throws IOException {
        ByteBuffer bbuffer = ByteBuffer.allocateDirect((int) (8192*channels*2));
        byte[] buffer = new byte[bbuffer.capacity()];
        int n;
        long total = 0;
        while ((n = is.read(buffer)) != -1) {
            bbuffer.rewind();
            bbuffer.put(buffer, 0, n);
            int ret = write(bbuffer, n);
            if (ret < 0) throw new EncoderException("addSamples returned error", ret);
            total += n;
            if (listener != null) listener.onProgress(total, length);
        }
    }

    /**
     * Sets the record head to a different position in the recording. The next
     * call to {@link #write(java.nio.ByteBuffer, long)} will append to this position.
     * All content after msecs gets automatically truncated.
     *
     * @param pos position in secs, or -1 for end of file
     * @throws EncoderException if the head could not be moved
     * @throws IllegalStateException if not paused
     */
    public boolean startNewStream(double pos) throws IOException {
        Log.d(TAG, "startNewStream("+pos+")");

        if (getState() == STATE_PAUSED) {
            closeStream();

            if (pos != -1) {
                File tmp = IOUtils.appendToFilename(file, "_temp");
                // chop and rename
                extract(file, tmp, 0, pos);
                if (!tmp.renameTo(file)) {
                    throw new EncoderException("could not rename file", -1);
                }
                return true;
            } else {
                return false; // next call to write will will just reopen stream and append to end of file
            }
        } else {
            throw new IllegalStateException("cannot move head when not paused");
        }
    }

    native private int init(String output, String mode, long channels, long rate, float quality);

    /**
     * Add some samples to the current file.
     * @param samples
     * @param length
     * @return < 0 in error case
     */
    native public int write(ByteBuffer samples, long length);


    /**
     * Closes the current stream, writing EOS packet.
     * @return 0 for success
     */
    native private int closeStream();

    /**
     * Pauses the current encoding process (closes the file).
     */
    native public int pause();

    /**
     * Call to free up resources. The encoder cannot be used after this method has been called.
     * @throws IllegalStateException if the encoder has been released previously
     */
    native public void release();

    /**
     * @return the current state ({@link #STATE_READY} = ready to encode,
     * {@link #STATE_PAUSED} = paused, < 0 uninitialised)
     */
    public native int getState();

    /**
     * Extract the part of an Ogg file between given start and/or end times.
     *
     * @param in ogg input file path
     * @param out ogg output file path
     * @param start start time in secs
     * @param end end time in secs, -1 for whole length
     * @throws  EncoderException if case of error
     */
    public static void extract(File in, File out, double start, double end) throws EncoderException {
        int res = chop(in.getAbsolutePath(), out.getAbsolutePath(), start, end);
        if (res != 0) {
            throw new EncoderException("Eror extracting ogg", res);
        }
    }

    /**
     * Validate the ogg stream
     * @param in the input file
     * @return true if valid
     */
    public static boolean validate(File in) {
        return validate(in.getAbsolutePath()) == 0;
    }

    /**
     * {@link #extract(java.io.File, java.io.File, double, double)}
     * @return 0 for success
     */
    native private static int chop(String in, String out, double start, double end);

    native private static int validate(String in);

    /**
     * Encodes a WAV file to ogg
     * @param wav the wav file
     * @param out path of encoded ogg file
     * @param quality encoding quality (0 - 1.0f)
     * @return
     * @throws IOException
     */
    public static int encodeWav(InputStream wav, File out, long length, float quality, @Nullable ProgressListener listener) throws IOException {
        wav = new BufferedInputStream(wav);

        WavHeader header = new WavHeader(wav);
        VorbisEncoder encoder = new VorbisEncoder(out,
                "w",
                header.getNumChannels(),
                header.getSampleRate(),
                quality);

        try {
            encoder.encodeStream(wav, length, listener);
        } finally {
            encoder.release();
        }
        return 0;
    }

    public static int encodeWav(File in, File out, float quality, ProgressListener listener) throws IOException {
        FileInputStream inS = new FileInputStream(in);
        try {
            return encodeWav(inS, out, in.length(), quality, listener);
        } finally {
            IOUtils.close(inS);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (getState() >= STATE_READY) {
            Log.w(TAG, "unreleased encoder in finalize() - call release() when done with encoder");
            release();
        }
    }

    static {
        try {
            System.loadLibrary("soundcloud_vorbis_encoder");
        } catch (UnsatisfiedLinkError e) {
            // only ignore exception in non-android env
            if ("Dalvik".equals(System.getProperty("java.vm.name"))) throw e;
        }
    }
}
