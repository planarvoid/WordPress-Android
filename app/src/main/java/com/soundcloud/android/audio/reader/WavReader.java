package com.soundcloud.android.audio.reader;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.WavHeader;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class WavReader extends AudioReader {
    private final File backing;

    private RandomAccessFile file;
    private WavHeader header;

    public static final String EXTENSION = "wav";

    public WavReader(File backing) throws IOException {
        this.backing = backing;
        doReopen();
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
            doReopen();
        } catch (IOException e) {
            Log.w(WavReader.class.getSimpleName(), e);
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    private void doReopen() throws IOException {
        if (file != null) file.close();
        file = new RandomAccessFile(backing, "r");
        header = new WavHeader(new FileInputStream(backing));
    }
}
