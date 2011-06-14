
package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.filecache.FileResponseCache;

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
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Local disk caching helper class. Uses FileResponseCache library found at
 * {@link} http://code.google.com/p/libs-for-android/
 * 
 * @author jschmidt
 */
public class CloudCache extends FileResponseCache {
    public static final File EXTERNAL_CACHE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files/.cache/");

    public static final File EXTERNAL_TRACK_CACHE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files/.s/");

    private final Context mContext;

    public CloudCache(Context context) {
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

    public static double cacheSizeInMb(Context c) {
        return dirSize(getCacheDir(c)) / 1048576d;
    }

    public static String cacheSizeInMbFormatted(Context c) {
        double sizeRaw = cacheSizeInMb(c);
        DecimalFormat maxDigitsFormatter = new DecimalFormat("#.##");
        return maxDigitsFormatter.format(sizeRaw);
    }

    private static long dirSize(File dir) {
        long result = 0;
        File[] fileList = dir.listFiles();
        if (fileList == null) return 0;
        for (File aFileList : fileList) {
            if (aFileList.isDirectory()) {
                result += dirSize(aFileList);
            } else {
                result += aFileList.length();
            }
        }
        return result;
    }

    public static File getCacheDir(Context context) {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ?
                EXTERNAL_CACHE_DIRECTORY :
                context.getCacheDir();
    }

    public static class DeleteCacheTask extends AsyncTask<File, Integer, Boolean> {
        @Override protected Boolean doInBackground(File... params) {
            final File dir = params[0];
            File[] files = dir.listFiles();
            File file;
            for (int i = 0; i < files.length; i++) {
                file = files[i];
                if (!file.delete()) Log.w(TAG, "could not delete file "+file);
                publishProgress(i, files.length);
            }
            return true;
        }
    }
}
