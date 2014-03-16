package com.soundcloud.android.tests;

import com.soundcloud.android.utils.IOUtils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

public class LogCollector {
    private static LogCollector instance;
    private static boolean deleteFile;
    private final File logsDir;
    private Process logcat;
    private String testCaseName;

    private LogCollector() {
        logsDir = getLogsDir();

        clearLogsDir();
        IOUtils.mkdirs(logsDir);
    }

    public static void startCollecting(String testCaseName) throws Exception {
        if (instance == null) {
            instance = new LogCollector();
        }
        instance.setTestCaseName(testCaseName);
        instance.clearLogcat();
        instance.collectLogs();
    }

    public static void stopCollecting() throws Exception {
        if(instance == null) {
            throw new Exception("Please start collecting first");
        }
        instance.stopCollectingLogs();
    }

    public static void markFileForDeletion() {
        deleteFile = true;
    }

    private void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    private void stopCollectingLogs() {
        logcat.destroy();
        deleteLogFile();
    }

    private void deleteLogFile() {
        if(deleteFile) {
            pathToFile().delete();
            deleteFile = false;
        }
    }

    private void collectLogs() throws IOException {
        logcat = Runtime.getRuntime().exec("/system/bin/logcat -f " + pathToFile().toString());
    }

    private File pathToFile() {
        return new File(logsDir, testCaseName);
    }

    private void clearLogsDir() {
        IOUtils.deleteDir(logsDir);
    }

    private void clearLogcat() throws IOException {
        Runtime.getRuntime().exec("/system/bin/logcat -c");
    }

    private File getLogsDir() {
        return new File(Environment.getExternalStorageDirectory(), "RobotiumLogs");
    }
}
