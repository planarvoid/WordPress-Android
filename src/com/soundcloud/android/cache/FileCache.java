package com.soundcloud.android.cache;

import com.google.android.filecache.CacheResponse;
import com.google.android.filecache.FileResponseCache;
import com.soundcloud.android.utils.CloudUtils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.net.ResponseCache;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Local disk caching helper class.
 *
 * Uses the new ICS
 * <a href="http://developer.android.com/reference/android/net/http/HttpResponseCache.html">HttpResponseCache</a>
 * if available or falls back to FileResponseCache library found at
 * <a href="http://code.google.com/p/libs-for-android/"> http://code.google.com/p/libs-for-android/</a>.
 */
public class FileCache extends FileResponseCache {
    public static final String TAG = FileCache.class.getSimpleName();

    private final File dir;
    private final long size;

    public FileCache(File dir, long size) {
        this.dir = dir;
        this.size = size;
    }

    @Override protected boolean isStale(File file, URI uri, String requestMethod, Map<String,
            List<String>> requestHeaders, Object cookie) {
        final boolean stale = super.isStale(file, uri, requestMethod, requestHeaders, cookie);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "file "+file.getName()+ ", stale:"+stale);
        }
        return stale;
    }

    @Override protected File getFile(URI uri, String requestMethod, Map<String, List<String>> requestHeaders, Object cookie) {
        return new File(dir, CloudUtils.md5(uri.toString()));
    }

    public CacheResponse getCacheResponse(String uri) {
        File f = getFile(URI.create(uri), null, null, null);
        if (f.exists()) {
            return new CacheResponse(f);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "FileCache{" +
                "dir=" + dir +
                ", size=" + size +
                '}';
    }

    /**
     * Installs a VM wide HTTP file cache - use the new HttpResponseCache in ICS or fall back
     * to {@link FileCache}.
     * <em>Only</em> active when using {@link java.net.HttpURLConnection}.
     * @param cacheDir where to store cache items
     * @param size     max cache size in bytes
     * @return         the installed response cache, or null if incompatible cache installed.
     */
    public static ResponseCache install(File cacheDir, long size) {
        try {
            ResponseCache cache = (ResponseCache) Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, cacheDir, size);
            Log.d(TAG, "Using ICS HttpResponseCache");
            return cache;
        } catch (Exception httpResponseCacheNotAvailable) {
            // not on ICS: use plain FileCache
            ResponseCache responseCache = ResponseCache.getDefault();
            if (responseCache instanceof FileCache) {
                Log.d(TAG, "Cache has already been installed.");
                return responseCache;
            } else if (responseCache == null) {
                FileCache cache = new FileCache(cacheDir, size);
                ResponseCache.setDefault(cache);
                return cache;
            } else {
                Class<? extends ResponseCache> type = responseCache.getClass();
                Log.e(TAG, "Another ResponseCache has already been installed: " + type);
                return null;
            }
        }
    }

    public static AsyncTask<FileCache, Integer, Boolean> trim(ResponseCache cache) {
        if (cache instanceof FileCache) {
            return new TrimCacheTask().execute((FileCache)cache);
        } else {
            return null;
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
            for (File d : dirs) if (d.isDirectory()) CloudUtils.deleteDir(d);
        }

        private void deletePlain(File... dirs) {
            List<File> allFiles = new ArrayList<File>();
            for (File dir : dirs) if (dir.isDirectory()) allFiles.addAll(Arrays.asList(dir.listFiles()));

            for (int i=0; i < allFiles.size(); i++) {
                File f = allFiles.get(i);
                if (!f.delete()) Log.w(TAG, "could not delete file "+f);
                publishProgress(i, allFiles.size());
            }
        }
    }

    public static class TrimCacheTask extends AsyncTask<FileCache, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(FileCache... params) {
            final FileCache cache = params[0];
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "trimming cache "+cache);
            }
            final long dirSize = CloudUtils.dirSize(cache.dir);
            final long maxCacheSize = cache.size;
            if (dirSize < maxCacheSize) return false;

            long toTrim = dirSize - maxCacheSize;

            File[] files = cache.dir.listFiles();
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    long result = f2.lastModified() - f1.lastModified();
                    if (result > 0) {
                        return -1;
                    } else if (result < 0) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });

            int i = 0;
            while (toTrim > 0 && i < files.length){
                final File file = files[i];
                final long filesize = file.length();
                if (!file.delete()) {
                    Log.w(TAG, "could not delete file " + file);
                } else {
                    toTrim -= filesize;
                }
                publishProgress(i, files.length);
                i++;
            }
            return true;
        }
    }
}
