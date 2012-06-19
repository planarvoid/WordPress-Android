package com.soundcloud.android.jni;

import com.soundcloud.android.audio.AudioConfig;
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

    public VorbisEncoder(File file, String mode, AudioConfig config) throws EncoderException {
        this(file, mode, config.channels, config.sampleRate, config.quality);
    }

    /**
     * Convenience method to add samples from an inputstream
     * @param is inputstream containing samples
     * @throws IOException
     */
    public void encodeStream(InputStream is, long length, ProgressListener listener) throws IOException {
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

    native private int init(String output, String mode, long channels, long rate, float quality);

    /**
     * Add some samples to the current file.
     * @param samples
     * @param length
     * @return < 0 in error case
     */
    native public int write(ByteBuffer samples, long length);


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
     * @return the current state (0 = ready to encode, 1 = paused, < 0 uninitialised)
     */
    native int getState();


    public static void extract(File in, File out, double start, double end) throws EncoderException {
        int res = chop(in.getAbsolutePath(), out.getAbsolutePath(), start, end);
        if (res != 0) {
            throw new EncoderException("Eror extracting ogg", res);
        }
    }

    /**
     * Extract the part of an Ogg file between given start and/or end times.
     *
     * @param in ogg input file path
     * @param out ogg output file path
     * @param start start time in secs
     * @param end end time in secs
     * @return 0 for success
     */
    native public static int chop(String in, String out, double start, double end);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (getState() >= 0) {
            Log.w(TAG, "unreleased encoder in finalize() - call release() when done with encoder");
            release();
        }
    }

    static {
        try {
            System.loadLibrary("soundcloud_audio_encoder");
        } catch (UnsatisfiedLinkError e) {
            // only ignore exception in non-android env
            if ("Dalvik".equals(System.getProperty("java.vm.name"))) throw e;
        }
    }
}
