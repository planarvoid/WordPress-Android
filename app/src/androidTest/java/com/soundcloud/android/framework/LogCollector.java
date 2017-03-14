package com.soundcloud.android.framework;

import com.soundcloud.android.utils.IOUtils;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class LogCollector {
    private static LogCollector instance;
    private static boolean deleteFile;
    private final File logsDir;
    private Process logcat;
    private String testCaseName;

    private LogCollector(Context context) {
        logsDir = getLogsDir(context);

        clearLogsDir(context);
        IOUtils.mkdirs(logsDir);
    }

    public static void startCollecting(Context context, String testCaseName) throws Exception {
        if (instance == null) {
            instance = new LogCollector(context);
        }
        instance.setTestCaseName(testCaseName);
        instance.clearLogcat();
        instance.collectLogs();
    }

    public static void stopCollecting() throws Exception {
        if (instance == null) {
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
        if (deleteFile) {
            pathToFile().delete();
            deleteFile = false;
        }
    }

    private void collectLogs() throws IOException {
        logcat = Runtime.getRuntime().exec("/system/bin/logcat -f " + pathToFile().toString());
    }

    private File pathToFile() {
        return new File(logsDir, testCaseName + ".log");
    }

    private void clearLogsDir(Context context) {
        //http://stackoverflow.com/questions/11539657/open-failed-ebusy-device-or-resource-busy
        File newLogsDir = new File(getLogsDir(context).getAbsolutePath() + "_old");
        IOUtils.renameCaseSensitive(getLogsDir(context), newLogsDir);
        IOUtils.deleteDir(newLogsDir);
    }

    private void clearLogcat() throws IOException {
        Runtime.getRuntime().exec("/system/bin/logcat -c");
    }

    private File getLogsDir(Context context) {
        return IOUtils.createExternalStorageDir(context, "RobotiumLogs");
    }
}
