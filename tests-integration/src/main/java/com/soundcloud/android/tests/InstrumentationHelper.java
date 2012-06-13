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
import android.content.Context;
import android.util.Log;

public final class InstrumentationHelper {
    public static final String USERNAME = "android-testing";
    public static final String PASSWORD = "android-testing";

    private InstrumentationHelper() {}
    private static final String TAG = InstrumentationHelper.class.getSimpleName();

    public static void loginAsDefault(final Instrumentation instrumentation) throws Exception {
        loginAs(instrumentation, USERNAME, PASSWORD);
    }

    public static void loginAs(final Instrumentation instrumentation,
                               final String username,
                               final String password) throws Exception {

        // TODO avoid creating app instances here, conflicts with instrumented app
        SoundCloudApplication app = (SoundCloudApplication) Instrumentation.newApplication(
                SoundCloudApplication.class,
                instrumentation.getTargetContext());
        app.onCreate();

        final Account account = getAccount(instrumentation.getTargetContext());
        if (account != null && !account.name.equals(username)) {
            Log.d(TAG, "clearing account and logging in again");
            if (logOut(instrumentation.getTargetContext())) {
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

    public static boolean logOut(Context context) throws Exception {
        Account account = getAccount(context);
        if (account != null) {
            assertTrue(AccountManager.get(context).removeAccount(account, null, null).getResult());
            return true;
        } else {
            return false;
        }
    }

    public static Account getAccount(Context context) {
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(context.getString(R.string.account_type));
        if (accounts.length == 0) {
            return null;
        } else if (accounts.length == 1) {
            return accounts[0];
        } else {
            throw new AssertionError("More than one account found");
        }
    }
}
