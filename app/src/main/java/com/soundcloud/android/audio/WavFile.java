package com.soundcloud.android.audio;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class WavFile implements AudioFile {
    private RandomAccessFile file;
    private final File backing;
    private final WavHeader header;

    public WavFile(File backing) throws IOException {
        this.backing = backing;
        file = new RandomAccessFile(backing, "r");
        file.seek(WavHeader.LENGTH);
        header = new WavHeader(new FileInputStream(backing));
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
    public long getPosition() {
        try {
            return getConfig().bytesToMs(file.getFilePointer() - WavHeader.LENGTH);
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public int read(ByteBuffer buffer, int length) throws IOException {
        return file.getChannel().read(buffer);
    }

    @Override
    public File getFile() {
        return backing;
    }

    @Override
    public void reopen() {
        try {
            if (file != null) file.close();
            file = new RandomAccessFile(backing, "r");
        } catch (IOException e) {
            Log.w(WavFile.class.getSimpleName(), e);
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
