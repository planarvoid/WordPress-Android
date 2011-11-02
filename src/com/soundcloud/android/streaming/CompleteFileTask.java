package com.soundcloud.android.streaming;

import static com.soundcloud.android.streaming.StreamStorage.LOG_TAG;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

class CompleteFileTask extends AsyncTask<File, Integer, Boolean> {
    private long mContentLength;
    private List<Integer> mIndexes;
    private int mChunkSize;

    public CompleteFileTask(long length, int chunkSize, List<Integer> indexes) {
        mIndexes = indexes;
        mChunkSize = chunkSize;
        mContentLength = length;
    }

    @Override
    protected Boolean doInBackground(File... params) {
        File chunkFile = params[0];
        File completeFile = params[1];

        Log.d(LOG_TAG, "About to write complete file to " + completeFile);

        if (completeFile.exists()) {
            Log.e(LOG_TAG, "Complete file already exists at path " + completeFile.getAbsolutePath());
            return false;
        }
        // optimization - if chunks have been written in order, just move and truncate file
        else if (isOrdered(mIndexes)) {
            Log.d(LOG_TAG, "chunk file is already in order, moving");
            return move(chunkFile, completeFile);
        } else {
            Log.d(LOG_TAG, "reassembling chunkfile");
            return reassembleFile(chunkFile, completeFile);
        }
    }

    private Boolean move(File chunkFile, File completeFile) {
        if (chunkFile.renameTo(completeFile)) {
            if (completeFile.length() != mContentLength) {
                try {
                    new RandomAccessFile(completeFile, "rw").setLength(mContentLength);
                    return true;
                } catch (IOException e) {
                    Log.w(LOG_TAG, e);
                }
            }
        } else {
            Log.w(LOG_TAG, "error moving files");
        }
        return false;
    }

    private boolean isOrdered(Iterable<Integer> indexes) {
        int last = 0;
        for (int i : indexes) {
            if (last > i) return false;
            last = i;
        }
        return true;
    }

    private Boolean reassembleFile(File chunkFile, File completeFile) {
        FileOutputStream fos = null;
        RandomAccessFile raf = null;
        try {
            fos = new FileOutputStream(completeFile);
            raf = new RandomAccessFile(chunkFile, "r");

            byte[] buffer = new byte[mChunkSize];
            for (int chunkNumber = 0; chunkNumber < mIndexes.size(); chunkNumber++) {
                int offset = mChunkSize * mIndexes.indexOf(chunkNumber);
                raf.seek(offset);
                raf.readFully(buffer, 0, mChunkSize);

                if (chunkNumber == mIndexes.size() - 1) {
                    fos.write(buffer, 0, (int) (mContentLength % mChunkSize));
                } else {
                    fos.write(buffer);
                }
            }
            Log.d(LOG_TAG, "complete file " + completeFile + " written");
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO error during complete file creation", e);
            if (completeFile.delete()) Log.d(LOG_TAG, "Deleted " + completeFile);
            return false;
        } finally {
            if (raf != null) try {
                raf.close();
            } catch (IOException ignored) {
            }
            if (fos != null) try {
                fos.close();
            } catch (IOException ignored) {
            }
        }
        return true;
    }
}
