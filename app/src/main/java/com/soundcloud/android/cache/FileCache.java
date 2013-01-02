package com.soundcloud.android.cache;

import com.integralblue.httpresponsecache.HttpResponseCache;
import com.soundcloud.android.SoundCloudApplication;
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
    public static final long   MAX_IMAGE_CACHE  = 20 * 1024  * 1024; // 20  MB

    public static ResponseCache installFileCache(final File cacheDir)  {
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache instanceof HttpResponseCache) {
            Log.d(TAG, "Cache has already been installed.");
            return responseCache;

        } else if (responseCache == null) {
            final long size = determineAvailableSpace(cacheDir);

            if (size > 0) {
                Log.d(TAG, "using "+IOUtils.inMbFormatted(size)+ " MB for image cache");

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            if (cacheDir.exists() || cacheDir.mkdirs()) {
                                HttpResponseCache.install(cacheDir, size);
                            }
                        } catch (IOException e) {
                            Log.w(TAG, "error installing cache", e);
                        } catch (IllegalArgumentException e) {
                            Log.w(TAG, "error installing cache", e);
                            SoundCloudApplication.handleSilentException("Error installing cache, SD Avail: "
                                    + IOUtils.isSDCardAvailable(), e);
                        }
                    }
                }.start();

                return null;
            } else {
                Log.d(TAG, "not using disk cache - not enough space left");
                return null;
            }
        } else {
            Class<? extends ResponseCache> type = responseCache.getClass();
            Log.e(TAG, "Another ResponseCache has already been installed: " + type);
            return null;
        }
    }

    private static long determineAvailableSpace(File dir) {
        if (IOUtils.getSpaceLeft(dir) < MAX_IMAGE_CACHE * 3) {
            return 0;
        } else {
            return MAX_IMAGE_CACHE;
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
