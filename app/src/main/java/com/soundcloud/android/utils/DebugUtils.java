package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;


public final class DebugUtils {

    public static final String UTF_8_ENC = Charsets.UTF_8.displayName();

    public static String getLogDump(int logTailLineCount) {
        BufferedReader bufferedReader = null;
        try {
            Process process = Runtime.getRuntime().exec(String.format("logcat -v time -d -t %d", logTailLineCount));
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder logDump = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logDump.append(line);
            }
            return logDump.toString();
        } catch (IOException e) {
            return ScTextUtils.EMPTY_STRING;
        } finally {
            IOUtils.close(bufferedReader);
        }
    }

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

    private DebugUtils() {}
}
