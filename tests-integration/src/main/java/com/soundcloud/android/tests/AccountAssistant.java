package com.soundcloud.android.tests;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static junit.framework.Assert.assertNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.http.PublicApiWrapper;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.tasks.FetchUserTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Instrumentation;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public final class AccountAssistant {
    public static final String USERNAME = "android-testing";
    public static final String PASSWORD = "android-testing";

    private AccountAssistant() {}
    private static final String TAG = AccountAssistant.class.getSimpleName();

    public static Account loginAsDefault(final Instrumentation instrumentation) throws Exception {
        return loginAs(instrumentation, USERNAME, PASSWORD);
    }

    public static Account loginAs(final Instrumentation instrumentation,
                                  final String username,
                                  final String password) throws Exception {

        final Account account = getAccount(instrumentation.getTargetContext());
        if (account != null && account.name.equals(username)) {
            Log.i(TAG, "Already logged in");
            return account;
        } else if (account != null && !account.name.equals(username)) {
            if(!logOut(instrumentation)){
                throw new RuntimeException("Could not log out of SoundCloud Account");
            }
        }
        return login(username, password, instrumentation);
    }

    private static Account login(String username, String password, Instrumentation instrumentation) {
        Log.i(TAG, "Logging in");

        Context context = instrumentation.getTargetContext();
        PublicApiWrapper publicApiWrapper = PublicApiWrapper.getInstance(context);
        Token token;
        User user;
        try {
            token = publicApiWrapper.login(username, password, Token.SCOPE_NON_EXPIRING);
            user = getUser(publicApiWrapper);
        } catch (Exception e) {
            Log.w(AccountAssistant.class.getSimpleName(), e);
            throw new AssertionError("error logging in: "+e.getMessage());
        }
        assertNotNull("could not get test user", user);

        if(SoundCloudApplication.instance.addUserAccountAndEnableSync(user, token, SignupVia.NONE)){
            return getAccount(context);
        };

        return null;
    }

    private static User getUser(PublicApiWrapper publicApiWrapper) {
        User user = null;
        int count = 0;
        int maxTries = 3;
        while(user == null && count < maxTries) {
            try {
                user = new FetchUserTask(publicApiWrapper).execute(Request.to(Endpoints.MY_DETAILS)).get();
            } catch (Exception e) {
                if (++count == maxTries) throw new AssertionError("error logging in: "+e.getMessage());
            }
            if (user == null) {
                sleep(5000);
            }
            count++;
        }
        return user;
    }

    private static void sleep(int miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static boolean logOut(Instrumentation instrumentation) throws Exception {
        return logOut(instrumentation.getTargetContext());
    }

    public static boolean logOut(Context context) throws Exception {
        Log.i(TAG, "Logging out");
        AccountOperations accountOperations = new AccountOperations(context);
        if(!accountOperations.soundCloudAccountExists()){
            return false;
        }

        Log.i(TAG, String.format("LoggedInUser: %s", getAccount(context).name));

        fireAndForget(new AccountOperations(context).removeSoundCloudAccount());
        PublicApiWrapper.getInstance(context).setToken(null);
        return true;
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