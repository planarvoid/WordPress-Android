package com.soundcloud.android.profile;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StartStationPresenter;
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

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final FeatureFlags featureFlags;
    private final StartStationPresenter startStationPresenter;

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
                                  FeatureFlags featureFlags,
                                  final Urn user, final AppCompatActivity profileActivity,
                                  final FollowingOperations followingOperations,
                                  final EngagementsTracking engagementsTracking,
                                  final StartStationPresenter startStationPresenter) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.featureFlags = featureFlags;
        this.startStationPresenter = startStationPresenter;

        ButterKnife.bind(this, profileActivity);

        if (accountOperations.isLoggedInUser(user)) {
            followButton.setVisibility(View.GONE);
        } else {
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    followingOperations
                            .toggleFollowing(user, followButton.isChecked())
                            .subscribe(new DefaultSubscriber<PropertySet>());
                    engagementsTracking.followUserUrn(user, followButton.isChecked());
                    updateStationButton();
                }
            });
        }

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FullImageDialog.show(profileActivity.getSupportFragmentManager(), user);
            }
        });
    }

    private void updateStationButton() {
        stationButton.setChecked(!(followButton.isChecked() || followButton.getVisibility() == View.GONE));
    }

    void setUserDetails(ProfileUser user) {
        username.setText(user.getName());
        setProfileButtons(user);

        if (!user.getUrn().equals(lastUser)) {
            lastUser = user.getUrn();
            imageOperations.displayCircularWithPlaceholder(user,
                                                           ApiImageSize.getFullImageSize(image.getResources()),
                                                           image);
        }
    }

    private void setProfileButtons(final ProfileUser user) {
        if (user.getFollowerCount() != Consts.NOT_SET) {
            followerCount.setText(numberFormatter.format(user.getFollowerCount()));
            followerCount.setVisibility(View.VISIBLE);
        } else {
            followerCount.setVisibility(View.GONE);
        }

        if (featureFlags.isEnabled(Flag.USER_STATIONS)) {
            showArtistStationButton(user);
        } else {
            // keep unchanged functionality unchanged until we ship user stations
            if (followButton instanceof ProfileToggleButton) {
                ((ProfileToggleButton) followButton).setTextOn(R.string.btn_following);
            }
        }

        followButton.setChecked(user.isFollowed());
        updateStationButton();
    }

    private void showArtistStationButton(final ProfileUser user) {
        if (user.getArtistStationUrn().isPresent()) {
            stationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateStationButton();
                    startStationPresenter.startStationForUser(v.getContext(), user.getArtistStationUrn().get());
                }
            });
            stationButton.setVisibility(View.VISIBLE);
        }
    }

    public static class ProfileHeaderPresenterFactory {

        private final ImageOperations imageOperations;
        private final CondensedNumberFormatter numberFormatter;
        private final AccountOperations accountOperations;
        private final FollowingOperations followingOperations;
        private final EngagementsTracking engagementsTracking;
        private final StartStationPresenter startStationPresenter;
        private final FeatureFlags featureFlags;

        @Inject
        public ProfileHeaderPresenterFactory(ImageOperations imageOperations,
                                             CondensedNumberFormatter numberFormatter,
                                             AccountOperations accountOperations,
                                             FollowingOperations followingOperations,
                                             EngagementsTracking engagementsTracking,
                                             StartStationPresenter startStationPresenter,
                                             FeatureFlags featureFlags) {
            this.imageOperations = imageOperations;
            this.numberFormatter = numberFormatter;
            this.accountOperations = accountOperations;
            this.followingOperations = followingOperations;
            this.engagementsTracking = engagementsTracking;
            this.startStationPresenter = startStationPresenter;
            this.featureFlags = featureFlags;
        }

        ProfileHeaderPresenter create(AppCompatActivity profileActivity, Urn user) {
            return new ProfileHeaderPresenter(imageOperations, numberFormatter, accountOperations,
                                              featureFlags, user, profileActivity,
                                              followingOperations, engagementsTracking, startStationPresenter);
        }
    }

}
