package com.soundcloud.android.collection;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;

class OfflineOnboardingView {

    @Bind(R.id.auto_offline) View autoOffline;
    @Bind(R.id.selective_offline) View selectiveOffline;

    interface Listener {
        void selectiveSync();
        void autoSync();
    }

    @Inject
    OfflineOnboardingView() {}

    public void setupContentView(AppCompatActivity activity, Listener listener) {
        ButterKnife.bind(this, activity);
        setListener(listener);
    }

    private void setListener(final Listener listener) {
        autoOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.autoSync();
            }
        });
        selectiveOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.selectiveSync();
            }
        });
    }

}
