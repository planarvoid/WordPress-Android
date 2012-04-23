package com.soundcloud.android.audio;

import com.soundcloud.android.jni.Info;
import com.soundcloud.android.jni.VorbisDecoder;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VorbisFile implements AudioFile {
    private VorbisDecoder decoder;
    private Info info;

    public VorbisFile(VorbisDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public AudioConfig getConfig() {
        switch (getInfo().channels) {
            case 1: return AudioConfig.PCM16_44100_1;
            case 2: return AudioConfig.PCM16_44100_2;
            default: return AudioConfig.DEFAULT;
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        final int ret = decoder.timeSeek(pos);
        if (ret < 0) throw new IOException("timeSeek returned "+ret);
    }

    @Override
    public long getDuration() {
        return (long) getInfo().duration;
    }

    @Override
    public int read(ByteBuffer buffer, int length) throws IOException {
        return decoder.decode(buffer, length);
    }

    @Override
    public void close() throws IOException {
        decoder.release();
    }

    private Info getInfo() {
        if (info == null) {
            info = decoder.getInfo();
        }
        return info;
    }
}
