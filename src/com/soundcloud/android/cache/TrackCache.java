package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;

import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TrackCache {

    /**
     * @param excludeFile the file to exclude from the cache deletion
     * @param cacheDir    the cache dir
     * @return if the cache dir was trimmed
     * @throws IOException IO problems
     */
    public static boolean trim(File excludeFile, File cacheDir) throws IOException {
        if (!cacheDir.exists()) return false;
        long size = 0;

        StatFs fs = new StatFs(cacheDir.getAbsolutePath());
        final long spaceLeft = fs.getBlockSize() * fs.getAvailableBlocks();

        File[] fileList = cacheDir.listFiles();
        if (fileList != null) {
            ArrayList<File> orderedFiles = new ArrayList<File>();
            for (File file : fileList) {
                if (!file.isDirectory() && (excludeFile == null || !file.equals(excludeFile))) {
                    size += file.length();
                }
                if (orderedFiles.size() == 0) {
                    orderedFiles.add(file);
                } else {
                    int j = 0;
                    while (j < orderedFiles.size()
                            && (orderedFiles.get(j)).lastModified() < file.lastModified()) {
                        j++;
                    }
                    orderedFiles.add(j, file);
                }
            }

            Log.i(TAG, "Current Cache Size " + size + " (space left " + spaceLeft + ")");

            if (size > Consts.TRACK_MAX_CACHE || spaceLeft < Consts.TRACK_CACHE_MIN_FREE_SPACE) {
                final long toTrim = Math.max(size - Consts.TRACK_MAX_CACHE, Math.abs(spaceLeft));
                int j = 0;
                long trimmed = 0;
                // XXX PUT THIS CODE UNDER TEST (INFINITE LOOPS)
                while (j < orderedFiles.size() && trimmed < toTrim) {
                    final File moribund = orderedFiles.get(j);
                    if (!moribund.equals(excludeFile)) {
                        Log.v(TAG, "Trimming " + moribund);
                        trimmed += moribund.length();
                        if (!moribund.delete()) {
                            Log.w(TAG, "error deleting " + moribund);
                        }
                    }
                    j++;
                }
            }
        }
        return true;
    }
}
