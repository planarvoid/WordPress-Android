package com.soundcloud.android.profile;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.NextFollowingOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.util.CondensedNumberFormatter;
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
    private final CondensedNumberFormatter numberFormatter;

    @Bind(R.id.header_info_layout) View headerInfoLayout;
    @Bind(R.id.indicator) View tabs;
    @Bind(R.id.username) TextView username;
    @Bind(R.id.image) ImageView image;
    @Bind(R.id.followers_count) TextView followerCount;
    @Bind(R.id.toggle_btn_follow) ToggleButton followButton;
    @Bind(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;

    private Urn lastUser;

    public ProfileHeaderPresenter(Activity profileActivity, ImageOperations imageOperations,
                                  CondensedNumberFormatter numberFormatter, AccountOperations accountOperations,
                                  final Urn user, final NextFollowingOperations followingOperations) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;

        ButterKnife.bind(this, profileActivity);

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
        followerCount.setText(numberFormatter.format(user.getFollowerCount()));
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
        private final CondensedNumberFormatter numberFormatter;
        private final AccountOperations accountOperations;
        private final NextFollowingOperations followingOperations;

        @Inject
        public ProfileHeaderPresenterFactory(ImageOperations imageOperations, CondensedNumberFormatter numberFormatter,
                                             AccountOperations accountOperations, NextFollowingOperations followingOperations) {
            this.imageOperations = imageOperations;
            this.numberFormatter = numberFormatter;
            this.accountOperations = accountOperations;
            this.followingOperations = followingOperations;
        }

        ProfileHeaderPresenter create(Activity profileActivity, Urn user) {
            return new ProfileHeaderPresenter(profileActivity, imageOperations, numberFormatter, accountOperations,
                    user, followingOperations);
        }
    }

}
