package com.soundcloud.android.profile;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.ProfileToggleButton;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class ProfileHeaderPresenter extends ActivityLightCycleDispatcher<RootActivity> {

    private static final int POSITION_IN_CONTEXT = 0;

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final AccountOperations accountOperations;
    private final FollowingOperations followingOperations;
    private final EngagementsTracking engagementsTracking;
    private final StartStationHandler stationHandler;
    private final ScreenProvider screenProvider;
    private final ProfileImageHelper profileImageHelper;

    @Bind(R.id.header_info_layout) View headerInfoLayout;
    @Bind(R.id.tab_indicator) View tabs;
    @Bind(R.id.username) TextView username;
    @Bind(R.id.image) ImageView image;
    @Nullable @Bind(R.id.profile_banner) ImageView banner; // not present in certain configurations
    @Bind(R.id.followers_count) TextView followerCount;
    @Bind(R.id.toggle_btn_follow) ToggleButton followButton;
    @Bind(R.id.btn_station) ToggleButton stationButton;

    private Urn lastUser;

    @Inject
    ProfileHeaderPresenter(ImageOperations imageOperations,
                           CondensedNumberFormatter numberFormatter,
                           AccountOperations accountOperations,
                           FollowingOperations followingOperations,
                           EngagementsTracking engagementsTracking,
                           StartStationHandler stationHandler,
                           ScreenProvider screenProvider,
                           ProfileImageHelper profileImageHelper) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.accountOperations = accountOperations;
        this.followingOperations = followingOperations;
        this.engagementsTracking = engagementsTracking;
        this.stationHandler = stationHandler;
        this.screenProvider = screenProvider;
        this.profileImageHelper = profileImageHelper;
    }

    @Override
    public void onCreate(final RootActivity activity, @Nullable Bundle bundle) {
        super.onCreate(activity, bundle);

        ButterKnife.bind(this, activity);

        final Urn user = ProfileActivity.getUserUrnFromIntent(activity.getIntent());
        if (accountOperations.isLoggedInUser(user)) {
            followButton.setVisibility(View.GONE);
        } else {
            followButton.setOnClickListener(createOnClickListener(user,
                                                                  followingOperations,
                                                                  engagementsTracking,
                                                                  screenProvider));
        }
        stationButton.setVisibility(View.GONE);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FullImageDialog.show(activity.getSupportFragmentManager(), user);
            }
        });
    }

    @Override
    public void onDestroy(RootActivity activity) {
        profileImageHelper.unsubscribe();
        super.onDestroy(activity);
    }

    private View.OnClickListener createOnClickListener(final Urn user,
                                                       final FollowingOperations followingOperations,
                                                       final EngagementsTracking engagementsTracking,
                                                       final ScreenProvider screenProvider) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                followingOperations
                        .toggleFollowing(user, followButton.isChecked())
                        .subscribe(new DefaultSubscriber<PropertySet>());

                engagementsTracking.followUserUrn(user,
                                                  followButton.isChecked(),
                                                  getEventContextMetadata(screenProvider));

                updateStationButton();
            }
        };
    }

    private EventContextMetadata getEventContextMetadata(ScreenProvider screenProvider) {
        return EventContextMetadata.builder()
                                   .module(Module.create(Module.SINGLE, POSITION_IN_CONTEXT))
                                   .pageName(screenProvider.getLastScreen().get())
                                   .build();
    }

    void setUserDetails(ProfileUser user) {
        username.setText(user.getName());
        setUserImage(user);
        setFollowerCount(user);
        setFollowingButton(user);
        setArtistStation(user);
    }

    private void setUserImage(ProfileUser user) {
        if (!user.getUrn().equals(lastUser)) {
            lastUser = user.getUrn();

            if (banner != null) {
                profileImageHelper.bindImages(new ProfileImageSource(user), banner, image);
            } else {
                imageOperations.displayCircularWithPlaceholder(user,
                                                               ApiImageSize.getFullImageSize(image.getResources()),
                                                               image);
            }
        }
    }

    private void setFollowerCount(ProfileUser user) {
        if (user.getFollowerCount() != Consts.NOT_SET) {
            followerCount.setText(numberFormatter.format(user.getFollowerCount()));
            followerCount.setVisibility(View.VISIBLE);
        } else {
            followerCount.setVisibility(View.GONE);
        }
    }

    private void setFollowingButton(ProfileUser user) {
        boolean hasArtistStation = user.getArtistStationUrn().isPresent();
        boolean stationVisible = stationButton.getVisibility() == View.VISIBLE;
        boolean isFollowed = user.isFollowed();

        if (followButton instanceof ProfileToggleButton) {
            if (isFollowed) {
                if (hasArtistStation || stationVisible) {
                    ((ProfileToggleButton) followButton).setTextOn(Consts.NOT_SET);
                } else {
                    ((ProfileToggleButton) followButton).setTextOn(R.string.btn_following);
                }
            }
        }

        followButton.setChecked(isFollowed);
    }

    private void setArtistStation(final ProfileUser user) {
        boolean hasArtistStation = user.getArtistStationUrn().isPresent();

        if (hasArtistStation) {
            stationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateStationButton();
                    stationHandler.startStation(v.getContext(), user.getArtistStationUrn().get());
                }
            });
            stationButton.setVisibility(View.VISIBLE);
            updateStationButton();
        }
    }

    private void updateStationButton() {
        boolean notFollowing = !followButton.isChecked();
        boolean followButtonVisible = followButton.getVisibility() == View.VISIBLE;

        stationButton.setChecked(notFollowing && followButtonVisible);
    }
}
