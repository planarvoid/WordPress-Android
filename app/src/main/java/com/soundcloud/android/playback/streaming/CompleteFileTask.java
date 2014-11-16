package com.soundcloud.android.playback.streaming;

import static com.soundcloud.android.playback.streaming.StreamStorage.LOG_TAG;

import com.soundcloud.android.utils.IOUtils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

class CompleteFileTask extends AsyncTask<File, Integer, Boolean> {
    static final long MAX_MD5_CHECK_SIZE = 5 * 1024 * 1024; // don't md5 check files over 5MB

    private final long length;
    private final String etag;
    private final List<Integer> indexes;
    private final int chunkSize;

    public CompleteFileTask(long length, String etag, int chunkSize, List<Integer> indexes) {
        this.indexes = indexes;
        this.chunkSize = chunkSize;
        this.length = length;
        this.etag = etag;
    }

    @Override
    protected Boolean doInBackground(File... params) {
        File chunkFile = params[0];
        File completeFile = params[1];

        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, "About to write complete file to " + completeFile);
        }

        if (completeFile.exists()) {
            Log.e(LOG_TAG, "Complete file already exists at path " + completeFile.getAbsolutePath());
            return false;
        }
        // make sure complete dir exists
        else if (!completeFile.getParentFile().exists() && !IOUtils.mkdirs(completeFile.getParentFile())) {
            Log.w(LOG_TAG, "could not create complete file dir");
            return false;
        }
        // optimization - if chunks have been written in order, just move and truncate file
        else if (isOrdered(indexes)) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "chunk file is already in order, moving");
            }
            return move(chunkFile, completeFile) && checkEtag(completeFile, etag);
        } else {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "reassembling chunkfile");
            }
            return reassembleFile(chunkFile, completeFile) && checkEtag(completeFile, etag);
        }
    }

    private boolean checkEtag(File file, String etag) {
        if (etag == null || file.length() > MAX_MD5_CHECK_SIZE) {
            return true;
        }

        final String calculatedEtag = '"' + IOUtils.md5(file) + '"';
        if (!calculatedEtag.equals(etag)) {
            Log.w(LOG_TAG, "etag " + etag + " for complete file " + file + " does not match " + calculatedEtag);
            return false;
        } else {
            return true;
        }
    }

    private Boolean move(File chunkFile, File completeFile) {
        if (chunkFile.renameTo(completeFile)) {
            if (completeFile.length() != length) {
                try {
                    new RandomAccessFile(completeFile, "rw").setLength(length);
                    return true;
                } catch (IOException e) {
                    Log.w(LOG_TAG, e);
                }
            }
        } else {
            Log.w(LOG_TAG, "error moving file");
        }
        return false;
    }

    private boolean isOrdered(Iterable<Integer> indexes) {
        int last = 0;
        for (int i : indexes) {
            if (last > i) {
                return false;
            }
            last = i;
        }
        return true;
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private Boolean reassembleFile(File chunkFile, File completeFile) {
        FileOutputStream fos = null;
        RandomAccessFile raf = null;
        try {
            fos = new FileOutputStream(completeFile);
            raf = new RandomAccessFile(chunkFile, "r");

            byte[] buffer = new byte[chunkSize];
            for (int chunkNumber = 0; chunkNumber < indexes.size(); chunkNumber++) {
                int offset = chunkSize * indexes.indexOf(chunkNumber);
                raf.seek(offset);
                raf.readFully(buffer, 0, chunkSize);

                if (chunkNumber == indexes.size() - 1) {
                    fos.write(buffer, 0, (int) (length % chunkSize));
                } else {
                    fos.write(buffer);
                }
            }
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "complete file " + completeFile + " written");
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO error during complete file creation", e);
            if (completeFile.delete()) {
                Log.d(LOG_TAG, "Deleted " + completeFile);
            }
            return false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ignored) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }
}
