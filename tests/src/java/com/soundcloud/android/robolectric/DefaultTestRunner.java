package com.soundcloud.android.robolectric;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.provider.DelegatingContentResolver;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.internal.Implements;
import org.junit.runners.model.InitializationError;

import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

public class DefaultTestRunner extends RobolectricTestRunner {
    public static TestApplication application;

    public DefaultTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File(".")) {
            @Override public String getApplicationName() {
                return TestApplication.class.getSimpleName();
            }
        });
    }

    @Override
    public void beforeTest(Method method) {
        application = (TestApplication) Robolectric.application;
    }

    @Override
    protected void bindShadowClasses() {
        super.bindShadowClasses();
        Robolectric.bindShadowClass(ShadowLog.class);
        Robolectric.bindShadowClass(ShadowHandlerThread.class);
        Robolectric.bindShadowClass(DelegatingContentResolver.class);
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "UnusedDeclaration", "CallToPrintStackTrace"})
    @Implements(Log.class)
    public static class ShadowLog {
        public static boolean isLoggable(String tag, int level) {
            return true;
        }

        public static int v(String tag, String msg) {
            System.out.println("[" + tag + "] " + msg);
            return 0;
        }

        public static int d(String tag, String msg) {
            System.out.println("[" + tag + "] " + msg);
            return 0;
        }

        public static int i(String tag, String msg) {
            System.out.println("[" + tag + "] " + msg);
            return 0;
        }

        public static int e(String tag, String msg, Throwable e) {
            System.out.println("[" + tag + "] " + msg);
            e.printStackTrace();
            return 0;
        }

        public static int w(String tag, String msg, Throwable e) {
            System.out.println("[" + tag + "] " + msg);
            e.printStackTrace();
            return 0;
        }

        public static int w(String tag, Throwable e) {
            System.out.println("[" + tag + "] " + e.getMessage());
            e.printStackTrace();
            return 0;
        }

        public static int w(String tag, String msg) {
            System.out.println("[" + tag + "] " + msg);
            return 0;
        }
    }
}
