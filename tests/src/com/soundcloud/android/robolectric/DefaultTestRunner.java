package com.soundcloud.android.robolectric;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.internal.Implements;
import org.junit.runners.model.InitializationError;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import java.io.File;

public class DefaultTestRunner extends RobolectricTestRunner {
    public DefaultTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File(".")));
    }

    @Override
    protected void bindShadowClasses() {
        super.bindShadowClasses();
        Robolectric.bindShadowClass(TestAccountManager.class);
        Robolectric.bindShadowClass(ShadowLog.class);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Implements(AccountManager.class)
    static class TestAccountManager {
        public static AccountManager get(Context context) {
            AccountManager am = mock(AccountManager.class);
            when(am.getAccounts()).thenReturn(
                    new Account[] {}
            );
            when(am.getAccountsByType(anyString())).thenReturn(
                    new Account[] {}
            );
            return am;
        }
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "UnusedDeclaration", "CallToPrintStackTrace"})
    @Implements(Log.class)
    public static class ShadowLog {
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
    }
}
