package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.filecache.FileResponseCache;
import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.ResponseCache;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Local disk caching helper class. Uses FileResponseCache library found at
 * {@link} http://code.google.com/p/libs-for-android/
 * 
 * @author jschmidt
 */
public class FileCache extends FileResponseCache {

    private final Context mContext;

    public FileCache(Context context) {
        mContext = context;
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
        try {
            File parent = getCacheDir(mContext);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(String.valueOf(uri).getBytes("UTF-8"));
            byte[] output = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (byte anOutput : output) {
                builder.append(Integer.toHexString(0xFF & anOutput));
            }
            String filename = builder.toString();

            return new File(parent, filename);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void install(Context context) {
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache instanceof FileCache) {
            Log.d(TAG, "Cache has already been installed.");
        } else if (responseCache == null) {
            FileCache dropCache = new FileCache(context);
            ResponseCache.setDefault(dropCache);
        } else {
            Class<? extends ResponseCache> type = responseCache.getClass();
            Log.e(TAG, "Another ResponseCache has already been installed: " + type);
        }
    }

    public static double dirSizeInMb(File dir) {
        return CloudUtils.dirSize(dir) / 1048576d;
    }

    public static File getCacheDir(Context context) {
        return CloudUtils.isSDCardAvailable() ? Consts.EXTERNAL_CACHE_DIRECTORY : context.getCacheDir();
    }

    public static class DeleteCacheTask extends AsyncTask<File, Integer, Boolean> {
        private boolean recurse;

        public DeleteCacheTask(boolean recurse) {
            this.recurse = recurse;
        }

        @Override protected Boolean doInBackground(File... dirs) {
            if (recurse) deleteRecursively(dirs); else deletePlain(dirs);
            return true;
        }

        private void deleteRecursively(File[] dirs) {
            for (File d : dirs) if (d.isDirectory()) CloudUtils.deleteDir(d);
        }

        private void deletePlain(File[] dirs) {
            List<File> allFiles = new ArrayList<File>();
            for (File dir : dirs) if (dir.isDirectory()) allFiles.addAll(Arrays.asList(dir.listFiles()));

            for (int i=0; i < allFiles.size(); i++) {
                File f = allFiles.get(i);
                if (!f.delete()) Log.w(TAG, "could not delete file "+f);
                publishProgress(i, allFiles.size());
            }
        }
    }

    public static class TrimCacheTask extends AsyncTask<File, Integer, Boolean> {
        public long maxCacheSize = 1024 * 1024;
        @Override
        protected Boolean doInBackground(File... params) {
            final File dir = params[0];

            final long dirSize = CloudUtils.dirSize(dir);
            if (dirSize < maxCacheSize) return false;

            long toTrim = dirSize - maxCacheSize;

            File[] files = dir.listFiles();
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
