package com.soundcloud.android.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class WavFile implements AudioFile {
    private RandomAccessFile file;
    private WaveHeader header;

    public WavFile(File backing) throws IOException {
       file = new RandomAccessFile(backing, "r");
       header = new WaveHeader(new FileInputStream(backing));
    }

    @Override
    public AudioConfig getConfig() {
        return header.getAudioConfig();
    }

    @Override
    public void seek(long pos) throws IOException {
         file.seek(header.offset(pos));
    }

    public long getDuration() {
        return header.getDuration();
    }

    @Override
    public int read(ByteBuffer buffer, int length) throws IOException {
        return file.getChannel().read(buffer);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
