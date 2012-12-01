package com.soundcloud.android.cache;

import com.integralblue.httpresponsecache.HttpResponseCache;
import com.soundcloud.android.utils.IOUtils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.ResponseCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Local disk caching helper class, installs backported HttpResponseCache library.
 *
 * @see
 * <a href="https://github.com/candrews/HttpResponseCache">https://github.com/candrews/HttpResponseCache</a>
 * @see <a href="http://developer.android.com/reference/android/net/http/HttpResponseCache.html">Android HttpResponseCache</a>
 */
public final class FileCache  {
    public static final String TAG = FileCache.class.getSimpleName();

    public static final long   CACHE_AUTO = -1;
    public static final double MAX_PCT_OF_FREE_SPACE = 0.03d;      // 3% of free space
    public static final long   MAX_IMAGE_CACHE  = 20 * 1024  * 1024; // 20  MB

    public static ResponseCache installFileCache(File cacheDir) throws IOException {
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache instanceof HttpResponseCache) {
            Log.d(TAG, "Cache has already been installed.");
            return responseCache;
        } else if (responseCache == null) {
            final long actualSize = determineSize(cacheDir, CACHE_AUTO);
            Log.d(TAG, "using "+IOUtils.inMbFormatted(actualSize)+ " MB for image cache");
            return HttpResponseCache.install(cacheDir, actualSize);
        } else {
            Class<? extends ResponseCache> type = responseCache.getClass();
            Log.e(TAG, "Another ResponseCache has already been installed: " + type);
            return null;
        }
    }

    private static long determineSize(File dir, long size) {
        if (size == CACHE_AUTO) {
            return IOUtils.getUsableSpace(dir, MAX_IMAGE_CACHE, MAX_PCT_OF_FREE_SPACE);
        } else {
            return size;
        }
    }

    public static class DeleteCacheTask extends AsyncTask<File, Integer, Boolean> {
        private boolean recurse;

        public DeleteCacheTask(boolean recurse) {
            this.recurse = recurse;
        }

        @Override protected Boolean doInBackground(File... params) {
            final File dir = params[0];
            if (recurse) {
                deleteRecursively(dir);
            } else {
                deletePlain(dir);
            }
            return true;
        }

        private void deleteRecursively(File... dirs) {
            for (File d : dirs) if (d.isDirectory()) IOUtils.deleteDir(d);
        }

        private void deletePlain(File... dirs) {
            List<File> allFiles = new ArrayList<File>();
            for (File dir : dirs) if (dir.isDirectory()) allFiles.addAll(Arrays.asList(IOUtils.nullSafeListFiles(dir, null)));

            for (int i=0; i < allFiles.size(); i++) {
                File f = allFiles.get(i);
                if (!f.delete()) Log.w(TAG, "could not delete file "+f);
                publishProgress(i, allFiles.size());
            }
        }
    }


}
