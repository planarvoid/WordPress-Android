package com.soundcloud.android.creators.record.jni;

import com.soundcloud.android.creators.record.AudioConfig;
import com.soundcloud.android.creators.record.PlaybackFilter;
import com.soundcloud.android.creators.record.WavHeader;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.creators.record.reader.WavReader;
import com.soundcloud.android.utils.BufferUtils;
import com.soundcloud.android.utils.IOUtils;

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

    public static final int STATE_READY = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_CLOSED = 2;

    @SuppressWarnings("UnusedDeclaration") // used in JNI code
    private int encoder_state;

    /**
     * Initialises the encoder for usage.
     *
     * @param file     output file
     * @param mode     the file mode ('w', 'a', ...)
     * @param channels number of channels
     * @param rate     sampleRate (44100)
     * @param quality  desired encoding quality (0-1.0)
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
     *
     * @param is inputstream containing samples
     * @throws IOException
     */
    public void encodeStream(InputStream is) throws IOException {
        encodeStream(is, -1, EncoderOptions.DEFAULT);
    }

    /**
     * Convenience method to add samples from an inputstream
     *
     * @param is      inputstream containing samples
     * @param length  total length of data
     * @param options encoding options
     * @throws IOException
     */
    public void encodeStream(InputStream is, long length, EncoderOptions options) throws IOException {
        Log.d(TAG, "encodeStream(length=" + length + ", " + options + ")");

        PlaybackFilter filter = options.filter;
        ProgressListener listener = options.listener;
        ByteBuffer bbuffer = BufferUtils.allocateAudioBuffer(AudioConfig.DEFAULT.bytesPerSecond * 2);
        final byte[] buffer = new byte[bbuffer.capacity()];
        int n;
        long total = 0;
        while ((n = is.read(buffer)) != -1) {
            bbuffer.rewind();
            bbuffer.put(buffer, 0, n);
            bbuffer.flip();

            if (filter != null) {
                filter.apply(bbuffer, total, length);
            }

            int ret = write(bbuffer, n);

            if (ret < 0) {
                throw new EncoderException("addSamples returned error", ret);
            }
            total += n;
            if (listener != null) {
                listener.onProgress(total, length);
            }
        }
    }

    /**
     * Sets the record head to a different position in the recording. The next
     * call to {@link #write(java.nio.ByteBuffer, long)} will append to this position.
     * All content after msecs gets automatically truncated.
     *
     * @param pos position in secs, or -1 for end of file
     * @throws EncoderException      if the head could not be moved
     * @throws IllegalStateException if not paused
     */
    public boolean startNewStream(double pos) throws IOException {
        Log.d(TAG, "startNewStream(" + pos + ")");

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


    /**
     * Extract the part of an Ogg file between given start and/or end times.
     *
     * @param in    ogg input file path
     * @param out   ogg output file path
     * @param start start time in secs
     * @param end   end time in secs, -1 for whole length
     * @throws EncoderException if case of error
     */
    public static void extract(File in, File out, double start, double end) throws EncoderException {
        int res = chop(in.getAbsolutePath(), out.getAbsolutePath(), start, end);
        if (res != 0) {
            throw new EncoderException("Eror extracting ogg", res);
        }
    }

    /**
     * Validate the ogg stream
     *
     * @param in the input file
     * @return true if valid
     */
    public static boolean validate(File in) {
        return validate(in.getAbsolutePath()) == 0;
    }

    /**
     * @param in      file to be encoded, either vorbis or wav format
     * @param out     the output file
     * @param options encoding options
     * @return always 0
     * @throws IOException
     */
    public static int encodeFile(File in, File out, EncoderOptions options) throws IOException {
        // guess based on extension first
        final String extension = IOUtils.extension(in);
        if (WavReader.EXTENSION.equals(extension)) {
            return encodeWav(in, out, options);
        } else if (VorbisReader.EXTENSION.equals(extension)) {
            return encodeVorbis(in, out, options);
        } else {
            // wav file ?
            try {
                WavHeader.fromFile(in);
                return encodeWav(in, out, options);
            } catch (IOException ignored) {
            }
            // vorbis file ?
            VorbisDecoder dec = null;
            try {
                dec = new VorbisDecoder(in);
                return encodeVorbis(in, out, options);
            } catch (DecoderException ignored) {
            } finally {
                if (dec != null) {
                    dec.release();
                }
            }
            throw new IOException("File format of " + in + " is not supported");
        }
    }

    /**
     * @param in      a wav input file
     * @param out     output file
     * @param options encoding options
     * @return always 0
     * @throws IOException
     */
    public static int encodeWav(File in, File out, EncoderOptions options) throws IOException {
        return encodeWav(new BufferedInputStream(new FileInputStream(in)), out, options);
    }

    public static int encodeWav(InputStream in, File out, EncoderOptions options) throws IOException {
        try {
            WavHeader header = new WavHeader(in);

            VorbisEncoder encoder = new VorbisEncoder(out,
                    "w",
                    header.getNumChannels(),
                    header.getSampleRate(),
                    options.quality);

            WavHeader.AudioData data = header.getAudioData(options.start, options.end);

            try {
                encoder.encodeStream(data.stream, data.length, options);
                encoder.closeStream();
            } finally {
                encoder.release();
            }
            return 0;
        } finally {
            IOUtils.close(in);
        }
    }

    /**
     * @param in      input file (vorbis format)
     * @param out     output file
     * @param options encoding options
     * @return always 0
     * @throws IOException
     */
    public static int encodeVorbis(File in, File out, EncoderOptions options) throws IOException {
        VorbisDecoder decoder = new VorbisDecoder(in);
        final VorbisInfo info = decoder.getInfo();

        final PlaybackFilter filter = options.filter;
        final ProgressListener listener = options.listener;

        VorbisEncoder encoder = new VorbisEncoder(out,
                "w",
                info.channels,
                info.sampleRate,
                options.quality);

        if (options.start != 0) {
            final int error = decoder.timeSeek(options.start / 1000d);
            if (error != 0) {
                throw new EncoderException("Could not seek", error);
            }
        }

        ByteBuffer buffer = BufferUtils.allocateAudioBuffer(16384);

        int read, total = 0;
        try {
            while ((read = decoder.decode(buffer, buffer.capacity())) > 0 &&
                    (options.end == -1 || decoder.timeTell() < options.end / 1000d)) {

                if (filter != null) {
                    filter.apply(buffer, total, read);
                }
                encoder.write(buffer, read);
                total += read;

                if (listener != null) {
                    listener.onProgress((long) decoder.timeTell(), (long) info.duration);
                }
            }
            encoder.closeStream();
        } finally {
            encoder.release();
            decoder.release();
        }
        if (read < 0) {
            throw new EncoderException("Error encoding", read);
        }
        return 0;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (getState() >= STATE_READY) {
            Log.w(TAG, "unreleased encoder in finalize() - call release() when done with encoder");
            release();
        }
    }

    //
    // native methods
    //

    /**
     * Add some samples to the current file.
     *
     * @param samples
     * @param length
     * @return number of bytes written, < 0 in error case
     */
    native public int write(ByteBuffer samples, long length);


    /**
     * Pauses the current encoding process (closes the file).
     */
    native public int pause();

    /**
     * Call to free up resources. The encoder cannot be used after this method has been called.
     *
     * @throws IllegalStateException if the encoder has been released previously
     */
    native public void release();

    /**
     * @return the current state ({@link #STATE_READY} = ready to encode,
     * {@link #STATE_PAUSED} = paused, < 0 uninitialised)
     */
    public native int getState();

    native private int init(String output, String mode, long channels, long rate, float quality);

    /**
     * Closes the current stream, writing EOS packet.
     *
     * @return 0 for success
     */
    native private int closeStream();

    /**
     * {@link #extract(java.io.File, java.io.File, double, double)}
     *
     * @return 0 for success
     */
    native private static int chop(String in, String out, double start, double end);

    native private static int validate(String in);

    static {
        try {
            System.loadLibrary("soundcloud_vorbis_encoder");
        } catch (UnsatisfiedLinkError e) {
            // only ignore exception in non-android env
            if ("Dalvik".equals(System.getProperty("java.vm.name"))) {
                throw e;
            }
        }
    }
}
