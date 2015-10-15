package com.soundcloud.android.you;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScrollContent;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

public class YouPresenter extends DefaultSupportFragmentLightCycle<YouFragment> implements YouView.Listener, ScrollContent {

    private final YouViewFactory youViewFactory;
    private final UserRepository userRepository;
    private final AccountOperations accountOperations;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final EventBus eventBus;
    private final FeatureOperations featureOperations;
    private final OfflineSettingsOperations offlineSettingsOperations;
    private final Navigator navigator;

    private Optional<YouView> youViewOpt = Optional.absent();
    private Optional<PropertySet> youOpt = Optional.absent();

    @Inject
    public YouPresenter(YouViewFactory youViewFactory,
                        UserRepository userRepository,
                        AccountOperations accountOperations,
                        ImageOperations imageOperations,
                        Resources resources,
                        EventBus eventBus,
                        FeatureOperations featureOperations,
                        OfflineSettingsOperations offlineSettingsOperations,
                        Navigator navigator) {
        this.youViewFactory = youViewFactory;
        this.userRepository = userRepository;
        this.accountOperations = accountOperations;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
        this.offlineSettingsOperations = offlineSettingsOperations;
        this.navigator = navigator;
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
        bindUserIfPresent();
    }

    @Override
    public void resetScroll() {
        if (youViewOpt.isPresent()){
            youViewOpt.get().resetScroll();
        }
    }

    private void setupOfflineSync(YouView youView) {
        if (featureOperations.isOfflineContentEnabled() || offlineSettingsOperations.hasOfflineContent()) {
            youView.showOfflineSettings();
        } else if (featureOperations.upsellMidTier()) {
            youView.showOfflineSettings();
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forSettingsImpression());
        } else {
            youView.hideOfflineSettings();
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
            final PropertySet you = youOpt.get();
            bindUser(headerView, you);
        }
    }

    private void bindUser(YouView headerView, PropertySet you) {
        headerView.setUsername(you.get(UserProperty.USERNAME));
        imageOperations.display(you.get(UserProperty.URN),
                ApiImageSize.getFullImageSize(resources),
                headerView.getProfileImageView());
    }

    private class YouSubscriber extends  DefaultSubscriber<PropertySet>{
        @Override
        public void onNext(PropertySet user) {
            youOpt = Optional.of(user);
            bindUserIfPresent();
        }
    }

    @Override
    public void onProfileClicked(View view) {
        if (youOpt.isPresent()) {
            navigator.openProfile(view.getContext(), youOpt.get().get(UserProperty.URN));
        }
    }

    @Override
    public void onActivitiesClicked(View view) {
        navigator.openActivities(view.getContext());
    }

    @Override
    public void onRecordClicked(View view) {
        navigator.openRecord(view.getContext(), Screen.YOU);
    }

    @Override
    public void onOfflineSettingsClicked(View view) {
        navigator.openOfflineSettings(view.getContext());
        if (featureOperations.upsellMidTier()) {
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forSettingsClick());
        }
    }

    @Override
    public void onNotificationSettingsClicked(View view) {
        navigator.openNotificationSettings(view.getContext());
    }

    @Override
    public void onBasicSettingsClicked(View view) {
        navigator.openSettings(view.getContext());
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
        new AlertDialog.Builder(view.getContext())
                .setTitle(R.string.sign_out_title)
                .setMessage(offlineSettingsOperations.hasOfflineContent()
                        ? R.string.sign_out_description_offline
                        : R.string.sign_out_description)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogoutActivity.start(view.getContext());
                    }
                }).show();
    }
}
