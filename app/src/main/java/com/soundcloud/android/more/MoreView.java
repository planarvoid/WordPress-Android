package com.soundcloud.android.more;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.android.main.MainPagerAdapter;

import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.support.v4.widget.NestedScrollView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
class MoreView implements MainPagerAdapter.ScrollContent {

    private final Unbinder unbinder;
    private Listener listener;

    @BindView(R.id.header_layout) View headerLayout;
    @BindView(R.id.image) ImageView profileImageView;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.more_version_text) TextView versionText;
    @BindView(R.id.more_offline_sync_settings_link) View offlineSettingsView;
    @BindView(R.id.more_report_bug) View reportBug;
    @BindView(R.id.more_upsell_block) View upsell;
    @BindView(R.id.more_upsell) TextView upsellText;
    @BindView(R.id.more_subscription_block) View subscriptionBlock;
    @BindView(R.id.more_subscription_tier) TextView tier;
    @BindView(R.id.more_restore_subscription_block) View restoreBlock;
    @BindView(R.id.more_restore_subscription) TextView restore;
    @BindView(R.id.scroll_view) NestedScrollView scrollView;

    MoreView(View view, final Listener listener) {
        this.listener = listener;
        unbinder = ButterKnife.bind(this, view);
        setAppVersionString(view.getResources());
    }

    @Override
    public void resetScroll() {
        scrollView.smoothScrollTo(0, 0);
    }

    private void setAppVersionString(Resources resources) {
        final String appVersionString = resources.getString(R.string.more_app_version, BuildConfig.VERSION_NAME);
        versionText.setText(appVersionString);
    }

    public void unbind() {
        unbinder.unbind();
        this.listener = null;
    }

    void showOfflineSettings() {
        offlineSettingsView.setVisibility(View.VISIBLE);
    }

    void hideOfflineSettings() {
        offlineSettingsView.setVisibility(View.GONE);
    }

    void showReportBug() {
        reportBug.setVisibility(View.VISIBLE);
    }

    void setUsername(String username) {
        this.username.setText(username);
    }

    void setSubscriptionTier(String tier) {
        this.tier.setText(tier);
        subscriptionBlock.setVisibility(View.VISIBLE);
    }

    void showUpsell(@StringRes int upsellTextId) {
        upsellText.setText(upsellTextId);
        upsell.setVisibility(View.VISIBLE);
    }

    boolean isUpsellVisible() {
        return upsell.getVisibility() == View.VISIBLE;
    }

    void showRestoreSubscription() {
        restoreBlock.setVisibility(View.VISIBLE);
    }

    void setRestoreSubscriptionEnabled(boolean enabled) {
        restore.setEnabled(enabled);
        restore.setClickable(enabled);
    }

    @OnClick(R.id.header_layout)
    void onHeaderLayoutClicked(View view) {
        if (listener != null) {
            listener.onProfileClicked();
        }
    }

    @OnClick(R.id.more_activity_link)
    void onActivityLinkClicked(View view) {
        if (listener != null) {
            listener.onActivitiesClicked();
        }
    }

    @OnClick(R.id.more_record_link)
    void onRecordLinkClicked(View view) {
        if (listener != null) {
            listener.onRecordClicked();
        }
    }

    @OnClick(R.id.more_offline_sync_settings_link)
    void onOfflineSyncSettingsClicked(View view) {
        if (listener != null) {
            listener.onOfflineSettingsClicked();
        }
    }

    @OnClick(R.id.more_upsell)
    void onUpsellClicked(View view) {
        if (listener != null) {
            listener.onUpsellClicked(view);
        }
    }

    @OnClick(R.id.more_restore_subscription)
    void onRestoreSubscriptionClicked(View view){
        if (listener != null) {
            listener.onRestoreSubscriptionClicked(view);
        }
    }

    @OnClick(R.id.more_notification_preferences_link)
    void onNotificationSettingsClicked(View view) {
        if (listener != null) {
            listener.onNotificationPreferencesClicked();
        }
    }

    @OnClick(R.id.more_basic_settings_link)
    void onBasicSettingsClicked(View view) {
        if (listener != null) {
            listener.onBasicSettingsClicked();
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
            listener.onHelpCenterClicked();
        }
    }

    @OnClick(R.id.more_legal_link)
    void onLegalClicked(View view) {
        if (listener != null) {
            listener.onLegalClicked();
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
        void onProfileClicked();

        void onActivitiesClicked();

        void onRecordClicked();

        void onOfflineSettingsClicked();

        void onNotificationPreferencesClicked();

        void onBasicSettingsClicked();

        void onReportBugClicked(View view);

        void onHelpCenterClicked();

        void onLegalClicked();

        void onSignOutClicked(View view);

        void onUpsellClicked(View view);

        void onRestoreSubscriptionClicked(View view);
    }

}
