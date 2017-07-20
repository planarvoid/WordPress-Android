package com.soundcloud.android.more;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.MainPagerAdapter;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

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
    private final NavigationExecutor navigationExecutor;
    private final Navigator navigator;
    private final BugReporter bugReporter;
    private final ApplicationProperties appProperties;
    private final OfflineSettingsStorage settingsStorage;
    private final ConfigurationOperations configurationOperations;
    private final FeedbackController feedbackController;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final CompositeDisposable disposables = new CompositeDisposable();

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
                     NavigationExecutor navigationExecutor,
                     Navigator navigator,
                     BugReporter bugReporter,
                     ApplicationProperties appProperties,
                     OfflineSettingsStorage settingsStorage,
                     ConfigurationOperations configurationOperations,
                     FeedbackController feedbackController,
                     PerformanceMetricsEngine performanceMetricsEngine) {
        this.moreViewFactory = moreViewFactory;
        this.userRepository = userRepository;
        this.accountOperations = accountOperations;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.navigationExecutor = navigationExecutor;
        this.navigator = navigator;
        this.bugReporter = bugReporter;
        this.appProperties = appProperties;
        this.settingsStorage = settingsStorage;
        this.configurationOperations = configurationOperations;
        this.feedbackController = feedbackController;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void onCreate(MoreFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        disposables.add(userRepository.userInfo(accountOperations.getLoggedInUserUrn())
                                      .observeOn(AndroidSchedulers.mainThread())
                                      .subscribeWith(new MoreObserver()));
    }

    @Override
    public void onViewCreated(MoreFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        final MoreView moreView = moreViewFactory.create(view, this);
        moreViewOpt = Optional.of(moreView);

        if (shouldShowGoItems()) {
            setupTier(moreView);
            setupUpsell(moreView);
            setupRestoreSubscription(moreView);
        }
        setupOfflineSyncSettings(moreView);
        setupFeedback(moreView);
        bindUserIfPresent();
    }

    private boolean shouldShowGoItems() {
        return featureOperations.getCurrentPlan().isGoPlan() || featureOperations.upsellHighTier();
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
        moreView.setSubscriptionTier(resources.getString(featureOperations.getCurrentPlan().tierName));
    }

    private void setupUpsell(MoreView moreView) {
        if (featureOperations.upsellHighTier()) {
            moreView.showUpsell(featureOperations.getCurrentPlan() == Plan.MID_TIER
                                ? R.string.more_upsell_mt
                                : R.string.more_upsell);
        }
    }

    private void setupRestoreSubscription(MoreView moreView) {
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
        disposables.clear();
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(MoreFragment fragment) {
        disposables.clear();
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

    private class MoreObserver extends DefaultMaybeObserver<User> {
        @Override
        public void onSuccess(User user) {
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
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forProfile(accountOperations.getLoggedInUserUrn()));
    }

    @Override
    public void onActivitiesClicked(View view) {
        performanceMetricsEngine.startMeasuring(MetricType.ACTIVITIES_LOAD);
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forActivities());
    }

    @Override
    public void onRecordClicked(View view) {
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forRecord(Optional.absent(), Optional.of(Screen.MORE)));
    }

    @Override
    public void onOfflineSettingsClicked(View view) {
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forOfflineSettings(true));
    }

    @Override
    public void onNotificationPreferencesClicked(View view) {
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forNotificationPreferences());
    }

    @Override
    public void onBasicSettingsClicked(View view) {
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forBasicSettings());
    }

    @Override
    public void onReportBugClicked(View view) {
        bugReporter.showGeneralFeedbackDialog(view.getContext());
    }

    @Override
    public void onHelpCenterClicked(View view) {
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forHelpCenter());
    }

    @Override
    public void onLegalClicked(View view) {
        navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forLegal());
    }

    @Override
    public void onSignOutClicked(final View view) {
        showSignOutPrompt(view.getContext());
    }

    @Override
    public void onUpsellClicked(View view) {
        navigationExecutor.openUpgrade(view.getContext(), UpsellContext.DEFAULT);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeFromSettingsClick());
    }

    @Override
    public void onRestoreSubscriptionClicked(View view) {
        disposables.add(RxJava.toV2Completable(configurationOperations.update())
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribeWith(new ConfigurationObserver()));
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

    private class ConfigurationObserver extends DefaultDisposableCompletableObserver {

        @Override
        public void onComplete() {
            if (!featureOperations.getCurrentPlan().isGoPlan()) {
                feedbackController.showFeedback(Feedback.create(R.string.more_subscription_check_not_subscribed));
            }
            setRestoreSubscriptionEnabled(true);
            super.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            feedbackController.showFeedback(Feedback.create(R.string.more_subscription_check_error));
            setRestoreSubscriptionEnabled(true);
        }
    }
}
