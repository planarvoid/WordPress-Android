package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

public class IOUtils {
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
        StatFs fs = new StatFs(dir.getAbsolutePath());
        return (long) fs.getBlockSize() * (long) fs.getAvailableBlocks();
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
                return name.contentEquals(f.getName());
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
}
