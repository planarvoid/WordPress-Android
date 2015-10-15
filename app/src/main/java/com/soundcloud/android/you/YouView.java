package com.soundcloud.android.you;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.android.main.ScrollContent;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
public class YouView implements ScrollContent {

    private Listener listener;

    @Bind(R.id.header_layout) View headerLayout;
    @Bind(R.id.image) ImageView profileImageView;
    @Bind(R.id.username) TextView username;
    @Bind(R.id.you_version_text) TextView versionText;
    @Bind(R.id.you_offline_sync_settings_link) View offlineSettingsView;
    @Bind(R.id.scroll_view) View scrollView;

    YouView(View view, final Listener listener) {
        this.listener = listener;
        ButterKnife.bind(this, view);

        setAppVersionString(view.getResources());
    }

    @Override
    public void resetScroll() {
        scrollView.scrollTo(0, 0);
    }

    private void setAppVersionString(Resources resources) {
        final String appVersionString = resources.getString(R.string.you_app_version, BuildConfig.VERSION_NAME);
        versionText.setText(appVersionString);
    }

    public void unbind() {
        ButterKnife.unbind(this);
        this.listener = null;
    }

    public void showOfflineSettings() {
        offlineSettingsView.setVisibility(View.VISIBLE);
    }

    public void hideOfflineSettings() {
        offlineSettingsView.setVisibility(View.GONE);
    }

    void setUsername(String username) {
        this.username.setText(username);
    }

    @OnClick(R.id.header_layout)
    void onHeaderLayoutClicked(View view) {
        if (listener != null) {
            listener.onProfileClicked(view);
        }
    }

    @OnClick(R.id.you_activity_link)
    void onActivityLinkClicked(View view) {
        if (listener != null) {
            listener.onActivitiesClicked(view);
        }
    }

    @OnClick(R.id.you_record_link)
    void onRecordLinkClicked(View view) {
        if (listener != null) {
            listener.onRecordClicked(view);
        }
    }

    @OnClick({R.id.you_offline_sync_settings_link})
    void onOfflineSyncSettingsClicked(View view) {
        if (listener != null) {
            listener.onOfflineSettingsClicked(view);
        }
    }

    @OnClick({R.id.you_notification_settings_link})
    void onNotificationSettingsClicked(View view) {
        if (listener != null) {
            listener.onNotificationSettingsClicked(view);
        }
    }

    @OnClick({R.id.you_basic_settings_link})
    void onBasicSettingsClicked(View view) {
        if (listener != null) {
            listener.onBasicSettingsClicked(view);
        }
    }

    @OnClick({R.id.you_help_center_link})
    void onHelpCenterClicked(View view) {
        if (listener != null) {
            listener.onHelpCenterClicked(view);
        }
    }

    @OnClick({R.id.you_legal_link})
    void onLegalClicked(View view) {
        if (listener != null) {
            listener.onLegalClicked(view);
        }
    }

    @OnClick({R.id.you_sign_out_link})
    void onSignOutClicked(View view) {
        if (listener != null) {
            listener.onSignOutClicked(view);
        }
    }

    ImageView getProfileImageView() {
        return profileImageView;
    }

    interface Listener {
        void onProfileClicked(View view);

        void onActivitiesClicked(View view);

        void onRecordClicked(View view);

        void onOfflineSettingsClicked(View view);

        void onNotificationSettingsClicked(View view);

        void onBasicSettingsClicked(View view);

        void onHelpCenterClicked(View view);

        void onLegalClicked(View view);

        void onSignOutClicked(View view);
    }

}
