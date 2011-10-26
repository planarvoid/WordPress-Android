package com.soundcloud.android.streaming;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

class ConvertFileToComplete extends AsyncTask<File, Integer, Boolean> {
    private long mContentLength;
    private List<Integer> mIndexes;
    private int mChunkSize;

    public ConvertFileToComplete(long length, int chunkSize, List<Integer> indexes) {
        mIndexes = indexes;
        mChunkSize = chunkSize;
        mContentLength = length;
    }

    @Override
    protected Boolean doInBackground(File... params) {
        File chunkFile = params[0];
        File completeFile = params[1];

        Log.d(StreamStorage.LOG_TAG, "writing complete file to " + completeFile);

        if (completeFile.exists()) {
            Log.e(StreamStorage.LOG_TAG, "Complete file exists at path " + completeFile.getAbsolutePath());
            return false;
        }

        FileOutputStream fos = null;
        RandomAccessFile raf = null;

        try {
            fos = new FileOutputStream(completeFile);
            raf = new RandomAccessFile(chunkFile, "r");

            if (!completeFile.createNewFile()) Log.w(StreamStorage.LOG_TAG, "could not create "+completeFile);
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
        } catch (IOException e) {
            Log.e(StreamStorage.LOG_TAG, "IO error during complete file creation");
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
        onPostExecute(true);
        return true;
    }


}
