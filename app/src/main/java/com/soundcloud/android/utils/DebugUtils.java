package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.common.base.Charsets;
import com.soundcloud.android.Consts;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.streaming.StreamItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import java.util.HashMap;
import java.util.Map;


public final class DebugUtils {

    public static final String UTF_8_ENC = Charsets.UTF_8.displayName();

    @SuppressWarnings("UnusedDeclaration")
    public static void dumpIntent(Intent intent) {
        Log.d(TAG, "dumpIntent("+intent+")");

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Map<String, Object> map = new HashMap<>();
            for (String k : extras.keySet()) {
                Object value = extras.get(k);
                map.put(k, value);
            }
            Log.d(TAG, "extras: "+map);
        }
    }

    public static void dumpStack(@NotNull Context context) {
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        final File debugDir = context.getExternalFilesDir("debug");
        if (debugDir != null) {
            IOUtils.mkdirs(debugDir);
            File traceFile = new File(debugDir, "traces-" + System.currentTimeMillis());

            try {
                PrintStream ps = new PrintStream(new FileOutputStream(traceFile), true, UTF_8_ENC);
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
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8_ENC));
                String line;
                PrintStream ps = new PrintStream(new FileOutputStream(logFile), true, UTF_8_ENC);
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

    public static void reportMediaPlayerError(Context context, StreamItem item, int what, int extra) {
        if (Log.isLoggable(PlaybackService.TAG, Log.DEBUG)) {
            Log.d(PlaybackService.TAG,
                    String.format("mediaPlayer error (what: %d, extra: %d, item: %s)", what, extra, item));
        }
        if (shouldReportPlaybackErrors(context)) {
            NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                    .getActiveNetworkInfo();

            // only report when there's an active connection
            if (info != null && info.isConnectedOrConnecting()) {
                ErrorUtils.handleSilentException("mp error", new MediaPlayerException(what, extra, info, item));
            }
        }
    }

    private static boolean shouldReportPlaybackErrors(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Consts.PrefKeys.PLAYBACK_ERROR_REPORTING_ENABLED, false
        );
    }

    private static class PlaybackError extends Exception {
        private final NetworkInfo networkInfo;
        private final String buildProp;

        PlaybackError(@Nullable IOException ioException, NetworkInfo info) {
            super(ioException);
            this.networkInfo = info;
            this.buildProp = getBuildProp();
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder(500);
            sb.append(super.getMessage())
                    .append(" networkType: ").append(networkInfo == null ? null : networkInfo.getTypeName())
                    .append(" build.prop: ").append(buildProp);
            return sb.toString();
        }
    }

    private static class MediaPlayerException extends PlaybackError {
        final int code, extra;
        final StreamItem item;

        public MediaPlayerException(int code, int extra, NetworkInfo info, StreamItem item) {
            super(null, info);
            this.code = code;
            this.extra = extra;
            this.item = item;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder(500);
            sb.append(super.getMessage())
                    .append(" code: ").append(code)
                    .append(", extra: ").append(extra)
                    .append(", item: ").append(item);
            return sb.toString();
        }
    }

    private static String getBuildProp() {
        StringBuilder props = new StringBuilder();
        File f = new File("/system/build.prop");
        InputStream instream;
        try {
            instream = new BufferedInputStream(new FileInputStream(f));
            String line;
            BufferedReader buffreader = new BufferedReader(new InputStreamReader(instream, UTF_8_ENC));
            while ((line = buffreader.readLine()) != null) {
                if (line.contains("media.stagefright")) {
                    props.append(line);
                    props.append(' ');
                }
            }
            instream.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return props.toString();
    }

    private DebugUtils() {}
}
