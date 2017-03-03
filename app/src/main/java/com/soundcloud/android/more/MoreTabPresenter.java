package com.soundcloud.android.more;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.MainPagerAdapter;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

public class MoreTabPresenter extends DefaultSupportFragmentLightCycle<MoreFragment>
        implements MoreView.Listener, MainPagerAdapter.ScrollContent, MainPagerAdapter.FocusListener {

    private final MoreViewFactory moreViewFactory;
    private final UserRepository userRepository;
    private final AccountOperations accountOperations;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final EventBus eventBus;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final Navigator navigator;
    private final BugReporter bugReporter;
    private final ApplicationProperties appProperties;
    private final OfflineSettingsStorage settingsStorage;
    private final ConfigurationOperations configurationOperations;
    private final FeedbackController feedbackController;

    private Subscription userSubscription = RxUtils.invalidSubscription();
    private Subscription configSubscription = RxUtils.invalidSubscription();

    private Optional<MoreView> moreViewOpt = Optional.absent();
    private Optional<More> moreOpt = Optional.absent();

    @Inject
    MoreTabPresenter(MoreViewFactory moreViewFactory,
                     UserRepository userRepository,
                     AccountOperations accountOperations,
                     ImageOperations imageOperations,
                     Resources resources,
                     EventBus eventBus,
                     FeatureOperations featureOperations,
                     OfflineContentOperations offlineContentOperations,
                     Navigator navigator,
                     BugReporter bugReporter,
                     ApplicationProperties appProperties,
                     OfflineSettingsStorage settingsStorage,
                     ConfigurationOperations configurationOperations,
                     FeedbackController feedbackController) {
        this.moreViewFactory = moreViewFactory;
        this.userRepository = userRepository;
        this.accountOperations = accountOperations;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.navigator = navigator;
        this.bugReporter = bugReporter;
        this.appProperties = appProperties;
        this.settingsStorage = settingsStorage;
        this.configurationOperations = configurationOperations;
        this.feedbackController = feedbackController;
    }

    @Override
    public void onCreate(MoreFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userSubscription = userRepository.userInfo(accountOperations.getLoggedInUserUrn())
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new MoreSubscriber());
    }

    @Override
    public void onViewCreated(MoreFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        final MoreView moreView = moreViewFactory.create(view, this);
        moreViewOpt = Optional.of(moreView);

        if (featureOperations.getCurrentPlan().isGoPlan() || featureOperations.upsellHighTier()) {
            setupTier(moreView);
            setupUpsell(moreView);
        }
        setupOfflineSyncSettings(moreView);
        setupFeedback(moreView);
        bindUserIfPresent();
    }

    @Override
    public void resetScroll() {
        if (moreViewOpt.isPresent()) {
            moreViewOpt.get().resetScroll();
        }
    }

    @Override
    public void onFocusChange(boolean hasFocus) {
        if (hasFocus && moreViewOpt.isPresent() && moreViewOpt.get().isUpsellVisible()) {
            eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeFromSettingsImpression());
        }
    }

    private void setupTier(MoreView moreView) {
        String tierName = resources.getString(featureOperations.getCurrentPlan().tierName);
        moreView.setSubscriptionTier(tierName);
    }

    private void setupUpsell(MoreView moreView) {
        if (featureOperations.upsellHighTier()) {
            moreView.showHighTierUpsell();
        }
        if (!featureOperations.getCurrentPlan().isGoPlan()) {
            moreView.showRestoreSubscription();
        }
    }

    private void setupOfflineSyncSettings(MoreView moreView) {
        if (featureOperations.isOfflineContentEnabled()) {
            moreView.showOfflineSettings();
        } else {
            moreView.hideOfflineSettings();
        }
    }

    private void setupFeedback(MoreView moreView) {
        if (appProperties.shouldAllowFeedback()) {
            moreView.showReportBug();
        }
    }

    @Override
    public void onDestroyView(MoreFragment fragment) {
        if (moreViewOpt.isPresent()) {
            moreViewOpt.get().unbind();
            moreViewOpt = Optional.absent();
        }
        configSubscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(MoreFragment fragment) {
        userSubscription.unsubscribe();
        super.onDestroy(fragment);
    }

    private void bindUserIfPresent() {
        if (moreViewOpt.isPresent() && moreOpt.isPresent()) {
            final MoreView headerView = moreViewOpt.get();
            bindUser(headerView, moreOpt.get());
        }
    }

    private void bindUser(MoreView headerView, More more) {
        headerView.setUsername(more.getUsername());
        imageOperations.displayCircularWithPlaceholder(more,
                                                       ApiImageSize.getFullImageSize(resources),
                                                       headerView.getProfileImageView());
    }

    private class MoreSubscriber extends DefaultSubscriber<User> {
        @Override
        public void onNext(User user) {
            moreOpt = Optional.of(new More(user));
            bindUserIfPresent();
        }

        @Override
        public void onError(Throwable e) {
            moreOpt = Optional.absent();
        }
    }

    @Override
    public void onProfileClicked(View view) {
        navigator.legacyOpenProfile(view.getContext(), accountOperations.getLoggedInUserUrn());
    }

    @Override
    public void onActivitiesClicked(View view) {
        navigator.openActivities(view.getContext());
    }

    @Override
    public void onRecordClicked(View view) {
        navigator.openRecord(view.getContext(), Screen.MORE);
    }

    @Override
    public void onOfflineSettingsClicked(View view) {
        if (showOfflineSettingsOnboarding()) {
            navigator.openOfflineSettingsOnboarding(view.getContext());
        } else {
            navigator.openOfflineSettings(view.getContext());
        }
    }

    private boolean showOfflineSettingsOnboarding() {
        return featureOperations.isOfflineContentEnabled()
                && !settingsStorage.hasSeenOfflineSettingsOnboarding();
    }

    @Override
    public void onNotificationPreferencesClicked(View view) {
        navigator.openNotificationPreferences(view.getContext());
    }

    @Override
    public void onBasicSettingsClicked(View view) {
        navigator.openBasicSettings(view.getContext());
    }

    @Override
    public void onReportBugClicked(View view) {
        bugReporter.showGeneralFeedbackDialog(view.getContext());
    }

    @Override
    public void onHelpCenterClicked(View view) {
        navigator.openHelpCenter(view.getContext());
    }

    @Override
    public void onLegalClicked(View view) {
        navigator.openLegal(view.getContext());
    }

    @Override
    public void onSignOutClicked(final View view) {
        showSignOutPrompt(view.getContext());
    }

    @Override
    public void onUpsellClicked(View view) {
        navigator.openUpgrade(view.getContext());
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeFromSettingsClick());
    }

    @Override
    public void onRestoreSubscriptionClicked(View view) {
        configSubscription = configurationOperations.update()
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(new ConfigurationSubscriber());
        setRestoreSubscriptionEnabled(false);
    }

    private void setRestoreSubscriptionEnabled(boolean enabled) {
        if (moreViewOpt.isPresent()) {
            moreViewOpt.get().setRestoreSubscriptionEnabled(enabled);
        }
    }

    private void showSignOutPrompt(final Context activityContext) {
        if (offlineContentOperations.hasOfflineContent()) {
            showOfflineContentSignOutPrompt(activityContext);
        } else {
            showDefaultSignOutPrompt(activityContext);
        }
    }

    private void showOfflineContentSignOutPrompt(final Context activityContext) {
        final View view = new CustomFontViewBuilder(activityContext)
                .setTitle(R.string.sign_out_title_offline)
                .setMessage(R.string.sign_out_description_offline).get();

        new AlertDialog.Builder(activityContext)
                .setView(view)
                .setPositiveButton(R.string.ok_got_it, (dialog, which) -> LogoutActivity.start(activityContext))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDefaultSignOutPrompt(final Context activityContext) {
        final View view = new CustomFontViewBuilder(activityContext)
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_description).get();

        new AlertDialog.Builder(activityContext)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> LogoutActivity.start(activityContext))
                .show();
    }

    private class ConfigurationSubscriber extends DefaultSubscriber<Configuration> {

        @Override
        public void onCompleted() {
            if(!featureOperations.getCurrentPlan().isGoPlan()) {
                feedbackController.showFeedback(Feedback.create(R.string.more_subscription_check_not_subscribed));
            }
            setRestoreSubscriptionEnabled(true);
        }

        @Override
        public void onError(Throwable e) {
            feedbackController.showFeedback(Feedback.create(R.string.more_subscription_check_error));
            setRestoreSubscriptionEnabled(true);
        }
    }
}
