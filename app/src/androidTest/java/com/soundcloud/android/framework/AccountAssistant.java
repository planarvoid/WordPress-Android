package com.soundcloud.android.framework;

import static java.lang.String.format;
import static java.util.Locale.getDefault;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.Endpoints;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rx.Subscription;

public final class AccountAssistant {

    private static final long INJECTION_TIMEOUT = 10000;

    private AccountAssistant() {
    }

    private static final String TAG = AccountAssistant.class.getSimpleName();

    private static final Lock lock = new ReentrantLock();
    private static final Condition accountDataCleaned = lock.newCondition();

    protected static Token setToken(Context context, Token token) throws IOException {
        final SoundCloudApplication application = SoundCloudApplication.fromContext(context);
        application.getAccountOperations().updateToken(token);
        return token;
    }

    static boolean addAccountAndEnableSync(Context context, Token token, ApiUser user) {
        final SoundCloudApplication application = SoundCloudApplication.fromContext(context);
        application.getAccountOperations().updateToken(token);
        return application.addUserAccountAndEnableSync(user, token, SignupVia.NONE);
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
        for (Account account : getAccounts(context)) {
            AccountManager.get(context).removeAccount(account, null, null).getResult(3, TimeUnit.SECONDS);
        }

        Account account = getAccount(context);
        if (account == null) {
            return false;
        }

        Log.i(TAG, format(Locale.US, "LoggedInUser: %s", getAccount(context).name));

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

    static Subscription accountDataCleanup(Context context) {
        return SoundCloudApplication.fromContext(context).getEventBus().subscribe(
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
    }

    static void waitForAccountDataCleanup(Subscription subscription) throws Exception {
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

    private static Account[] getAccounts(Context context) {
        AccountManager am = AccountManager.get(context);
        return am.getAccountsByType(context.getString(R.string.account_type));
    }

    static PublicApiUser getLoggedInUser(PublicApi apiWrapper) throws IOException {
        HttpResponse response = apiWrapper.get(Request.to(Endpoints.MY_DETAILS));
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            throw new IOException(format(getDefault(), "%s response status was: %d", Endpoints.MY_DETAILS, status));
        }
        final InputStream content = response.getEntity().getContent();
        return PublicApi.buildObjectMapper().readValue(content, PublicApiUser.class);
    }

    static PublicApi createApiWrapper(Context context) {
        waitForAccountOperationsToBeInjected(context);
        return new PublicApi(context);
    }
}
