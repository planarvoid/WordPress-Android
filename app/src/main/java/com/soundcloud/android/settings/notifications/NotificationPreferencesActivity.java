package com.soundcloud.android.settings.notifications;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.CircularProgressBar;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

public class NotificationPreferencesActivity extends LoggedInActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject NotificationPreferencesOperations operations;

    private Subscription subscription = RxUtils.invalidSubscription();

    public NotificationPreferencesActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (operations.needsSyncOrRefresh()) {
            if (savedInstanceState != null) {
                clearContentFragment();
            }
            refresh();
        } else {
            showPreferences();
        }
    }

    private void showPreferences() {
        setContentFragment(new NotificationPreferencesFragment());
    }

    private void showProgressBar() {
        CircularProgressBar progressBar = (CircularProgressBar) findViewById(R.id.loading);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        findViewById(R.id.loading).setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    private void refresh() {
        subscription = operations.refresh()
                                 .onErrorResumeNext(Observable.empty())
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .subscribe(new PreferencesFetchSubscriber(this));
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_NOTIFICATIONS;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLoadingLayout(this);
    }

    private static class PreferencesFetchSubscriber extends DefaultSubscriber<NotificationPreferences> {

        NotificationPreferencesActivity activity;

        public PreferencesFetchSubscriber(NotificationPreferencesActivity activity) {
            this.activity = activity;
            activity.showProgressBar();
        }

        @Override
        public void onCompleted() {
            if (!activity.isFinishing()) {
                activity.hideProgressBar();
                activity.showPreferences();
            }
        }
    }
}
