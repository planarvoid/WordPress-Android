package com.soundcloud.android.tests;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Instrumentation;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

public final class IntegrationTestHelper {
    public static final String USERNAME = "android-testing";
    public static final String PASSWORD = "android-testing";

    private IntegrationTestHelper() {}
    private static final String TAG = IntegrationTestHelper.class.getSimpleName();

    public static Account loginAsDefault(final Instrumentation instrumentation) throws Exception {
        return loginAs(instrumentation, USERNAME, PASSWORD);
    }

    public static Account loginAs(final Instrumentation instrumentation,
                               final String username,
                               final String password) throws Exception {


        final Account account = getAccount(instrumentation.getTargetContext());
        if (account != null && !account.name.equals(username)) {
            Log.d(TAG, "clearing account and logging in again");
            if (logOut(instrumentation.getTargetContext())) {
                return loginAs(instrumentation, username, password);
            } else {
                throw new RuntimeException("Could not log out");
            }
        } else if (account == null) {
            Log.d(TAG, "logging in");
            Context context = instrumentation.getTargetContext();
            AndroidCloudAPI.Wrapper wrapper = AndroidCloudAPI.Wrapper.create(context, null);
            Token token;
            try {
                token = wrapper.login(username, password, Token.SCOPE_NON_EXPIRING);
            } catch (IOException e) {
                Log.w(IntegrationTestHelper.class.getSimpleName(), e);
                throw new AssertionError("error logging in: "+e.getMessage());
            }
            User user = new FetchUserTask(wrapper).execute(Request.to(Endpoints.MY_DETAILS)).get();
            assertNotNull("could not get test user", user);
            assertNotNull("addAccount failed", SoundCloudApplication.addAccount(context, user, token, SignupVia.UNKNOWN));
            return account;
        } else {
            Log.d(TAG, "already logged in as user "+account);
            return account;
        }
    }

    public static boolean logOut(Instrumentation instrumentation) throws Exception {
        return logOut(instrumentation.getTargetContext());
    }

    public static boolean logOut(Context context) throws Exception {
        Account account = getAccount(context);
        if (account != null) {
            assertTrue(AccountManager.get(context).removeAccount(account, null, null).getResult());
            SoundCloudApplication app = (SoundCloudApplication) context.getApplicationContext();
            app.onAccountRemoved(account);
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

    public static void clearDb(Instrumentation instrumentation) {
        Log.d(TAG, "clearing out database");
        final Context context = instrumentation.getTargetContext();
        // clear out db

        DBHelper helper = new DBHelper(context);
        final SQLiteDatabase db = helper.getWritableDatabase();
        helper.onRecreateDb(db);
        db.close();
    }

    /**
     * Need to initialize AsyncTask on UI thread, to have internal handler
     * initialized correctly. Calls this at the beginning of your test.
     * Android testing is fscked up.
     */
    public static void initAsyncTask(Instrumentation instrumentation) {
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Void>()  {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        return null;
                    }
                };
            }
        });
    }
}
