package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;

public class DebugUtils {
    public static void dumpStack(Context context) {
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        final File debugDir = context.getExternalFilesDir("debug");
        if (debugDir != null) {
            IOUtils.mkdirs(debugDir);
            File traceFile = new File(debugDir, "traces-"+System.currentTimeMillis());

            try {
                PrintStream ps = new PrintStream(new FileOutputStream(traceFile));
                for (Map.Entry<Thread, StackTraceElement[]> e : traces.entrySet()) {
                    ps.println(e.getKey());

                    for (StackTraceElement se : e.getValue()) {
                        ps.println("  "+se);
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

    public static void dumpLog(Context context) {
        final File debugDir = context.getExternalFilesDir("debug");
        if (debugDir != null) {
            try {
                IOUtils.mkdirs(debugDir);
                File logFile = new File(debugDir, "log-"+System.currentTimeMillis());
                Process process = Runtime.getRuntime().exec("logcat -d");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                PrintStream ps = new PrintStream(new FileOutputStream(logFile));
                while ((line = bufferedReader.readLine()) != null) {
                    ps.println(line);
                }
                ps.close();
                Log.d(TAG, "wrote log to " + logFile);
            } catch (IOException e) {
                Log.w(TAG, "error writing logs");
            }
        } else {
            Log.w(TAG, "could not log because file not available");
        }
    }


    public static void setLogLevels() {
        System.getProperty("log.tag.SyncAdapterService", "DEBUG");
        System.getProperty("log.tag.ApiSyncService", "DEBUG");
        System.getProperty("log.tag.SyncManager", "DEBUG");
    }
}
