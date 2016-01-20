package com.soundcloud.android.upgrade;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.policies.DailyUpdateService;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class UpgradeProgressActivity extends AppCompatActivity {

    static final String TAG = "UpgradeProgress";

    @Inject EventBus eventBus;
    @Inject Navigator navigator;

    private Subscription eventSubscription = RxUtils.invalidSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_progress);

        SoundCloudApplication.getObjectGraph().inject(this);

        eventSubscription = eventBus.subscribe(EventQueue.POLICY_UPDATES, new PolicyUpdateEventSubscriber());

        Log.d(TAG, "Starting policy update");
        DailyUpdateService.start(this);
    }

    @Override
    protected void onDestroy() {
        eventSubscription.unsubscribe();
        super.onDestroy();
    }

    private class PolicyUpdateEventSubscriber extends DefaultSubscriber<PolicyUpdateEvent> {
        @Override
        public void onNext(PolicyUpdateEvent event) {
            Log.d(TAG, "All done! Launching onboarding");
            navigator.openOfflineOnboarding(UpgradeProgressActivity.this);
        }
    }
}
