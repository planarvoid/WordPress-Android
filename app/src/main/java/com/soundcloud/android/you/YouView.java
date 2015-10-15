package com.soundcloud.android.you;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
public class YouView {

    private final Navigator navigator;

    @Bind(R.id.header_layout) View headerLayout;
    @Bind(R.id.image) ImageView profileImageView;
    @Bind(R.id.username) TextView username;

    YouView(@Provided Navigator navigator, View view) {
        this.navigator = navigator;
        ButterKnife.bind(this, view);
    }

    public void unbind(){
        ButterKnife.unbind(this);
    }

    void setUrn(final Urn urn) {
        headerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigator.openProfile(view.getContext(), urn, Screen.YOU);
            }
        });
    }

    void setUsername(String username) {
        this.username.setText(username);
    }

    @OnClick(R.id.you_activity_link)
    void onActivityLinkClicked(View view) {
        navigator.openActivities(view.getContext());
    }

    @OnClick(R.id.you_record_link)
    void onRecordLinkClicked(View view) {
        navigator.openRecord(view.getContext(), Screen.YOU);
    }

    @OnClick({ R.id.you_basic_settings_link, R.id.you_help_center_link, R.id.you_offline_sync_settings_link,
    R.id.you_notification_settings_link, R.id.you_legal_link})
    void onSettingsClicked(View view) {
        navigator.openSettings(view.getContext());
    }

    ImageView getProfileImageView() {
        return profileImageView;
    }

}
