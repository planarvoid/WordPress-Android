package com.soundcloud.android.audio;

import com.soundcloud.android.jni.VorbisInfo;
import com.soundcloud.android.jni.VorbisDecoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VorbisFile extends AudioFile {
    private final VorbisDecoder decoder;
    private VorbisInfo info;

    public static final String EXTENSION = "ogg";

    public VorbisFile(File file) throws IOException {
        this(new VorbisDecoder(file));
    }

    public VorbisFile(VorbisDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public AudioConfig getConfig() {
        return AudioConfig.findMatching(getInfo().sampleRate, getInfo().channels);
    }

    @Override
    public void seek(long pos) throws IOException {
        final int ret = decoder.timeSeek(pos / 1000d);
        if (ret < 0) throw new IOException("timeSeek returned "+ret);
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
        } else {
            buffer.position(ret);
            return ret;
        }
    }

    @Override
    public File getFile() {
        return decoder.file;
    }

    @Override
    public void reopen() {
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
