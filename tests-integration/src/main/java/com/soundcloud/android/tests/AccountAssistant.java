package com.soundcloud.android.tests;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import rx.Subscription;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Instrumentation;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AccountAssistant {

    private static final long INJECTION_TIMEOUT = 2000;

    private AccountAssistant() {}
    private static final String TAG = AccountAssistant.class.getSimpleName();

    private static final Lock lock = new ReentrantLock();
    private static final Condition accountDataCleaned = lock.newCondition();

    public static Account loginAsDefault(final Instrumentation instrumentation) throws Exception {
        final Context context = instrumentation.getTargetContext();
        return TestUser.defaultUser.logIn(context) ? getAccount(context) : null;
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

    protected static Token getToken(ApiWrapper apiWrapper, String username, String password) throws IOException {
        return apiWrapper.login(username, password, Token.SCOPE_NON_EXPIRING);
    }

    private static Account login(String username, String password, Instrumentation instrumentation) {
        Context context = instrumentation.getTargetContext();
        ApiWrapper apiWrapper = AccountAssistant.createApiWrapper(context);
        try {
            Token token = getToken(apiWrapper, username, password);
            PublicApiUser user = getLoggedInUser(apiWrapper);
            if (waitForInjectionAddAccountAndEnableSync(context, token, user)) {
                return getAccount(context);
            }
        } catch (IOException e) {
            Log.i(TAG, "error logging in", e);
            throw new AssertionError("error logging in: " + e.getMessage());
        }

        return null;
    }

    static boolean waitForInjectionAddAccountAndEnableSync(Context context, Token token, PublicApiUser user) {
        waitForAccountOperationsToBeInjected(context);
        return SoundCloudApplication.fromContext(context).addUserAccountAndEnableSync(user, token, SignupVia.NONE);
    }

    // Dirty workaround :
    //      we wait on the integration tests thread for the application
    //      to perform injection in order to mutate the application.
    //
    // A real user can't face this race condition.
    private static void waitForAccountOperationsToBeInjected(Context context) {
        final SoundCloudApplication application = SoundCloudApplication.fromContext(context);
        final int waitingTimeBetweenEachAttempt = 200;
        final int maxAttempt = (int) INJECTION_TIMEOUT / waitingTimeBetweenEachAttempt;

        for (int attempt = 0; application.getAccountOperations() == null && attempt < maxAttempt; attempt++) {
            try {
                Log.i(TAG, "Login: waiting for the application to be ready #" + attempt);
                Thread.sleep(waitingTimeBetweenEachAttempt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean logOut(Instrumentation instrumentation) throws Exception {
        return logOut(instrumentation.getTargetContext());
    }

    public static boolean logOut(Context context) throws Exception {
        Log.i(TAG, "Logging out");
        Account account = getAccount(context);
        if(account == null){
            return false;
        }

        Log.i(TAG, String.format("LoggedInUser: %s", getAccount(context).name));


        final Subscription subscription = SoundCloudApplication.fromContext(context).getEventBus().subscribe(
                EventQueue.CURRENT_USER_CHANGED, new DefaultSubscriber<CurrentUserChangedEvent>() {

                    @Override
                    public void onNext(CurrentUserChangedEvent event) {
                        lock.lock();
                        try {
                            Log.i(TAG, "User account data cleanup finished");
                            accountDataCleaned.signal();
                        } finally {
                            lock.unlock();
                        }
                    }
                });

        AccountManager.get(context).removeAccount(account, null, null);

        lock.lock();
        // wait for the data cleanup action
        try {
            Log.i(TAG, "Waiting for user account data cleanup...");
            accountDataCleaned.await(5, TimeUnit.SECONDS);
        } finally {
            lock.unlock();
            subscription.unsubscribe();
        }

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

    static PublicApiUser getLoggedInUser(ApiWrapper apiWrapper) throws IOException {
        final InputStream content = apiWrapper.get(Request.to(Endpoints.MY_DETAILS)).getEntity().getContent();
        return PublicApiWrapper.buildObjectMapper().readValue(content, PublicApiUser.class);
    }

    static ApiWrapper createApiWrapper(Context context) {
        final HttpProperties properties = new HttpProperties(context.getResources());
        return new ApiWrapper(properties.getClientId(), properties.getClientSecret(), PublicApiWrapper.ANDROID_REDIRECT_URI, null);
    }
}
