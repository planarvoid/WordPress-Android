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
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.ProfileToggleButton;
import com.soundcloud.java.collections.PropertySet;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class ProfileHeaderPresenter {

    private static final int POSITION_IN_CONTEXT = 0;

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final StartStationHandler stationHandler;

    @Bind(R.id.header_info_layout) View headerInfoLayout;
    @Bind(R.id.tab_indicator) View tabs;
    @Bind(R.id.username) TextView username;
    @Bind(R.id.image) ImageView image;
    @Bind(R.id.followers_count) TextView followerCount;
    @Bind(R.id.toggle_btn_follow) ToggleButton followButton;
    @Bind(R.id.btn_station) ToggleButton stationButton;

    private Urn lastUser;

    public ProfileHeaderPresenter(ImageOperations imageOperations,
                                  CondensedNumberFormatter numberFormatter,
                                  AccountOperations accountOperations,
                                  final Urn user, final AppCompatActivity profileActivity,
                                  final FollowingOperations followingOperations,
                                  final EngagementsTracking engagementsTracking,
                                  final StartStationHandler stationHandler,
                                  final ScreenProvider screenProvider) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.stationHandler = stationHandler;

        ButterKnife.bind(this, profileActivity);

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
                FullImageDialog.show(profileActivity.getSupportFragmentManager(), user);
            }
        });
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
            imageOperations.displayCircularWithPlaceholder(user,
                                                           ApiImageSize.getFullImageSize(image.getResources()),
                                                           image);
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

    public static class ProfileHeaderPresenterFactory {

        private final ImageOperations imageOperations;
        private final CondensedNumberFormatter numberFormatter;
        private final AccountOperations accountOperations;
        private final FollowingOperations followingOperations;
        private final EngagementsTracking engagementsTracking;
        private final StartStationHandler stationHandler;
        private final ScreenProvider screenProvider;

        @Inject
        public ProfileHeaderPresenterFactory(ImageOperations imageOperations,
                                             CondensedNumberFormatter numberFormatter,
                                             AccountOperations accountOperations,
                                             FollowingOperations followingOperations,
                                             EngagementsTracking engagementsTracking,
                                             StartStationHandler stationHandler,
                                             ScreenProvider screenProvider) {
            this.imageOperations = imageOperations;
            this.numberFormatter = numberFormatter;
            this.accountOperations = accountOperations;
            this.followingOperations = followingOperations;
            this.engagementsTracking = engagementsTracking;
            this.stationHandler = stationHandler;
            this.screenProvider = screenProvider;
        }

        ProfileHeaderPresenter create(AppCompatActivity profileActivity, Urn user) {
            return new ProfileHeaderPresenter(imageOperations, numberFormatter, accountOperations,
                                              user, profileActivity, followingOperations, engagementsTracking,
                                              stationHandler, screenProvider);
        }
    }

}
