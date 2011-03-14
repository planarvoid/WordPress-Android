
package com.soundcloud.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.ResponseCache;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.android.filecache.FileResponseCache;

/**
 * Local disk caching helper class. Uses FileResponseCache library found at
 * {@link} http://code.google.com/p/libs-for-android/
 * 
 * @author jschmidt
 */
public class CloudCache extends FileResponseCache {
    private static final String TAG = "CloudCache";

    public static final String EXTERNAL_CACHE_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Android/data/com.soundcloud.android/files/.cache/";

    public static final String EXTERNAL_TRACK_CACHE_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Android/data/com.soundcloud.android/files/.s/";

    public static void install(Context context) {
        ResponseCache responseCache = ResponseCache.getDefault();
        if (responseCache instanceof CloudCache) {
            Log.d(TAG, "Cache has already been installed.");
        } else if (responseCache == null) {
            CloudCache dropCache = new CloudCache(context);
            ResponseCache.setDefault(dropCache);
        } else {
            Class<? extends ResponseCache> type = responseCache.getClass();
            Log.e(TAG, "Another ResponseCache has already been installed: " + type);
        }
    }

    public static double cacheSizeInMbDouble(Context c) {
        return Double.parseDouble(Long.toString(dirSize(getCacheDir(c)))) / 1048576;
    }

    public static String cacheSizeInMbString(Context c) {
        Double sizeRaw = cacheSizeInMbDouble(c);
        DecimalFormat maxDigitsFormatter = new DecimalFormat("#.##");
        return maxDigitsFormatter.format(sizeRaw);
    }

    /**
     * Return the size of a directory in bytes
     */
    private static long dirSize(File dir) {
        long result = 0;
        File[] fileList = dir.listFiles();

        if (fileList == null)
            return 0;

        for (int i = 0; i < fileList.length; i++) {
            // Recursive call if it's a directory
            if (fileList[i].isDirectory()) {
                result += dirSize(fileList[i]);
            } else {
                // Sum the file size in bytes
                result += fileList[i].length();
            }
        }
        Log.i(TAG, "RETURNING " + result);
        return result; // return the file size
    }

    private static File getCacheDir(Context context) {

        File cacheDir;

        // Choose chache directory
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED))
            cacheDir = new File(EXTERNAL_CACHE_DIRECTORY);
        else
            cacheDir = context.getCacheDir();

        return cacheDir;
    }

    private final Context mContext;

    public CloudCache(Context context) {
        mContext = context;
    }

    @Override
    protected boolean isStale(File file, URI uri, String requestMethod,
            Map<String, List<String>> requestHeaders, Object cookie) {
        if (cookie instanceof Long) {
            Long maxAge = (Long) cookie;

            long age = System.currentTimeMillis() - file.lastModified();
            Log.i(TAG, "IS STALE MAX AGE " + age + " | " + maxAge.longValue());
            if (age > maxAge.longValue()) {
                return true;
            }
        }
        Boolean ret = super.isStale(file, uri, requestMethod, requestHeaders, cookie);
        Log.i(TAG, "Normal return val " + ret);
        return ret;
    }

    @Override
    protected File getFile(URI uri, String requestMethod, Map<String, List<String>> requestHeaders,
            Object cookie) {
        try {
            File parent = getCacheDir(mContext);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(String.valueOf(uri).getBytes("UTF-8"));
            byte[] output = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < output.length; i++) {
                builder.append(Integer.toHexString(0xFF & output[i]));
            }
            String filename = builder.toString();
            return new File(parent, filename);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class DeleteCacheTask extends AsyncTask<String, Integer, Boolean> {
        public WeakReference<Activity> mActivityRef;

        public Boolean preserveDirs = false;

        public void setActivity(Activity activity) {
            mActivityRef = new WeakReference<Activity>(activity);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (mActivityRef.get() == null)
                return false;

            File folder = getCacheDir(mActivityRef.get());
            File[] files = folder.listFiles();
            File file;
            int length = files.length;
            for (int i = 0; i < length; i++) {
                file = files[i];
                if (!preserveDirs || file.isFile()) {
                    file.delete();
                    publishProgress(i, length);
                }
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }
    }

}
