package com.soundcloud.android.profile;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
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

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class UserDetailsPresenter extends DefaultSupportFragmentLightCycle<UserDetailsFragment>
        implements SwipeRefreshLayout.OnRefreshListener {

    private MultiSwipeRefreshLayout refreshLayout;
    private Subscription subscription = RxUtils.invalidSubscription();

    private final UserProfileOperations profileOperations;
    private final UserDetailsView userDetailsView;
    private final CondensedNumberFormatter numberFormatter;
    private final Navigator navigator;
    private final ScreenProvider screenProvider;

    private Urn userUrn;
    private Optional<SearchQuerySourceInfo> searchQuerySourceInfo;
    private Observable<UserProfileInfo> userDetailsObservable;
    private UserProfileInfo userProfileInfo;

    @Inject
    UserDetailsPresenter(UserProfileOperations profileOperations,
                         UserDetailsView userDetailsView,
                         CondensedNumberFormatter numberFormatter,
                         Navigator navigator,
                         ScreenProvider screenProvider) {
        this.profileOperations = profileOperations;
        this.userDetailsView = userDetailsView;
        this.numberFormatter = numberFormatter;
        this.navigator = navigator;
        this.screenProvider = screenProvider;
    }

    @Override
    public void onCreate(UserDetailsFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userUrn = fragment.getArguments().getParcelable(ProfileArguments.USER_URN_KEY);
        searchQuerySourceInfo = Optional.fromNullable(fragment.getArguments().getParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY));

        userDetailsObservable = profileOperations.userProfileInfo(userUrn).cache();
    }

    @Override
    public void onViewCreated(final UserDetailsFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        userDetailsView.setView(view);
        userDetailsView.setListener(new UserDetailsView.UserDetailsListener() {
            @Override
            public void onLinkClicked(SocialMediaLinkItem socialMediaLinkItem) {
                navigator.navigateTo(NavigationTarget.forNavigation(fragment.getActivity(),
                                                                    socialMediaLinkItem.url(),
                                                                    Optional.absent(),
                                                                    screenProvider.getLastScreen(),
                                                                    Optional.of(DiscoverySource.RECOMMENDATIONS))); // TODO (REC-1302): Use correct one
            }

            @Override
            public void onViewFollowersClicked() {
                navigator.navigateTo(NavigationTarget.forFollowers(getFragmentActivity(view), userUrn, searchQuerySourceInfo));
            }

            @Override
            public void onViewFollowingClicked() {
                navigator.navigateTo(NavigationTarget.forFollowings(getFragmentActivity(view), userUrn, searchQuerySourceInfo));
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
