package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.NextFollowingOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;

import android.app.Activity;
import android.graphics.Color;
import android.support.design.widget.CollapsingToolbarLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class ProfileHeaderPresenter {

    private final ImageOperations imageOperations;

    @InjectView(R.id.header_info_layout) View headerInfoLayout;
    @InjectView(R.id.indicator) View tabs;
    @InjectView(R.id.username) TextView username;
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.followers_count) TextView followerCount;
    @InjectView(R.id.follow_button) ToggleButton followButton;
    @InjectView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;

    private Urn lastUser;

    public ProfileHeaderPresenter(Activity profileActivity, ImageOperations imageOperations,
                                  AccountOperations accountOperations, final Urn user,
                                  final NextFollowingOperations followingOperations) {
        this.imageOperations = imageOperations;

        ButterKnife.inject(this, profileActivity);

        if (accountOperations.isLoggedInUser(user)){
            followButton.setVisibility(View.GONE);
        } else {
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    followingOperations.toggleFollowing(user, followButton.isChecked()).subscribe(new DefaultSubscriber<PropertySet>());
                }
            });
        }
    }

    public void setUserDetails(ProfileUser user) {
        collapsingToolbarLayout.setExpandedTitleColor(Color.BLACK);
        collapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
        username.setText(user.getName());
        followerCount.setText(user.getFollowerCount());
        followButton.setChecked(user.isFollowed());

        if (!user.getUrn().equals(lastUser)){
            lastUser = user.getUrn();
            imageOperations.displayInAdapterView(lastUser,
                    ApiImageSize.getFullImageSize(image.getResources()),
                    image);
        }
    }

    public static class ProfileHeaderPresenterFactory {

        private final ImageOperations imageOperations;
        private final AccountOperations accountOperations;
        private final NextFollowingOperations followingOperations;

        @Inject
        public ProfileHeaderPresenterFactory(ImageOperations imageOperations, AccountOperations accountOperations,
                                             NextFollowingOperations followingOperations) {
            this.imageOperations = imageOperations;
            this.accountOperations = accountOperations;
            this.followingOperations = followingOperations;
        }

        ProfileHeaderPresenter create(Activity profileActivity, Urn user) {
            return new ProfileHeaderPresenter(profileActivity, imageOperations, accountOperations, user, followingOperations);
        }
    }

}
