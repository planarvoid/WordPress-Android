package com.soundcloud.android.streaming;

import static com.soundcloud.android.streaming.StreamStorage.LOG_TAG;

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

        Log.d(LOG_TAG, "About to write complete file to " + completeFile);

        if (completeFile.exists()) {
            Log.e(LOG_TAG, "Complete file exists at path " + completeFile.getAbsolutePath());
            return false;
        }

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
            if (completeFile.delete()) Log.d(LOG_TAG, "Deleted "+completeFile);
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
