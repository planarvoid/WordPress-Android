package com.soundcloud.android.framework;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.Endpoints;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AccountAssistant {

    private static final long INJECTION_TIMEOUT = 10000;

    private AccountAssistant() {
    }

    private static final String TAG = AccountAssistant.class.getSimpleName();

    private static final Lock lock = new ReentrantLock();
    private static final Condition accountDataCleaned = lock.newCondition();

    protected static Token getToken(Context context, PublicApi apiWrapper, String username, String password) throws IOException {
        final Token token = apiWrapper.login(username, password);
        final SoundCloudApplication application = SoundCloudApplication.fromContext(context);
        application.getAccountOperations().updateToken(token);
        return token;
    }

    static boolean addAccountAndEnableSync(Context context, Token token, PublicApiUser user) {
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
        if (account == null) {
            return false;
        }

        Log.i(TAG, String.format("LoggedInUser: %s", getAccount(context).name));

        AccountManagerFuture<Boolean> accountManagerFuture =
                AccountManager.get(context).removeAccount(account, null, null);
        return accountManagerFuture.getResult(3, TimeUnit.SECONDS);
    }

    public static boolean logOutWithAccountCleanup(Instrumentation instrumentation) throws Exception {
        Context context = instrumentation.getTargetContext();
        Subscription subscription = AccountAssistant.accountDataCleanup(context);
        boolean result = AccountAssistant.logOut(context);
        AccountAssistant.waitForAccountDataCleanup(subscription);
        return result;
    }

    public static Subscription accountDataCleanup(Context context) {
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
        return subscription;
    }

    public static void waitForAccountDataCleanup(Subscription subscription) throws Exception{
        lock.lock();
        // wait for the data cleanup action
        try {
            Log.i(TAG, "Waiting for user account data cleanup...");
            accountDataCleaned.await(15, TimeUnit.SECONDS);
        } finally {
            lock.unlock();
            subscription.unsubscribe();
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

    static PublicApiUser getLoggedInUser(PublicApi apiWrapper) throws IOException {
        final InputStream content = apiWrapper.get(Request.to(Endpoints.MY_DETAILS)).getEntity().getContent();
        return PublicApi.buildObjectMapper().readValue(content, PublicApiUser.class);
    }

    static PublicApi createApiWrapper(Context context) {
        waitForAccountOperationsToBeInjected(context);
        return new PublicApi(context);
    }
}
