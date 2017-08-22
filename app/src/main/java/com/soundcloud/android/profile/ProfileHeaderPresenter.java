package com.soundcloud.android.profile;

import butterknife.BindView;
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
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.users.User;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.ProfileToggleButton;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class ProfileHeaderPresenter extends DefaultActivityLightCycle<RootActivity> {

    private static final int POSITION_IN_CONTEXT = 0;

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final AccountOperations accountOperations;
    private final FollowingOperations followingOperations;
    private final EngagementsTracking engagementsTracking;
    private final StartStationHandler stationHandler;
    private final ScreenProvider screenProvider;
    private final ProfileImageHelper profileImageHelper;

    @BindView(R.id.header_info_layout) View headerInfoLayout;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.image) ImageView image;
    @Nullable @BindView(R.id.profile_banner) ImageView banner; // not present in certain configurations
    @BindView(R.id.followers_count) TextView followerCount;
    @BindView(R.id.toggle_btn_follow) ToggleButton followButton;
    @BindView(R.id.btn_station) ToggleButton stationButton;
    @BindView(R.id.pro_badge) ImageView proBadge;

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
                                                                  engagementsTracking));
        }
        stationButton.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy(RootActivity activity) {
        profileImageHelper.unsubscribe();
        super.onDestroy(activity);
    }

    private View.OnClickListener createOnClickListener(final Urn user,
                                                       final FollowingOperations followingOperations,
                                                       final EngagementsTracking engagementsTracking) {
        return v -> {
            followingOperations.toggleFollowing(user, followButton.isChecked()).subscribe(new DefaultDisposableCompletableObserver());
            engagementsTracking.followUserUrn(user,
                                              followButton.isChecked(),
                                              getEventContextMetadata());
            updateStationButton();
        };
    }

    private EventContextMetadata getEventContextMetadata() {
        return EventContextMetadata.builder()
                                   .module(Module.create(Module.SINGLE, POSITION_IN_CONTEXT))
                                   .pageName(screenProvider.getLastScreenTag())
                                   .build();
    }

    void setUserDetails(User user) {
        username.setText(user.username());
        setUserImage(user);
        setFollowerCount(user);
        setFollowingButton(user);
        setArtistStation(user);
        setProBadge(user);
    }

    private void setProBadge(User user) {
        final int visibility = user.isPro() ? View.VISIBLE : View.GONE;
        proBadge.setVisibility(visibility);
    }

    private void setUserImage(User user) {
        if (!user.urn().equals(lastUser)) {
            lastUser = user.urn();
            final ImageResource imageResource = getImageResource(user);
            if (banner != null) {
                profileImageHelper.bindImages(new ProfileImageSource(user), banner, image);
            } else {
                imageOperations.displayCircularWithPlaceholder(imageResource,
                                                               ApiImageSize.getFullImageSize(image.getResources()),
                                                               image);
            }
            image.setOnClickListener(view -> FullImageDialog.show(ViewUtils.getFragmentActivity(view).getSupportFragmentManager(), imageResource));
        }
    }

    @NonNull
    private ImageResource getImageResource(final User user) {
        return SimpleImageResource.create(user.urn(), user.avatarUrl());
    }

    private void setFollowerCount(User user) {
        if (user.followersCount() != Consts.NOT_SET) {
            followerCount.setText(numberFormatter.format(user.followersCount()));
            followerCount.setVisibility(View.VISIBLE);
        } else {
            followerCount.setVisibility(View.GONE);
        }
    }

    private void setFollowingButton(User user) {
        boolean hasArtistStation = user.artistStation().isPresent();
        boolean stationVisible = stationButton.getVisibility() == View.VISIBLE;
        boolean isFollowed = user.isFollowing();

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

    private void setArtistStation(final User user) {
        boolean hasArtistStation = user.artistStation().isPresent();

        if (hasArtistStation) {
            stationButton.setOnClickListener(v -> {
                updateStationButton();
                stationHandler.startStation(user.artistStation().get());
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
