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

        SoundCloudApplication app = (SoundCloudApplication) Instrumentation.newApplication(
                        SoundCloudApplication.class,
                        instrumentation.getTargetContext());
        app.onCreate();

        final Account account = app.getAccount();
        if (account != null && !account.name.equals(username)) {
            Log.d(TAG, "clearing account and logging in again");
            logOut(instrumentation);
            loginAs(instrumentation, username, password);
        } else if (account == null) {
            Log.d(TAG, "logging in");
            Token token = app.login(username, password);
            User user = new FetchUserTask(app, -1).execute(Request.to(Endpoints.MY_DETAILS)).get();
            assertNotNull(user);
            assertTrue(app.addUserAccount(user, token, SignupVia.API));
        } else {
            Log.d(TAG, "already logged in as user "+account);
        }
    }

    public static boolean logOut(final Instrumentation instrumentation) throws Exception {
        AccountManager am = AccountManager.get(instrumentation.getTargetContext());
        Account[] accounts = am.getAccountsByType(instrumentation.getTargetContext().getString(R.string.account_type));
        if (accounts.length > 0) {
            for (Account a : accounts) {
                assertTrue(am.removeAccount(a, null, null).getResult());
            }
            return true;
        } else {
            return false;
        }
    }
}
