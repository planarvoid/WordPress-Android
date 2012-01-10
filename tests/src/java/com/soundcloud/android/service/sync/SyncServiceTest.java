package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.util.LinkedHashMap;

@RunWith(DefaultTestRunner.class)
public class SyncServiceTest {

    @Test
    public void shouldSync() throws Exception {
        ApiSyncService svc = new ApiSyncService();
        Intent intent = new Intent(ApiSyncService.SYNC_ACTION, Content.ME_TRACKS.uri);

        final LinkedHashMap<Integer,Bundle> received = new LinkedHashMap<Integer, Bundle>();
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(new Handler(Looper.myLooper())) {
            @Override protected void onReceiveResult(int resultCode, Bundle resultData) {
                received.put(resultCode, resultData);
            }
        });
        svc.onHandleIntent(intent);

        expect(received.size()).toBe(2);
        expect(received.containsKey(ApiSyncService.STATUS_RUNNING)).toBeTrue();
        expect(received.containsKey(ApiSyncService.STATUS_SYNC_ERROR)).toBeFalse();
    }
}
