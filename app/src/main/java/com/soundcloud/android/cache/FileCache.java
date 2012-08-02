package com.soundcloud.android.cache;

import com.google.android.filecache.ScFileCacheResponse;
import com.google.android.filecache.FileResponseCache;
import com.soundcloud.android.utils.FiletimeComparator;
import com.soundcloud.android.utils.IOUtils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.net.ResponseCache;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static final long IMAGE_CACHE_AUTO = -1;
    public static final double MAX_PCT_OF_FREE_SPACE = 0.03d;      // 3% of free space
    public static final long MAX_IMAGE_CACHE  = 20 * 1024  * 1024; // 20  MB

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
        return new File(dir, IOUtils.md5(uri.toString()));
    }

    public ScFileCacheResponse getCacheResponse(String uri) {
        File f = getFile(URI.create(uri), null, null, null);
        if (f.exists()) {
            return new ScFileCacheResponse(f);
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
    public static ResponseCache autoInstall(File cacheDir, long size) {
        // currently not used because it's very slow and adds 15,20 secs to some requests (ICS 4.0.2)
        // TODO: find out why
        try {
            final long actualSize = determineSize(cacheDir, size);
            Log.d(TAG, "using "+IOUtils.inMbFormatted(actualSize)+ " MB for image cache");
            ResponseCache cache = (ResponseCache) Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, cacheDir, actualSize);
            Log.d(TAG, "Using ICS HttpResponseCache");
            return cache;
        } catch (Exception httpResponseCacheNotAvailable) {
            return installFileCache(cacheDir, size);
        }
    }

    public static ResponseCache installFileCache(File cacheDir, long size) {
        // not on ICS: use plain FileCache
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache instanceof FileCache) {
            Log.d(TAG, "Cache has already been installed.");
            return responseCache;
        } else if (responseCache == null) {
            final long actualSize = determineSize(cacheDir, size);
            Log.d(TAG, "using "+IOUtils.inMbFormatted(actualSize)+ " MB for image cache");
            FileCache cache = new FileCache(cacheDir, actualSize);
            ResponseCache.setDefault(cache);
            return cache;
        } else {
            Class<? extends ResponseCache> type = responseCache.getClass();
            Log.e(TAG, "Another ResponseCache has already been installed: " + type);
            return null;
        }
    }

    private static long determineSize(File dir, long size) {
        if (size == IMAGE_CACHE_AUTO) {
            return IOUtils.getUsableSpace(dir, MAX_IMAGE_CACHE, MAX_PCT_OF_FREE_SPACE);
        } else {
            return size;
        }
    }

    public AsyncTask<FileCache, Integer, Boolean> trim() {
        return new TrimCacheTask().execute(this);
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

    public static class TrimCacheTask extends AsyncTask<FileCache, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(FileCache... params) {
            final FileCache cache = params[0];
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "trimming cache "+cache);
            }
            final long dirSize = IOUtils.getDirSize(cache.dir);
            final long maxCacheSize = cache.size;
            if (dirSize < maxCacheSize) return false;

            long toTrim = dirSize - maxCacheSize;

            final File[] files = IOUtils.nullSafeListFiles(cache.dir, null);
            Arrays.sort(files, new FiletimeComparator(true));

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
