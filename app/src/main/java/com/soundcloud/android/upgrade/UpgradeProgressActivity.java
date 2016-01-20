package com.soundcloud.android.upgrade;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class UpgradeProgressActivity extends AppCompatActivity {

    static final String TAG = "UpgradeProgress";

    @Inject Navigator navigator;
    @Inject UpgradeProgressOperations upgradeProgressOperations;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_progress);

        SoundCloudApplication.getObjectGraph().inject(this);

        subscription = upgradeProgressOperations.awaitAccountUpgrade()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultSubscriber<Object>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "All done! Launching onboarding");
                        navigator.openOfflineOnboarding(UpgradeProgressActivity.this);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }
}
