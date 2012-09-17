package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import org.apache.http.HttpHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Comparator;

public final class IOUtils {
    private static final int BUFFER_SIZE = 4096;

    private IOUtils() {}

    public static @NotNull File[] nullSafeListFiles(File f, @Nullable FilenameFilter filter) {
        if (f == null) return new File[0];
        File[] files;
        if (filter != null) {
            files = f.listFiles(filter);
        } else {
            files = f.listFiles();
        }
        return files == null ? new File[0] : files;
    }

    public static long getDirSize(File dir) {
        long result = 0;
        File[] fileList = nullSafeListFiles(dir, null);
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

    public static long getTotalSpace(File dir) {
        try {
            StatFs fs = new StatFs(dir.getAbsolutePath());
            return (long) fs.getBlockSize() * (long) fs.getBlockCount();
        } catch (IllegalArgumentException e) {
            // gets thrown when call to statfs fails
            Log.e(TAG, "getTotalSpace("+dir+")", e);
            return 0;
        }
    }

    public static File getFromMediaUri(ContentResolver resolver, Uri uri) {
        if (uri == null) return null;

        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        } else if ("content".equals(uri.getScheme())) {
            final String[] filePathColumn = { MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME };
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, filePathColumn, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int columnIndex = (uri.toString().startsWith("content://com.google.android.gallery3d")) ?
                            cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME) :
                            cursor.getColumnIndex(MediaStore.MediaColumns.DATA); // if it is a picasa image on newer devices with OS 3.0 and up
                    if (columnIndex != -1) {
                        return new File(cursor.getString(columnIndex));
                    }
                }
            } catch (SecurityException ignored) {
                // nothing to be done
            } finally {
                if (cursor != null) cursor.close();
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

    public static byte[] readInputStreamAsBytes(InputStream in) throws IOException {
        byte[] b = new byte[BUFFER_SIZE];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        int n;
        while ((n = in.read(b)) != -1) {
            bos.write(b, 0, n);
        }
        bos.close();
        in.close();
        return bos.toByteArray();
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
            for (File f : nullSafeListFiles(dir, null)) {
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
            // The directory is now empty so delete it
            return dir.delete();
        }
        return false;
    }

    public static File ensureUpdatedDirectory(File newDir, File deprecatedDir) {
        mkdirs(newDir);
        if (deprecatedDir.exists()) {
            for (File f : nullSafeListFiles(deprecatedDir, null)) {
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
        if (f != null && f.exists() && f.getParentFile() != null) {
            File[] files = nullSafeListFiles(f.getParentFile(), new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.equals(f.getName());
                }
            });
            return files.length > 0;
        } else return false;
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
            return ScTextUtils.hexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "error", e);
            return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public static void copy(InputStream is, File out) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buffer = new byte[BUFFER_SIZE];
        int n;
        while ((n = is.read(buffer)) != -1) {
            fos.write(buffer, 0, n);
        }
        fos.close();
    }

    public static void copy(File in, File out) throws IOException {
        final FileInputStream is = new FileInputStream(in);
        try {
            copy(is, out);
        } finally {
            is.close();
        }
    }

    public static void close(Closeable file) {
        if (file != null) {
            try {
                file.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static @NotNull File appendToFilename(File file, String text) {
        String name = file.getName();
        final int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            String ext = name.substring(lastDot, name.length());
            return new File(file.getParentFile(), name.substring(0, lastDot)+text+ext);
        } else {
            return new File(file.getParentFile(), file.getName()+text);
        }
    }

    public static @Nullable String extension(File file) {
        final String name = file.getName();
        final int lastDot = name.lastIndexOf('.');
        if (lastDot != -1 && lastDot != name.length() -1) {
            return name.substring(lastDot+1, name.length()).toLowerCase();
        } else {
            return null;
        }
    }

    public static @NotNull File changeExtension(File file, String ext) {
        final String name = file.getName();
        final int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            return new File(file.getParentFile(), name.substring(0, lastDot)+"."+ext);
        } else {
            return new File(file.getParentFile(), file.getName()+"."+ext);
        }
    }

    public static @NotNull File removeExtension(@NotNull File file) {
        if (file.isDirectory()) return file;
        String name = file.getName();
        final int lastPeriodPos = name.lastIndexOf('.');
        return lastPeriodPos <= 0 ? file : new File(file.getParent(), name.substring(0, lastPeriodPos));
    }

    public static void skipFully(InputStream in, long n) throws IOException {
        while (n > 0) {
            long amt = in.skip(n);
            if (amt == 0) {
                // Force a blocking read to avoid infinite loop
                if (in.read() == -1) {
                    throw new EOFException();
                }
                n--;
            } else {
                n -= amt;
            }
        }
    }

    /**
     * some phones have really low transfer rates when the screen is turned off, so request a full
     * performance lock on newer devices
     *
     * @see <a href="http://code.google.com/p/android/issues/detail?id=9781">http://code.google.com/p/android/issues/detail?id=9781</a>
     */
    public static WifiManager.WifiLock createHiPerfWifiLock(Context context, String tag) {
        return ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD ?
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF : WifiManager.WIFI_MODE_FULL,
                        tag
                );
    }
}
