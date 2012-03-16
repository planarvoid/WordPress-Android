package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

public final class IOUtils {
    private static final int BUFFER_SIZE = 4096;

    private IOUtils() {}

    public static long getDirSize(File dir) {
        long result = 0;
        File[] fileList = dir.listFiles();
        if (fileList == null) return 0;
        for (File aFileList : fileList) {
            if (aFileList.isDirectory()) {
                result += getDirSize(aFileList);
            } else {
                result += aFileList.length();
            }
        }
        return result;
    }

    public static long getSpaceLeft(File dir) {
        try {
            StatFs fs = new StatFs(dir.getAbsolutePath());
            return (long) fs.getBlockSize() * (long) fs.getAvailableBlocks();
        } catch (IllegalArgumentException e) {
            // gets thrown when call to statfs fails
            Log.e(TAG, "getSpaceLeft("+dir+")", e);
            return 0;
        }
    }

    public static File getFromMediaUri(ContentResolver resolver, Uri uri) {
        if (uri == null) return null;

        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        } else if ("content".equals(uri.getScheme())) {
            String[] filePathColumn = {MediaStore.MediaColumns.DATA};
            Cursor cursor = resolver.query(uri, filePathColumn, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        return new File(filePath);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return null;
    }

    public static String readInputStream(InputStream in) throws IOException {
        StringBuilder stream = new StringBuilder();
        byte[] b = new byte[BUFFER_SIZE];
        for (int n; (n = in.read(b)) != -1;) {
            stream.append(new String(b, 0, n));
        }
        return stream.toString();
    }

    public static boolean mkdirs(File d) {
        if (!d.exists()) {
            final boolean success = d.mkdirs();
            if (!success) Log.w(TAG, "mkdir " + d.getAbsolutePath() + " returned false");
            return success;
        } else {
            return false;
        }
    }

    public static boolean deleteFile(File f) {
        if (f != null && f.exists()) {
            if (!f.delete()) {
                Log.w(TAG, "could not delete "+f);
                return  false;
            } else return true;
        } else return false;
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File f : children) {
                    boolean success;
                    if (f.isDirectory()) {
                         success = deleteDir(f);
                    } else {
                        success = deleteFile(f);
                    }
                    if (!success) {
                        return false;
                    }
                }
            }
            // The directory is now empty so delete it
            return dir.delete();
        }
        return false;
    }

    public static File ensureUpdatedDirectory(File newDir, File deprecatedDir) {
        mkdirs(newDir);
        if (deprecatedDir.exists()) {
            for (File f : deprecatedDir.listFiles()) {
                if (!f.renameTo(new File(newDir, f.getName()))) {
                    Log.w(TAG, "could not rename "+f);
                }
            }
            deleteDir(deprecatedDir);
        }
        return newDir;
    }

    public static boolean renameCaseSensitive(File oldFile, File newFile){
        if (oldFile.equals(newFile)){
            return oldFile.renameTo(newFile);
        } else if (oldFile.getParentFile() == null) {
            return false;
        } else {
            File tmp = new File(oldFile.getParentFile(),"."+System.currentTimeMillis());
            return oldFile.renameTo(tmp) && tmp.renameTo(newFile);
        }
    }

    public static boolean fileExistsCaseSensitive(final File f) {
        return f.exists() && f.getParentFile() != null && f.getParentFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.equals(f.getName());
            }
        }).length > 0;
    }

    public static boolean nomedia(File dir) {
        if (!dir.isDirectory()) return false;

        File nomedia = new File(dir, ".nomedia");
        if (nomedia.exists() && nomedia.isFile()) {
            return true;
        } else {
            try {
                return nomedia.createNewFile();
            } catch (IOException e) {
                Log.w(TAG, "error creating .nomedia file");
                return false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void checkState(Context c) {
        mkdirs(getCacheDir(c));
        if (isSDCardAvailable()) {
            // remove old track cache directory if it exists
            if (Consts.EXTERNAL_TRACK_CACHE_DIRECTORY.exists()) {
                deleteDir(Consts.EXTERNAL_TRACK_CACHE_DIRECTORY);
            }

            // fix deprecated casing
            if (fileExistsCaseSensitive(Consts.DEPRECATED_EXTERNAL_STORAGE_DIRECTORY)) {
                boolean renamed = renameCaseSensitive(
                        Consts.DEPRECATED_EXTERNAL_STORAGE_DIRECTORY, Consts.EXTERNAL_STORAGE_DIRECTORY);
                Log.d(TAG, "Attempting to rename external storage: " + renamed);
            }

            // create external storage directory
            mkdirs(Consts.EXTERNAL_STORAGE_DIRECTORY);
            mkdirs(Consts.EXTERNAL_STREAM_DIRECTORY);

            // ignore all media below files
            nomedia(Consts.FILES_PATH);
        }
    }

    public static File getCacheDir(Context c) {
        if (isSDCardAvailable()) {
            return Consts.EXTERNAL_CACHE_DIRECTORY;
        } else {
            return c.getCacheDir();
        }
    }

    public static File getCacheFile(Context c, String name) {
          return new File(getCacheDir(c), name);
    }

    public static boolean isSDCardAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }


    public static long getUsableSpace(File dir, long maxSpace, double maxPct) {
        return getUsableSpace(getDirSize(dir), getSpaceLeft(dir), maxSpace, maxPct);
    }

    /**
     * @param usedSpace  the currently used space by the cache
     * @param spaceLeft  space left on the filesystem
     * @param maxSpace   the max space to use
     * @param maxPct     percentage of free space to use
     * @return total usable space
     */
    public static long getUsableSpace(long usedSpace, long spaceLeft, long maxSpace, double maxPct) {
        return Math.min((long) (Math.floor((usedSpace + spaceLeft) * maxPct)), maxSpace);
    }

    public static String inMbFormatted(File dir) {
        return inMbFormatted(getDirSize(dir));
    }

    public static String inMbFormatted(double bytes) {
        return new DecimalFormat("#.#").format(bytes / 1048576d);
    }

    public static String md5(String s) {
        return md5(new ByteArrayInputStream(s.getBytes()));
    }

    public static String md5(File f) {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            return md5(is);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) try { is.close(); } catch (IOException ignored) { }
        }
    }

    public static String md5(InputStream f) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = f.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            return CloudUtils.hexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "error", e);
            return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void fetchUriToFile(String url, File file, boolean useCache) throws FileNotFoundException {
        OutputStream os = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(useCache);
            conn.connect();
            final int status = conn.getResponseCode();
            if (status == HttpStatus.SC_OK) {
                InputStream is = conn.getInputStream();
                os = new BufferedOutputStream(new FileOutputStream(file));
                final byte[] buffer = new byte[8192];
                int n;
                while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                    os.write(buffer, 0, n);
                }
            } else {
                throw new FileNotFoundException("HttpStatus: "+status);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException(e.getMessage());
        } catch (IOException e) {
            deleteFile(file);
            throw new FileNotFoundException(e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
            if (os != null) try {
                os.close();
            } catch (IOException ignored) {
            }
        }
    }


    /**
     * @param context context
     * @param info current network info
     * @return the proxy to be used for the given network, or null
     */
    public static String getProxy(Context context, NetworkInfo info) {
        final String proxy;
        if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            // adjust mobile proxy settings
            String proxyHost = Proxy.getHost(context);
            if (proxyHost == null) {
                proxyHost = Proxy.getDefaultHost();
            }
            int proxyPort = Proxy.getPort(context);
            if (proxyPort == -1) {
                proxyPort = Proxy.getDefaultPort();
            }
            if (proxyHost != null && proxyPort > -1) {
                proxy = new HttpHost(proxyHost, proxyPort).toURI();
            } else {
                proxy = null;
            }
        } else {
            proxy = null;
        }
        return proxy;
    }

    public static HttpClient createHttpClient(String userAgent) {
        if (Build.VERSION.SDK_INT >= 8) {
            return AndroidHttpClient.newInstance(userAgent);
        } else {
            return new DefaultHttpClient();
        }
    }

    public static void closeHttpClient(HttpClient client) {
        if (client instanceof AndroidHttpClient) {
            // avoid leak error logging
            ((AndroidHttpClient) client).close();
        } else if (client != null) {
            client.getConnectionManager().shutdown();
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info =  mgr == null ? null : mgr.getActiveNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }

    public static boolean isWifiConnected(Context c) {
        ConnectivityManager mgr = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mgr == null ? null : mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return info != null && info.isConnectedOrConnecting();
    }
}
