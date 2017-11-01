package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.util.Locale;

public class IOUtils {

    private static final int BUFFER_SIZE = 8192;
    private static final int FREE_SPACE_BUFFER = 100 * 1024 * 1024;

    @Inject
    public IOUtils() {
    }

    @Nullable
    public static File createExternalStorageDir(Context context, String dir) {
        return createDir(getExternalStorageDir(context), dir);
    }

    @Nullable
    public static File createSDCardDir(Context context, String dir) {
        return createDir(getSDCardDir(context), dir);
    }

    @Nullable
    public static File getExternalStorageDir(Context context) {
        final File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        return externalFilesDirs == null || externalFilesDirs.length == 0 ? null : externalFilesDirs[0];
    }

    @Nullable
    public static File getSDCardDir(Context context) {
        final File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        if (externalFilesDirs == null || externalFilesDirs.length == 0 || externalFilesDirs.length == 1) {
            return null;
        }
        return externalFilesDirs[1];
    }

    @Nullable
    private static File createDir(File path, String folder) {
        if (path != null) {
            final File dir = new File(path, folder);
            dir.mkdirs();
            return dir;
        } else {
            return null;
        }
    }

    public static boolean isSDCardMounted(Context context) {
        File sdCardDir = getSDCardDir(context);
        return sdCardDir != null && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(sdCardDir));
    }

    public static long getExternalStorageFreeSpace(Context context) {
        return getStorageFreeSpace(getExternalStorageDir(context));
    }

    public static long getExternalStorageTotalSpace(Context context) {
        return getStorageTotalSpace(getExternalStorageDir(context));
    }

    public static long getSDCardStorageFreeSpace(Context context) {
        return getStorageFreeSpace(getSDCardDir(context));
    }

    public static long getSDCardStorageTotalSpace(Context context) {
        return getStorageTotalSpace(getSDCardDir(context));
    }

    public static long getStorageFreeSpace(File file) {
        return file == null ? 0 : Math.max(file.getFreeSpace() - FREE_SPACE_BUFFER, 0);
    }

    public static long getStorageTotalSpace(File file) {
        return file == null ? 0 : file.getTotalSpace();
    }

    @NotNull
    public static File[] nullSafeListFiles(File f, @Nullable FilenameFilter filter) {
        if (f == null) {
            return new File[0];
        }
        File[] files;
        if (filter != null) {
            files = f.listFiles(filter);
        } else {
            files = f.listFiles();
        }
        return files == null ? new File[0] : files;
    }

    public long dirSize(File... directories) {
        return getDirSize(directories);
    }

    @Deprecated // use dirSize()
    public static long getDirSize(File... directories) {
        long result = 0;
        for (File dir : directories) {
            File[] fileList = nullSafeListFiles(dir, null);
            for (File file : fileList) {
                if (file.isDirectory() &&
                        !dir.equals(file) /* check should not be necessary, but SO on some version of android */
                        ) {
                    result += getDirSize(file);
                } else {
                    result += file.length();
                }
            }
        }
        return result;
    }

    public static File getFromMediaUri(ContentResolver resolver, Uri uri) {
        if (uri == null) {
            return null;
        }

        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        } else if ("content".equals(uri.getScheme())) {
            final String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, filePathColumn, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int columnIndex = (uri.toString().startsWith("content://com.google.android.gallery3d")) ?
                                            cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME) :
                                            cursor.getColumnIndex(MediaStore.MediaColumns.DATA); // if it is a picasa image on newer devices with OS 3.0 and up
                    if (columnIndex != -1) {
                        String path = cursor.getString(columnIndex);
                        if (path != null) {
                            return new File(path);
                        }
                    }
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
                // nothing to be done
                // When uri comes from google's drive, it crashes with _data not found
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return null;
    }

    public static String getFilenameFromUri(Uri stream, ContentResolver resolver) {
        String[] projections = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = resolver.query(stream, projections, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                if (title != null) {
                    return title;
                }
            }
        } catch (SecurityException | IllegalArgumentException ignore) {
            // ignore
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return stream.getLastPathSegment();
    }

    public static String readInputStream(InputStream in) throws IOException {
        final byte[] bytes = readInputStreamAsBytes(in);
        return new String(bytes, 0, bytes.length, "UTF-8");
    }

    public static byte[] readInputStreamAsBytes(InputStream in) throws IOException {
        return readInputStreamAsBytes(in, BUFFER_SIZE);
    }

    private static byte[] readInputStreamAsBytes(InputStream in, final int contentLength) throws IOException {
        byte[] b = new byte[contentLength];
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

    public static void writeFileFromString(File file, String content) {
        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(content.getBytes("UTF-8"));
        } catch (IOException e) {
            ErrorUtils.handleThrowable(e, IOUtils.class);
        } finally {
            IOUtils.close(output);
        }
    }

    static void consumeStream(@Nullable HttpURLConnection connection) {
        try {
            if (connection != null) {
                final int contentLength = connection.getContentLength();
                if (contentLength > 0) {
                    readInputStreamAsBytes(connection.getInputStream(), contentLength);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean mkdirs(File d) {
        if (!d.exists()) {
            final boolean success = d.mkdirs();
            if (!success) {
                Log.w(TAG, "mkdir " + d.getAbsolutePath() + " returned false");
            }
            return success;
        } else {
            return false;
        }
    }

    public static boolean deleteFile(File f) {
        if (f != null && f.exists()) {
            if (!f.delete()) {
                Log.w(TAG, "could not delete " + f);
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
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

    public static boolean isEmptyDir(@NotNull File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            return files == null || files.length == 0;
        }
        throw new IllegalArgumentException("Argument " + dir.toString() + " is not a directory");
    }

    public static boolean cleanDir(@NotNull File dir) {
        boolean result = true;
        if (dir.isDirectory()) {
            if (isEmptyDir(dir)) {
                return true;
            }
            for (File aFile : dir.listFiles()) {
                if (aFile.isDirectory()) {
                    result = result && deleteDir(aFile);
                } else {
                    result = result && deleteFile(aFile);
                }
            }
            return result;
        }
        throw new IllegalArgumentException("Argument " + dir.toString() + " is not a directory " +
                                                   "(exists=" + dir.exists() + ", " +
                                                   "canWrite=" + dir.canWrite() + ", canRead=" + dir.canRead() + ")");
    }

    public static boolean renameCaseSensitive(File oldFile, File newFile) {
        if (oldFile.equals(newFile)) {
            return oldFile.renameTo(newFile);
        } else if (oldFile.getParentFile() == null) {
            return false;
        } else {
            File tmp = new File(oldFile.getParentFile(), "." + System.currentTimeMillis());
            return oldFile.renameTo(tmp) && tmp.renameTo(newFile);
        }
    }

    private static boolean nomedia(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }

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

    public static void createCacheDirs(Context context, @Nullable File streamCacheDirectory) {
        if (isExternalStorageAvailable()) {
            if (streamCacheDirectory != null) {
                mkdirs(streamCacheDirectory);
            }

            // create external storage directory
            final File externalStorageDirectory = getExternalStorageDir(context);
            if (externalStorageDirectory != null) {
                mkdirs(externalStorageDirectory);
                // ignore all media below files
                nomedia(externalStorageDirectory);
            } else {
                ErrorUtils.handleSilentException(new IllegalStateException("External storage directory not available"));
            }

        }
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * @param spaceLeft space left on the filesystem
     * @param maxSpace  the max space to use
     * @return max usable space
     */
    static long getMaxUsableSpace(long spaceLeft, long maxSpace) {
        return Math.min(maxSpace, spaceLeft);
    }

    static String inMbFormatted(File... directories) {
        return inMbFormatted(getDirSize(directories));
    }

    static String inMbFormatted(double bytes) {
        return new DecimalFormat("#.#").format(bytes / 1048576d);
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mgr == null ? null : mgr.getActiveNetworkInfo();
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

    /**
     * Closes a cursor. These cannot use the {@link this#close(java.io.Closeable)}
     * function as cursors do not implement Closeable pre-honecomb
     */
    public static void close(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    @NotNull
    public static File appendToFilename(File file, String text) {
        String name = file.getName();
        final int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            String ext = name.substring(lastDot, name.length());
            return new File(file.getParentFile(), name.substring(0, lastDot) + text + ext);
        } else {
            return new File(file.getParentFile(), file.getName() + text);
        }
    }

    @Nullable
    public static String extension(File file) {
        final String name = file.getName();
        final int lastDot = name.lastIndexOf('.');
        if (lastDot != -1 && lastDot != name.length() - 1) {
            return name.substring(lastDot + 1, name.length()).toLowerCase(Locale.US);
        } else {
            return null;
        }
    }

    @NotNull
    public static File changeExtension(File file, String ext) {
        final String name = file.getName();
        final int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            return new File(file.getParentFile(), name.substring(0, lastDot) + "." + ext);
        } else {
            return new File(file.getParentFile(), file.getName() + "." + ext);
        }
    }

    @NotNull
    public static File removeExtension(@NotNull File file) {
        if (file.isDirectory()) {
            return file;
        }
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
        return ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, tag);
    }

    public static boolean checkReadExternalStoragePermission(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setMessage(R.string.crop_external_permission_rationale)
                        .setPositiveButton(R.string.ok_got_it, (dialogInterface, i) -> requestExternalStoragePermission(activity));
                builder.show();
            } else {
                requestExternalStoragePermission(activity);
            }
            return false;
        } else {
            return true;
        }
    }

    public static boolean checkReadExternalStoragePermission(final Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(fragment.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getContext())
                        .setMessage(R.string.crop_external_permission_rationale)
                        .setPositiveButton(R.string.ok_got_it, (dialogInterface, i) -> requestExternalStoragePermission(fragment));
                builder.show();
            } else {
                requestExternalStoragePermission(fragment);
            }
            return false;
        } else {
            return true;
        }
    }

    @TargetApi(16)
    private static void requestExternalStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                          Consts.RequestCodes.REQUEST_EXTERNAL_STORAGE_PERMISSION);
    }

    @TargetApi(16)
    private static void requestExternalStoragePermission(Fragment fragment) {
        fragment.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    Consts.RequestCodes.REQUEST_EXTERNAL_STORAGE_PERMISSION);
    }

    public static boolean isExternalStoragePermissionGranted(int requestCode, int[] grantResults) {
        return requestCode == Consts.RequestCodes.REQUEST_EXTERNAL_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }


}
