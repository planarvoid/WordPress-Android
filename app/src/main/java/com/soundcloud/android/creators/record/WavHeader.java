/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.creators.record;

import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.LimitInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Locale;

/**
 * This class represents the header of a WAVE format audio file, which usually
 * have a .wav suffix.  The following integer valued fields are contained:
 * <ul>
 * <li> format - usually PCM, ALAW or ULAW.
 * <li> numChannels - 1 for mono, 2 for stereo.
 * <li> sampleRate - usually 8000, 11025, 16000, 22050, or 44100 hz.
 * <li> bitsPerSample - usually 16 for PCM, 8 for ALAW, or 8 for ULAW.
 * <li> numBytes - size of audio data after this header, in bytes.
 * </ul>
 *
 * @see <a href="http://ccrma.stanford.edu/courses/422/projects/WaveFormat">WAVE format</a>
 * @see <a href="http://www.devdaily.com/java/jwarehouse/android/core/java/android/speech/srec/WaveHeader.java.shtml">WaveHeader.java</a>
 */
@SuppressWarnings({"PointlessBitwiseExpression", "UnusedDeclaration"})
public class WavHeader {
    public static final int LENGTH = 44;

    /**
     * Indicates PCM format.
     */
    public static final short FORMAT_PCM = 1;
    /**
     * Indicates ALAW format.
     */
    public static final short FORMAT_ALAW = 6;
    /**
     * Indicates ULAW format.
     */
    public static final short FORMAT_ULAW = 7;

    private short format;
    private short numChannels;
    private int sampleRate;
    private short bitsPerSample;
    private int numBytes;

    private InputStream is;

    /**
     * Initialises the WaveHeader data from an InputStream.
     * The stream will be positioned at the first byte of the actual data.
     *
     * @param is the InputStream containing WAV data
     * @throws IOException
     */
    public WavHeader(InputStream is) throws IOException {
        this(is, false);
    }

    /**
     * @param is     the InputStream containing WAV data
     * @param rewind whether to rewind the stream after reading the header
     * @throws IOException
     */
    public WavHeader(InputStream is, boolean rewind) throws IOException {
        if (rewind) {
            if (is.markSupported()) {
                is.mark(LENGTH + 32);
            } else {
                throw new IOException("asked to rewind but " + is.getClass() + " does not support it");
            }
        }
        read(is);
        if (rewind) {
            is.reset();
        }
        this.is = is;
    }


    /**
     * Construct a WaveHeader, with fields initialized.
     *
     * @param format        format of audio data,
     *                      one of {@link #FORMAT_PCM}, {@link #FORMAT_ULAW}, or {@link #FORMAT_ALAW}.
     * @param numChannels   1 for mono, 2 for stereo.
     * @param sampleRate    typically 8000, 11025, 16000, 22050, or 44100 hz.
     * @param bitsPerSample usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     * @param numBytes      size of audio data after this header, in bytes.
     */
    public WavHeader(short format, short numChannels, int sampleRate, short bitsPerSample, int numBytes) {
        this.format = format;
        this.sampleRate = sampleRate;
        this.numChannels = numChannels;
        this.bitsPerSample = bitsPerSample;
        this.numBytes = numBytes;
    }

    /**
     * Get the format field.
     *
     * @return format field,
     * one of {@link #FORMAT_PCM}, {@link #FORMAT_ULAW}, or {@link #FORMAT_ALAW}.
     */
    public short getFormat() {
        return format;
    }

    /**
     * Set the format field.
     *
     * @param format one of {@link #FORMAT_PCM}, {@link #FORMAT_ULAW}, or {@link #FORMAT_ALAW}.
     * @return reference to this WaveHeader instance.
     */
    public WavHeader setFormat(short format) {
        this.format = format;
        return this;
    }

    /**
     * Get the number of channels.
     *
     * @return number of channels, 1 for mono, 2 for stereo.
     */
    public short getNumChannels() {
        return numChannels;
    }

    /**
     * Set the number of channels.
     *
     * @param numChannels 1 for mono, 2 for stereo.
     * @return reference to this WaveHeader instance.
     */
    public WavHeader setNumChannels(short numChannels) {
        this.numChannels = numChannels;
        return this;
    }

    /**
     * Get the sample rate.
     *
     * @return sample rate, typically 8000, 11025, 16000, 22050, or 44100 hz.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Set the sample rate.
     *
     * @param sampleRate sample rate, typically 8000, 11025, 16000, 22050, or 44100 hz.
     * @return reference to this WaveHeader instance.
     */
    public WavHeader setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    /**
     * Get the number of bits per sample.
     *
     * @return number of bits per sample,
     * usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     */
    public final short getBitsPerSample() {
        return bitsPerSample;
    }

    public final int getBytesPerSample() {
        return getBitsPerSample() / 8;
    }

    /**
     * Set the number of bits per sample.
     *
     * @param bitsPerSample number of bits per sample,
     *                      usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     * @return reference to this WaveHeader instance.
     */
    public WavHeader setBitsPerSample(short bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
        return this;
    }

    /**
     * Get the size of audio data after this header, in bytes.
     *
     * @return size of audio data after this header, in bytes.
     */
    public final int getNumBytes() {
        return numBytes;
    }

    /**
     * Set the size of audio data after this header, in bytes.
     *
     * @param numBytes size of audio data after this header, in bytes.
     * @return reference to this WaveHeader instance.
     */
    public WavHeader setNumBytes(int numBytes) {
        this.numBytes = numBytes;
        return this;
    }

    /**
     * @return the duration, in ms
     */
    public final long getDuration() {
        return getAudioConfig().bytesToMs(numBytes);
    }

    /**
     * Read and initialize a WaveHeader.
     *
     * @param in {@link java.io.InputStream} to read from.
     * @return number of bytes consumed.
     * @throws IOException
     */
    private int read(InputStream in) throws IOException {
        /* RIFF header */
        readId(in, "RIFF");
        @SuppressWarnings("UnusedDeclaration")
        int numOfBytes = readInt(in) - 36;
        readId(in, "WAVE");

        /* fmt chunk */
        readId(in, "fmt ");
        if (16 != readInt(in)) {
            throw new IOException("fmt chunk length not 16");
        }
        format = readShort(in);
        numChannels = readShort(in);
        sampleRate = readInt(in);
        int byteRate = readInt(in);
        short blockAlign = readShort(in);
        bitsPerSample = readShort(in);
        if (byteRate != numChannels * sampleRate * getBytesPerSample()) {
            throw new IOException("fmt.ByteRate field inconsistent");
        }
        if (blockAlign != numChannels * getBytesPerSample()) {
            throw new IOException("fmt.BlockAlign field inconsistent");
        }

        /* data chunk */
        readId(in, "data");
        this.numBytes = readInt(in);

        return LENGTH;
    }

    private static void readId(InputStream in, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) != in.read()) {
                throw new IOException(id + " tag not present");
            }
        }
    }

    private static int readInt(InputStream in) throws IOException {
        return in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    }

    private static short readShort(InputStream in) throws IOException {
        return (short) (in.read() | (in.read() << 8));
    }

    /**
     * @param ms the time in milliseconds
     * @return the byte offset into the file
     */
    public long offset(final long ms) {
        if (ms < 0) {
            return WavHeader.LENGTH;
        } else {
            final long offset = Math.min(numBytes, getAudioConfig().msToByte(ms));
            return LENGTH + getAudioConfig().validBytePosition(offset);
        }
    }

    /**
     * Write a WAVE file header.
     *
     * @param out {@link java.io.OutputStream} to receive the header.
     * @return number of bytes written.
     * @throws IOException
     */
    public int write(OutputStream out) throws IOException {
        /* RIFF header */
        writeId(out, "RIFF");
        writeInt(out, 36 + numBytes);
        writeId(out, "WAVE");

        /* fmt chunk */
        writeId(out, "fmt ");
        writeInt(out, 16);
        writeShort(out, format);
        writeShort(out, numChannels);
        writeInt(out, sampleRate);
        writeInt(out, numChannels * sampleRate * getBytesPerSample());
        writeShort(out, (short) (numChannels * getBytesPerSample()));
        writeShort(out, bitsPerSample);

        /* data chunk */
        writeId(out, "data");
        writeInt(out, numBytes);

        return LENGTH;
    }

    public int write(DataOutput out) throws IOException {
        /* RIFF header */
        out.writeBytes("RIFF");
        out.writeInt(36 + numBytes);
        out.writeBytes("WAVE");

        /* fmt chunk */
        out.writeBytes("fmt ");
        out.writeInt(Integer.reverseBytes(16));
        out.writeShort(Short.reverseBytes((short) 1));
        out.writeShort(Short.reverseBytes(numChannels));
        out.writeInt(Integer.reverseBytes(sampleRate));
        out.writeInt(Integer.reverseBytes(numChannels * sampleRate * getBytesPerSample()));
        out.writeShort(Short.reverseBytes((short) (numChannels * getBytesPerSample())));
        out.writeShort(Short.reverseBytes(bitsPerSample));

        /* data chunk */
        out.writeBytes("data");
        out.writeInt(numBytes);

        return LENGTH;
    }

    private static void writeId(OutputStream out, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            out.write(id.charAt(i));
        }
    }

    private static void writeInt(OutputStream out, int val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
        out.write(val >> 16);
        out.write(val >> 24);
    }

    private static void writeShort(OutputStream out, short val) throws IOException {
        out.write(val >> 0);
        out.write(val >> 8);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                             "WaveHeader format=%d numChannels=%d sampleRate=%d bitsPerSample=%d numBytes=%d",
                             format, numChannels, sampleRate, bitsPerSample, numBytes);
    }

    public AudioConfig getAudioConfig() {
        if (format == FORMAT_PCM && bitsPerSample == 16) {
            return AudioConfig.findMatching(sampleRate, numChannels);
        }
        throw new IllegalArgumentException("unknown audioformat: " + toString());
    }

    /**
     * Gets the audio data for this wav stream, respecting offsets.
     * <em>This method can only be called once.</em>
     *
     * @param start start position in msec
     * @param end   end pos in msec, -1 for end of file
     * @return an inputstream with partial audio data
     * @throws IOException
     */
    public AudioData getAudioData(long start, long end) throws IOException {
        InputStream stream = is;
        long length = numBytes;

        AudioConfig config = getAudioConfig();
        if (start > 0) {
            final long offset = Math.min(numBytes, config.validBytePosition(config.msToByte(start)));
            IOUtils.skipFully(is, offset);
            length -= offset;
        }

        if (end > 0) {
            final long endPos = Math.min(numBytes, config.validBytePosition(config.msToByte(end)));
            stream = new LimitInputStream(is, endPos - (numBytes - length));
            length -= (numBytes - endPos);
        }
        return new AudioData(stream, length);
    }

    /**
     * Changes the WAV header to reflect the new file length. Needed after appending
     * to an existing file.
     *
     * @param file the wav file
     * @return true for success
     * @throws IOException
     */
    public static boolean fixLength(RandomAccessFile file) throws IOException {
        final long fileLength = file.length();
        if (fileLength == 0) {
            return false;
        } else if (fileLength > LENGTH) {
            // remaining bytes
            file.seek(4);
            file.writeInt(Integer.reverseBytes((int) (fileLength - 8)));
            // total bytes
            file.seek(LENGTH - 4);
            file.writeInt(Integer.reverseBytes((int) (fileLength - LENGTH)));
            return true;
        } else {
            return false;
        }
    }

    public static
    @NotNull
    WavHeader fromFile(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        WavHeader h = new WavHeader(fis);
        fis.close();
        return h;
    }

    public static void writeHeader(File f, int length) throws IOException {
        WavHeader h = new WavHeader(WavHeader.FORMAT_PCM, (short) 1, 44100, (short) 16, length);
        OutputStream os = new FileOutputStream(f);
        h.write(os);
        os.close();
    }

    public static class AudioData {
        public final InputStream stream;
        public final long length;

        AudioData(InputStream is, long length) {
            this.stream = is;
            this.length = length;
        }
    }
}