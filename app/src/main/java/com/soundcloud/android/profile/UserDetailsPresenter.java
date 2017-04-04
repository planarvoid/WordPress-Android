package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.SocialMediaLinkItem;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserProfileInfo;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import java.util.List;

class UserDetailsPresenter extends DefaultSupportFragmentLightCycle<UserDetailsFragment>
        implements SwipeRefreshLayout.OnRefreshListener {

    private MultiSwipeRefreshLayout refreshLayout;
    private Subscription subscription = RxUtils.invalidSubscription();

    private final UserProfileOperations profileOperations;
    private final UserDetailsView userDetailsView;
    private final CondensedNumberFormatter numberFormatter;
    private final Navigator navigator;

    private Urn userUrn;
    private SearchQuerySourceInfo searchQuerySourceInfo;
    private Observable<UserProfileInfo> userDetailsObservable;
    private UserProfileInfo userProfileInfo;

    UserDetailsPresenter(UserProfileOperations profileOperations,
                         UserDetailsView userDetailsView,
                         CondensedNumberFormatter numberFormatter,
                         Navigator navigator) {
        this.profileOperations = profileOperations;
        this.userDetailsView = userDetailsView;
        this.numberFormatter = numberFormatter;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(UserDetailsFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userUrn = fragment.getArguments().getParcelable(ProfileArguments.USER_URN_KEY);
        searchQuerySourceInfo = fragment.getArguments().getParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY);

        userDetailsObservable = profileOperations.userProfileInfo(userUrn).cache();
    }

    @Override
    public void onViewCreated(final UserDetailsFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        userDetailsView.setView(view);
        userDetailsView.setListener(new UserDetailsView.UserDetailsListener() {
            @Override
            public void onLinkClicked(SocialMediaLinkItem socialMediaLinkItem) {
                Intent intent = socialMediaLinkItem.toIntent();
                fragment.startActivity(intent);
            }

            @Override
            public void onViewFollowersClicked() {
                navigator.openFollowers(view.getContext(), userUrn, searchQuerySourceInfo);
            }

            @Override
            public void onViewFollowingClicked() {
                navigator.openFollowings(view.getContext(), userUrn, searchQuerySourceInfo);
            }
        });

        userDetailsView.showEmptyView(EmptyView.Status.WAITING);
        configureRefreshLayout(view);

        if (userProfileInfo != null) {
            updateViews(userProfileInfo);
        }

        loadUser();
    }

    private void configureRefreshLayout(View view) {
        refreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.str_layout);
        refreshLayout.setColorSchemeResources(R.color.soundcloud_orange);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setSwipeableChildren(R.id.user_details_holder, android.R.id.empty);
    }

    private void loadUser() {
        subscription.unsubscribe();
        subscription = userDetailsObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ProfileUserSubscriber());
    }

    @Override
    public void onDestroyView(UserDetailsFragment fragment) {
        refreshLayout = null;
        userDetailsView.setListener(null);
        userDetailsView.clearViews();
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onRefresh() {
        userDetailsObservable = profileOperations.userProfileInfo(userUrn).cache();
        loadUser();
    }

    private void updateViews(UserProfileInfo userProfileInfo) {
        setupFollows(userProfileInfo.getUser());
        setupDescription(userProfileInfo.getDescription());
        setupLinks(userProfileInfo.getSocialMediaLinks().getCollection());
    }

    private void setupFollows(User user) {
        userDetailsView.setFollowersCount(numberFormatter.format(user.followersCount()));
        userDetailsView.setFollowingsCount(numberFormatter.format(user.followingsCount()));
    }

    private void setupDescription(Optional<String> description) {
        if (isNotBlank(description)) {
            userDetailsView.showBio(description.get());
        } else {
            userDetailsView.hideBio();
        }
    }

    private void setupLinks(List<SocialMediaLinkItem> socialMediaLinks) {
        if (socialMediaLinks.isEmpty()) {
            userDetailsView.hideLinks();
        } else {
            userDetailsView.showLinks(socialMediaLinks);
        }
    }

    private boolean isNotBlank(Optional<String> stringOptional) {
        return stringOptional.isPresent() && Strings.isNotBlank(stringOptional.get());
    }

    private class ProfileUserSubscriber extends DefaultSubscriber<UserProfileInfo> {
        @Override
        public void onError(Throwable e) {
            super.onError(e);

            userDetailsView.hideEmptyView();

            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
            }
        }

        @Override
        public void onNext(UserProfileInfo userProfileInfo) {
            UserDetailsPresenter.this.userProfileInfo = userProfileInfo;
            updateViews(userProfileInfo);

            userDetailsView.hideEmptyView();
            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
            }
        }
    }
}
