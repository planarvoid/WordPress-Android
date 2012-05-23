package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import org.apache.http.StatusLine;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;

public class DebugUtils {
    public static void dumpStack(@NotNull Context context) {
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        final File debugDir = context.getExternalFilesDir("debug");
        if (debugDir != null) {
            IOUtils.mkdirs(debugDir);
            File traceFile = new File(debugDir, "traces-" + System.currentTimeMillis());

            try {
                PrintStream ps = new PrintStream(new FileOutputStream(traceFile));
                for (Map.Entry<Thread, StackTraceElement[]> e : traces.entrySet()) {
                    ps.println(e.getKey());

                    for (StackTraceElement se : e.getValue()) {
                        ps.println("  " + se);
                    }
                    ps.println();
                }
                ps.close();
                Log.d(TAG, "dumped stack to " + traceFile);
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        } else {
            Log.w(TAG, "could not dump stack because file not available");
        }
    }

    public static boolean dumpLog(@NotNull Context context) {
        if (context.getPackageManager().checkPermission("android.permission.READ_LOGS", context.getPackageName())
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "no READ_LOGS permission, skipping dumpLog");
            return false;
        }

        final File debugDir = context.getExternalFilesDir("debug");
        if (debugDir != null) {
            try {
                IOUtils.mkdirs(debugDir);
                File logFile = new File(debugDir, "log-" + System.currentTimeMillis());
                Process process = Runtime.getRuntime().exec("logcat -d");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                PrintStream ps = new PrintStream(new FileOutputStream(logFile));
                while ((line = bufferedReader.readLine()) != null) {
                    ps.println(line);
                }
                ps.close();
                Log.d(TAG, "wrote log to " + logFile);
                return true;
            } catch (IOException e) {
                Log.w(TAG, "error writing logs");
                return false;
            }
        } else {
            Log.w(TAG, "could not log because file not available");
            return false;
        }
    }


    public static void setLogLevels() {
        System.getProperty("log.tag.SyncAdapterService", "DEBUG");
        System.getProperty("log.tag.ApiSyncService", "DEBUG");
        System.getProperty("log.tag.SyncManager", "DEBUG");
    }

    static class PlaybackError extends Exception {
        private final NetworkInfo networkInfo;
        private final StringBuilder buildProp;

        PlaybackError(IOException ioException, NetworkInfo info) {
            super(ioException);
            this.networkInfo = info;
            this.buildProp = getBuildProp();
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage()).append(" ")
                    .append("networkType: ").append(networkInfo == null ? null : networkInfo.getTypeName())
                    .append(" ")
                    .append("build.prop: ").append(buildProp);
            return sb.toString();
        }
    }

    public static class StatusException extends PlaybackError {
        private final StatusLine status;

        public StatusException(StatusLine status, NetworkInfo info) {
            super(null, info);
            this.status = status;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage());
            if (status != null) {
                sb.append(" status: ").append(status.toString());
            }
            return sb.toString();
        }
    }

    public static class MediaPlayerException extends PlaybackError {
        final int code, extra;

        public MediaPlayerException(int code, int extra, NetworkInfo info) {
            super(null, info);
            this.code = code;
            this.extra = extra;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage())
                    .append(" ")
                    .append("code: ").append(code).append(", extra: ").append(extra);
            return sb.toString();
        }
    }

    private static StringBuilder getBuildProp() {
            StringBuilder props = new StringBuilder();
            File f = new File("/system/build.prop");
            InputStream instream = null;
            try {
                instream = new BufferedInputStream(new FileInputStream(f));
                String line;
                BufferedReader buffreader = new BufferedReader(new InputStreamReader(instream));
                while ((line = buffreader.readLine()) != null) {
                    if (line.contains("media.stagefright")) {
                        props.append(line);
                        props.append(" ");
                    }
                }
                instream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return props;
        }
}
