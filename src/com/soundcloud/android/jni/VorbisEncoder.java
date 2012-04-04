package com.soundcloud.android.jni;

import com.soundcloud.android.record.AudioConfig;
import com.soundcloud.android.record.WaveHeader;

import java.io.BufferedInputStream;
import java.io.File;
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
    public void addSamples(InputStream is) throws IOException {
        ByteBuffer bbuffer = ByteBuffer.allocateDirect((int) (8192*channels*2));
        byte[] buffer = new byte[bbuffer.capacity()];
        int n;
        while ((n = is.read(buffer)) != -1) {
            bbuffer.rewind();
            bbuffer.put(buffer, 0, n);
            addSamples(bbuffer, n);
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
    public static int encodeWav(InputStream wav, File out, float quality) throws IOException {
        wav = new BufferedInputStream(wav);

        WaveHeader header = new WaveHeader(wav);
        VorbisEncoder encoder = new VorbisEncoder(out,
                "w",
                header.getNumChannels(),
                header.getSampleRate(),
                quality);

        encoder.addSamples(wav);
        return encoder.finish();
    }

    // native methods
    native public int init(String output, String mode, long channels, long rate, float quality);
    native public int addSamples(ByteBuffer samples, long length);
    native public int finish();

    static {
        System.loadLibrary("soundcloud_audio");
    }
}
