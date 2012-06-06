package com.soundcloud.android.tests;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Instrumentation;
import android.util.Log;

public final class InstrumentationHelper {
    public static final String USERNAME = "android-testing";
    public static final String PASSWORD = "android";

    private InstrumentationHelper() {}
    private static final String TAG = InstrumentationHelper.class.getSimpleName();


    public static void loginAsDefault(final Instrumentation instrumentation) throws Exception {
        loginAs(instrumentation, USERNAME, PASSWORD);
    }

    public static void loginAs(final Instrumentation instrumentation,
                               final String username,
                               final String password) throws Exception {

        SoundCloudApplication app = getAppFromInstrumentation(instrumentation);

        final Account account = app.getAccount();
        if (account != null && !account.name.equals(username)) {
            Log.d(TAG, "clearing account and logging in again");
            if (logOut(instrumentation)) {
                loginAs(instrumentation, username, password);
            } else {
                throw new RuntimeException("Could not log out");
            }
        } else if (account == null) {
            Log.d(TAG, "logging in");
            Token token = app.login(username, password, Token.SCOPE_NON_EXPIRING);
            User user = new FetchUserTask(app).execute(Request.to(Endpoints.MY_DETAILS)).get();
            assertNotNull(user);
            assertTrue(app.addUserAccount(user, token, SignupVia.API));
        } else {
            Log.d(TAG, "already logged in as user "+account);
        }
    }

    public static boolean logOut(final Instrumentation instrumentation) throws Exception {
        SoundCloudApplication app = getAppFromInstrumentation(instrumentation);

        AccountManager am = AccountManager.get(instrumentation.getTargetContext());
        Account[] accounts = am.getAccountsByType(instrumentation.getTargetContext().getString(R.string.account_type));
        if (accounts.length > 0) {
            for (Account a : accounts) {
                assertTrue(am.removeAccount(a, null, null).getResult());
                app.onAccountRemoved(a);
            }
            return true;
        } else {
            return false;
        }
    }

    public static SoundCloudApplication getAppFromInstrumentation(Instrumentation instrumentation) throws Exception {
        SoundCloudApplication app = (SoundCloudApplication) Instrumentation.newApplication(
                SoundCloudApplication.class,
                instrumentation.getTargetContext());
        app.onCreate();
        return app;
    }
}
