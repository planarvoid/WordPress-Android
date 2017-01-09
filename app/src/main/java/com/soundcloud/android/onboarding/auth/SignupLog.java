package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class SignupLog {
    protected static final int THROTTLE_WINDOW = 60 * 60 * 1000;
    protected static final int THROTTLE_AFTER_ATTEMPT = 5;
    private static final String SIGNUP_LOG_NAME = ".dr";

    public static boolean shouldThrottleSignup(Context context) {
        final long[] signupLog = readLog(context);
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

    public static Thread writeNewSignupAsync(final Context context) {
        Thread t = new Thread(() -> {
            writeNewSignup(context, System.currentTimeMillis());
        });
        t.start();
        return t;
    }

    static boolean writeNewSignup(Context context, long timestamp) {
        long[] toWrite, current = readLog(context);
        if (current == null) {
            toWrite = new long[1];
        } else {
            toWrite = new long[current.length + 1];
            System.arraycopy(current, 0, toWrite, 0, current.length);
        }
        toWrite[toWrite.length - 1] = timestamp;
        return writeLog(context, toWrite);
    }

    static boolean writeLog(Context context, long[] toWrite) {
        ObjectOutputStream out = null;
        try {
            final File signupLog = getSignupLog(context);

            if (signupLog == null) return false;

            out = new ObjectOutputStream(new FileOutputStream(signupLog));
            out.writeObject(toWrite);
            return true;
        } catch (IOException e) {
            Log.w(SoundCloudApplication.TAG, "Error writing to sign up log ", e);
            return false;
        } finally {
            if (out != null) {
                IOUtils.close(out);
            }
        }
    }

    @Nullable
    static long[] readLog(Context context) {
        final File signupLog = getSignupLog(context);
        if (signupLog != null && signupLog.exists()) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(signupLog));
                return (long[]) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                Log.e(SoundCloudApplication.TAG, "Error reading sign up log ", e);
            } finally {
                IOUtils.close(in);
            }
        }
        return null;
    }

    private SignupLog() {
    }

    @Nullable
    private static File getSignupLog(Context context) {
        return IOUtils.getExternalStorageDir(context, SIGNUP_LOG_NAME);
    }
}
