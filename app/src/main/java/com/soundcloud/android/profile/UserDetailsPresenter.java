package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

public class UserDetailsPresenter extends DefaultSupportFragmentLightCycle<UserDetailsFragment> implements SwipeRefreshLayout.OnRefreshListener, RefreshAware {

    private boolean isNotEmpty;
    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private MultiSwipeRefreshLayout refreshLayout;
    private Subscription subscription = Subscriptions.empty();
    private ProfileActivity activity;
    private View[] refreshViews;

    private final UserDetailsView userDetailsView;

    private final Func1<ProfileUser, Boolean> hasDetails = new Func1<ProfileUser, Boolean>() {
        @Override
        public Boolean call(ProfileUser profileUser) {
            return profileUser.hasDescription();
        }
    };

    @Override
    public void onCreate(UserDetailsFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        // we dont have a proper onAttach method in LightCycle, so this will have to do
        activity = (ProfileActivity) fragment.getActivity();
    }

    public UserDetailsPresenter(UserDetailsView userDetailsView) {
        this.userDetailsView = userDetailsView;
    }

    public void setHeaderSize(int currentHeaderSize) {
        userDetailsView.setTopPadding(currentHeaderSize);
    }

    @Override
    public void onViewCreated(UserDetailsFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        setHeaderSize(view.getResources().getDimensionPixelSize(R.dimen.profile_header_expanded_height));
        configureEmptyView();

        refreshViews = new View[]{view.findViewById(R.id.user_details_holder), view.findViewById(android.R.id.empty)};
        if (refreshLayout != null){
            refreshLayout.setSwipeableChildren(refreshViews);
        }
        subscribeToUserObservable(((ProfileActivity) fragment.getActivity()).profileUserProvider().user());
    }

    @Override
    public void onDestroyView(UserDetailsFragment fragment) {
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void attachRefreshLayout(MultiSwipeRefreshLayout refreshLayout) {
        this.refreshLayout = refreshLayout;
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setSwipeableChildren(refreshViews);
    }

    @Override
    public void detachRefreshLayout() {
        this.refreshLayout = null;
    }

    @Override
    public void onRefresh() {
        subscribeToUserObservable(activity.profileUserProvider().refreshUser());
    }


    private void subscribeToUserObservable(Observable<ProfileUser> currentUserObservable) {
        subscription.unsubscribe();
        subscription = currentUserObservable.filter(hasDetails)
                .subscribe(new ProfileUserSubscriber());
    }

    private void configureEmptyView() {
        if (!isNotEmpty){
            userDetailsView.showEmptyView(emptyViewStatus);
        } else {
            userDetailsView.hideEmptyView();
        }
    }

    private void updateViews(ProfileUser user) {
        isNotEmpty = user.hasDetails();
        setupWebsite(user);
        setupDiscogs(user);
        setupMyspace(user);
        setupDescription(user);
    }

    private void setupDescription(ProfileUser user) {
        if (ScTextUtils.isNotBlank(user.getDescription())) {
            userDetailsView.showDescription(user.getDescription());
        } else {
            userDetailsView.hideDescription();
        }
    }

    private void setupWebsite(final ProfileUser user) {
        final String websiteUrl = user.getWebsiteUrl();
        if (ScTextUtils.isNotBlank(websiteUrl)) {
            userDetailsView.showWebsite(websiteUrl, user.getWebsiteName());
        } else {
            userDetailsView.hideWebsite();
        }
    }

    private void setupDiscogs(final ProfileUser user) {
        if (ScTextUtils.isNotBlank(user.getDiscogsName())) {
            userDetailsView.showDiscogs(user.getDiscogsName());
        } else {
            userDetailsView.hideDiscogs();
        }
    }

    private void setupMyspace(final ProfileUser user) {
        if (ScTextUtils.isNotBlank(user.getMyspaceName())) {
            userDetailsView.showMyspace(user.getMyspaceName());
        } else {
            userDetailsView.hideMyspace();
        }
    }

    private class ProfileUserSubscriber extends DefaultSubscriber<ProfileUser> {
        @Override
        public void onCompleted() {
            emptyViewStatus = EmptyView.Status.OK;
            configureEmptyView();

            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
            }
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            emptyViewStatus = EmptyView.Status.ERROR;
            configureEmptyView();

            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
            }
        }

        @Override
        public void onNext(ProfileUser profileUser) {
            updateViews(profileUser);
            configureEmptyView();
        }
    }
}
