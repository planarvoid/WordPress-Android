package com.soundcloud.android.service.sync;

import static com.soundcloud.android.tests.IntegrationTestHelper.initAsyncTask;
import static com.soundcloud.android.tests.IntegrationTestHelper.loginAsDefault;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SyncAdapterServiceTest extends InstrumentationTestCase {
    private static final String TAG = SyncAdapterServiceTest.class.getSimpleName();

    public void testStartSync() throws Throwable {
        initAsyncTask(getInstrumentation());

        Context context = getInstrumentation().getTargetContext();

        // switch on notifications + no wifi requirement to sync
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(Consts.PrefKeys.NOTIFICATIONS_WIFI_ONLY, false)
                .putBoolean(SyncConfig.PREF_SYNC_WIFI_ONLY, false)
                .commit();

        // delete previous sync state
        context.getContentResolver().delete(Content.COLLECTIONS.uri, null, null);
        ContentStats.clear(context);

        final Account test = loginAsDefault(getInstrumentation());
        final CountDownLatch latch = new CountDownLatch(1);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                latch.countDown();
            }
        }, new IntentFilter(SyncAdapterService.SYNC_FINISHED));

        final long start = System.currentTimeMillis();

        requestSync(test);

        assertTrue("timeout waiting for sync", latch.await(120, TimeUnit.SECONDS));
        Log.d(TAG, "sync finished in " + (System.currentTimeMillis() - start) + " ms");

        // wait for sync thread to exit and possibly crash
        Thread.sleep(1000);
    }

    private static void requestSync(Account test) {
        final Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(test, ScContentProvider.AUTHORITY, extras);
    }
}
