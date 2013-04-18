package com.soundcloud.android.activity.auth;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SignupLog {
    public static final int THROTTLE_WINDOW = 60 * 60 * 1000;
    public static final int THROTTLE_AFTER_ATTEMPT = 5;
    private static final File SIGNUP_LOG = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".dr");

    static boolean shouldThrottleSignup() {
        final long[] signupLog = readLog();
        if (signupLog == null) {
            return false;
        } else {
            int i = signupLog.length - 1;
            while (i >= 0 &&
                    System.currentTimeMillis() - signupLog[i] < THROTTLE_WINDOW &&
                    signupLog.length - i <= THROTTLE_AFTER_ATTEMPT) {
                i--;
            }
            return signupLog.length - i > THROTTLE_AFTER_ATTEMPT;
        }
    }

    static void writeNewSignup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeNewSignup(System.currentTimeMillis());
            }
        });
    }

    static boolean writeNewSignup(long timestamp) {
        long[] toWrite, current = readLog();
        if (current == null) {
            toWrite = new long[1];
        } else {
            toWrite = new long[current.length + 1];
            System.arraycopy(current, 0, toWrite, 0, current.length);
        }
        toWrite[toWrite.length - 1] = timestamp;
        return writeLog(toWrite);
    }

    static boolean writeLog(long[] toWrite) {
        try {
            IOUtils.mkdirs(SIGNUP_LOG.getParentFile());

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SIGNUP_LOG));
            out.writeObject(toWrite);
            out.close();
            return true;
        } catch (IOException e) {
            Log.w(SoundCloudApplication.TAG, "Error writing to sign up log ", e);
            return false;
        }
    }

    @Nullable
    static long[] readLog() {

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(SIGNUP_LOG));
            return (long[]) in.readObject();
        } catch (IOException e) {
            Log.e(SoundCloudApplication.TAG, "Error reading sign up log ", e);
        } catch (ClassNotFoundException e) {
            Log.e(SoundCloudApplication.TAG, "Error reading sign up log ", e);
        }
        return null;
    }
}
