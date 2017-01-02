package com.soundcloud.android.more;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.android.main.ScrollContent;

import android.content.res.Resources;
import android.support.v4.widget.NestedScrollView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
public class MoreView implements ScrollContent {

    private final Unbinder unbinder;
    private Listener listener;

    @BindView(R.id.header_layout) View headerLayout;
    @BindView(R.id.image) ImageView profileImageView;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.more_version_text) TextView versionText;
    @BindView(R.id.more_offline_sync_settings_link) View offlineSettingsView;
    @BindView(R.id.more_report_bug) View reportBug;
    @BindView(R.id.scroll_view) NestedScrollView scrollView;
    @BindView(R.id.more_go_indicator) View goIndicator;

    MoreView(View view, final Listener listener) {
        this.listener = listener;
        unbinder = ButterKnife.bind(this, view);

        setAppVersionString(view.getResources());
    }

    @Override
    public void resetScroll() {
        scrollView.smoothScrollTo(0, 0);
    }

    public void showGoIndicator(boolean showGoIndicator) {
        goIndicator.setVisibility(showGoIndicator ? View.VISIBLE : View.GONE);
    }

    private void setAppVersionString(Resources resources) {
        final String appVersionString = resources.getString(R.string.more_app_version, BuildConfig.VERSION_NAME);
        versionText.setText(appVersionString);
    }

    public void unbind() {
        unbinder.unbind();
        this.listener = null;
    }

    public void showOfflineSettings() {
        offlineSettingsView.setVisibility(View.VISIBLE);
    }

    public void hideOfflineSettings() {
        offlineSettingsView.setVisibility(View.GONE);
    }

    public void showReportBug() {
        reportBug.setVisibility(View.VISIBLE);
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

    @OnClick(R.id.more_activity_link)
    void onActivityLinkClicked(View view) {
        if (listener != null) {
            listener.onActivitiesClicked(view);
        }
    }

    @OnClick(R.id.more_record_link)
    void onRecordLinkClicked(View view) {
        if (listener != null) {
            listener.onRecordClicked(view);
        }
    }

    @OnClick(R.id.more_offline_sync_settings_link)
    void onOfflineSyncSettingsClicked(View view) {
        if (listener != null) {
            listener.onOfflineSettingsClicked(view);
        }
    }

    @OnClick(R.id.more_notification_preferences_link)
    void onNotificationSettingsClicked(View view) {
        if (listener != null) {
            listener.onNotificationPreferencesClicked(view);
        }
    }

    @OnClick(R.id.more_basic_settings_link)
    void onBasicSettingsClicked(View view) {
        if (listener != null) {
            listener.onBasicSettingsClicked(view);
        }
    }

    @OnClick(R.id.more_report_bug)
    void onReportBugClicked(View view) {
        if (listener != null) {
            listener.onReportBugClicked(view);
        }
    }

    @OnClick(R.id.more_help_center_link)
    void onHelpCenterClicked(View view) {
        if (listener != null) {
            listener.onHelpCenterClicked(view);
        }
    }

    @OnClick(R.id.more_legal_link)
    void onLegalClicked(View view) {
        if (listener != null) {
            listener.onLegalClicked(view);
        }
    }

    @OnClick(R.id.more_sign_out_link)
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

        void onNotificationPreferencesClicked(View view);

        void onBasicSettingsClicked(View view);

        void onReportBugClicked(View view);

        void onHelpCenterClicked(View view);

        void onLegalClicked(View view);

        void onSignOutClicked(View view);
    }

}
