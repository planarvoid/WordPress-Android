package com.soundcloud.android.you;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.ScrollContent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

public class YouPresenter extends DefaultSupportFragmentLightCycle<YouFragment>
        implements YouView.Listener, ScrollContent {

    private final YouViewFactory youViewFactory;
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

    private Optional<YouView> youViewOpt = Optional.absent();
    private Optional<You> youOpt = Optional.absent();

    @Inject
    public YouPresenter(YouViewFactory youViewFactory,
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
                        OfflineSettingsStorage settingsStorage) {
        this.youViewFactory = youViewFactory;
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
    }

    @Override
    public void onCreate(YouFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userRepository.userInfo(accountOperations.getLoggedInUserUrn())
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new YouSubscriber());
    }

    @Override
    public void onViewCreated(YouFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        final YouView youView = youViewFactory.create(view, this);
        youViewOpt = Optional.of(youView);

        setupOfflineSync(youView);
        setupFeedback(youView);
        bindUserIfPresent();
    }

    @Override
    public void resetScroll() {
        if (youViewOpt.isPresent()) {
            youViewOpt.get().resetScroll();
        }
    }

    private void setupOfflineSync(YouView youView) {
        if (featureOperations.isOfflineContentEnabled() || offlineContentOperations.hasOfflineContent()) {
            youView.showOfflineSettings();
        } else if (featureOperations.upsellHighTier()) {
            youView.showOfflineSettings();
            eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forSettingsImpression());
        } else {
            youView.hideOfflineSettings();
        }
    }

    private void setupFeedback(YouView youView) {
        if (appProperties.shouldAllowFeedback()) {
            youView.showReportBug();
        }
    }

    @Override
    public void onDestroyView(YouFragment fragment) {
        if (youViewOpt.isPresent()) {
            youViewOpt.get().unbind();
        }
        super.onDestroyView(fragment);
    }

    private void bindUserIfPresent() {
        if (youViewOpt.isPresent() && youOpt.isPresent()) {
            final YouView headerView = youViewOpt.get();
            final You you = youOpt.get();
            bindUser(headerView, you);
        }
    }

    private void bindUser(YouView headerView, You you) {
        headerView.setUsername(you.getUsername());
        imageOperations.displayCircularWithPlaceholder(you,
                                                       ApiImageSize.getFullImageSize(resources),
                                                       headerView.getProfileImageView());
    }

    private class YouSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet user) {
            youOpt = Optional.of(new You(user));
            bindUserIfPresent();
        }

        @Override
        public void onError(Throwable e) {
            youOpt = Optional.absent();
        }
    }

    @Override
    public void onProfileClicked(View view) {
        navigator.openProfile(view.getContext(), accountOperations.getLoggedInUserUrn());
    }

    @Override
    public void onExploreClicked(View view) {
        navigator.openExplore(view.getContext(), Screen.MORE);
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
        if (featureOperations.upsellHighTier()) {
            eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forSettingsClick());
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
                .setPositiveButton(R.string.ok_got_it, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogoutActivity.start(activityContext);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDefaultSignOutPrompt(final Context activityContext) {
        final View view = new CustomFontViewBuilder(activityContext)
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_description).get();

        new AlertDialog.Builder(activityContext)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogoutActivity.start(activityContext);
                    }
                })
                .show();
    }

}
