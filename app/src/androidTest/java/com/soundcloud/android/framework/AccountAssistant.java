package com.soundcloud.android.framework;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.androidnetworkmanagerclient.NetworkManagerClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Subscription;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AccountAssistant {

    private static final int MAX_RETRIES = 3;

    private AccountAssistant() {
    }

    private static final String TAG = AccountAssistant.class.getSimpleName();

    private static final Lock lock = new ReentrantLock();
    private static final Condition accountDataCleaned = lock.newCondition();

    public static boolean loginWith(Context context, TestUser testUser, NetworkManagerClient networkManagerClient) {
        int tryCount = 0;
        boolean accountAdded = false;
        do {
            try {
                tryCount++;
                PublicApiUser loggedInUser = AccountAssistant.getLoggedInUser(testUser.token.getAccessToken());
                accountAdded = AccountAssistant.addAccountAndEnableSync(context, testUser.token, loggedInUser.toApiMobileUser());
            } catch (IOException e) {
                Log.e(TAG, "Error fetching account data", e);
                cycleWifi(networkManagerClient);
            }
        } while (!accountAdded && tryCount <= MAX_RETRIES);
        return accountAdded;
    }

    private static void cycleWifi(NetworkManagerClient networkManagerClient) {
        networkManagerClient.switchWifiOff();
        networkManagerClient.switchWifiOn();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static boolean addAccountAndEnableSync(Context context, Token token, ApiUser user) {
        final SoundCloudApplication application = SoundCloudApplication.fromContext(context);
        application.getAccountOperations().updateToken(token);
        return application.addUserAccountAndEnableSync(user, token, SignupVia.NONE);
    }

    public static boolean logOut(Instrumentation instrumentation) throws Exception {
        return logOut(instrumentation.getTargetContext());
    }

    private static boolean logOut(Context context) throws Exception {
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

    private static Subscription accountDataCleanup(Context context) {
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

    private static void waitForAccountDataCleanup(Subscription subscription) throws Exception {
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
        final Account[] accounts = getAccounts(context);
        if (accounts.length == 0) {
            return null;
        } else if (accounts.length == 1) {
            return accounts[0];
        } else {
            throw new AssertionError("More than one account found");
        }
    }

    @SuppressWarnings("MissingPermission") //safe since we are manipulating our own SC account.
    private static Account[] getAccounts(Context context) {
        return AccountManager.get(context).getAccountsByType(context.getString(R.string.account_type));
    }

    static PublicApiUser getLoggedInUser(String accessToken) throws IOException {
        OkHttpClient okHttpClient =  new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("http://api.soundcloud.com/me?"+ ApiRequest.Param.OAUTH_TOKEN + "=" + accessToken)
                .build();

        Response response = okHttpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        ObjectMapper objectMapper = JacksonJsonTransformer.buildObjectMapper();
        return objectMapper.readValue(response.body().byteStream(), PublicApiUser.class);
    }
}
