package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

class UserDetailsPresenter extends DefaultSupportFragmentLightCycle<UserDetailsFragment>
        implements SwipeRefreshLayout.OnRefreshListener, RefreshableProfileItem {

    private boolean isNotEmpty;
    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private MultiSwipeRefreshLayout refreshLayout;
    private Subscription subscription = Subscriptions.empty();
    private View[] refreshViews;

    private final ProfileOperations profileOperations;
    private final UserDetailsView userDetailsView;
    private final UserDetailsScroller userDetailsScroller;

    private final Func1<ProfileUser, Boolean> hasDetails = new Func1<ProfileUser, Boolean>() {
        @Override
        public Boolean call(ProfileUser profileUser) {
            return profileUser.hasDescription();
        }
    };
    private Urn userUrn;
    private Observable<ProfileUser> userDetailsObservable;

    public UserDetailsPresenter(ProfileOperations profileOperations, UserDetailsView userDetailsView, UserDetailsScroller userDetailsScroller) {
        this.profileOperations = profileOperations;
        this.userDetailsView = userDetailsView;
        this.userDetailsScroller = userDetailsScroller;
    }

    @Override
    public void onCreate(UserDetailsFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userUrn = getUserUrn(fragment);
        createUserDetailsObservable();
    }

    @Override
    public void onViewCreated(final UserDetailsFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        userDetailsView.setView(view);
        userDetailsView.setListener(new UserDetailsView.UserDetailsListener() {
            @Override
            public void onViewUri(Uri uri) {
                fragment.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });

        configureEmptyView();

        final View userDetailsView = view.findViewById(R.id.user_details_holder);
        final EmptyView emptyView = (EmptyView) view.findViewById(android.R.id.empty);
        refreshViews = new View[]{userDetailsView, emptyView};
        if (refreshLayout != null){
            refreshLayout.setSwipeableChildren(refreshViews);
        }

        userDetailsScroller.setViews(userDetailsView, emptyView);
        loadUser();
    }

    private void loadUser() {
        subscription.unsubscribe();
        subscription = userDetailsObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ProfileUserSubscriber());
    }

    private Urn getUserUrn(UserDetailsFragment fragment) {
        return fragment.getActivity().getIntent().getParcelableExtra(ProfileActivity.EXTRA_USER_URN);
    }

    @Override
    public void onDestroyView(UserDetailsFragment fragment) {
        refreshViews = null;
        userDetailsView.setListener(null);
        userDetailsView.clearViews();
        userDetailsScroller.clearViews();
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
        createUserDetailsObservable();
        loadUser();
    }

    private void createUserDetailsObservable() {
        userDetailsObservable = profileOperations.getLocalAndSyncedProfileUser(userUrn)
                .filter(hasDetails)
                .cache();
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

            emptyViewStatus = ErrorUtils.emptyViewStatusFromError(e);
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
