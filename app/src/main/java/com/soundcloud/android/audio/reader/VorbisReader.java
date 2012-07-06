package com.soundcloud.android.audio.reader;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.jni.DecoderException;
import com.soundcloud.android.jni.VorbisInfo;
import com.soundcloud.android.jni.VorbisDecoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VorbisReader extends AudioReader {
    private VorbisDecoder decoder;
    private VorbisInfo info;

    public static final String EXTENSION = "ogg";

    public VorbisReader(File file) throws IOException {
        decoder = new VorbisDecoder(file);
    }

    @Override
    public AudioConfig getConfig() {
        return AudioConfig.findMatching(getInfo().sampleRate, getInfo().channels);
    }

    @Override
    public void seek(long pos) throws IOException {
        final int ret = decoder.timeSeek(pos / 1000d);
        if (ret < 0) throw new IOException("timeSeek("+pos+") returned "+ret);
    }

    @Override
    public long getDuration() {
        return (long) (getInfo().duration * 1000d);
    }

    @Override
    public long getPosition() {
        return (long) (decoder.timeTell() * 1000d);
    }

    @Override
    public int read(ByteBuffer buffer, int length) throws IOException {
        final int ret = decoder.decode(buffer, length);
        if (ret == 0) {
            return EOF;
        } else if (ret > 0) {
            buffer.position(ret);
            return ret;
        } else {
            throw new DecoderException("error decoding", ret);
        }
    }

    @Override
    public File getFile() {
        return decoder.file;
    }

    @Override
    public void reopen() throws DecoderException {
        File file = decoder.file;
        decoder.release();
        decoder = new VorbisDecoder(file);
        info = null;
    }

    @Override
    public void close() throws IOException {
        decoder.release();
    }

    private VorbisInfo getInfo() {
        if (info == null) {
            info = decoder.getInfo();
            if (info == null) info = new VorbisInfo();
        }
        return info;
    }
}
