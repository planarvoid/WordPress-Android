package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.User;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

class OldUserDetailsPresenter extends DefaultSupportFragmentLightCycle<ScrollableProfileFragment>
        implements SwipeRefreshLayout.OnRefreshListener {

    private boolean isNotEmpty;
    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private MultiSwipeRefreshLayout refreshLayout;
    private Subscription subscription = RxUtils.invalidSubscription();

    private final UserProfileOperations profileOperations;
    private final OldUserDetailsView oldUserDetailsView;

    private Urn userUrn;
    private Observable<User> userDetailsObservable;
    private User profileUser;

    OldUserDetailsPresenter(UserProfileOperations profileOperations, OldUserDetailsView oldUserDetailsView) {
        this.profileOperations = profileOperations;
        this.oldUserDetailsView = oldUserDetailsView;
    }

    @Override
    public void onCreate(ScrollableProfileFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userUrn = fragment.getArguments().getParcelable(ProfileArguments.USER_URN_KEY);

        userDetailsObservable = profileOperations.getLocalAndSyncedProfileUser(userUrn)
                                                 .cache();
    }

    @Override
    public void onViewCreated(final ScrollableProfileFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        oldUserDetailsView.setView(view);
        oldUserDetailsView.setListener(uri -> fragment.startActivity(new Intent(Intent.ACTION_VIEW, uri)));

        configureEmptyView();
        configureRefreshLayout(view);

        if (profileUser != null) {
            updateViews(profileUser);
        }

        loadUser();
    }

    private void configureRefreshLayout(View view) {
        refreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.str_layout);
        refreshLayout.setColorSchemeResources(R.color.ak_sc_orange);
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
    public void onDestroyView(ScrollableProfileFragment fragment) {
        refreshLayout = null;
        oldUserDetailsView.setListener(null);
        oldUserDetailsView.clearViews();
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onRefresh() {
        userDetailsObservable = profileOperations.getSyncedProfileUser(userUrn)
                                                 .cache();
        loadUser();
    }

    private void configureEmptyView() {
        if (!isNotEmpty) {
            oldUserDetailsView.showEmptyView(emptyViewStatus);
        } else {
            oldUserDetailsView.hideEmptyView();
        }
    }

    private void updateViews(User user) {
        isNotEmpty = hasDetails(user);
        setupWebsite(user);
        setupDiscogs(user);
        setupMyspace(user);
        setupDescription(user);
    }

    public boolean hasDetails(User user) {
        return hasDescription(user)
                || hasDiscogs(user)
                || hasWebsite(user)
                || hasMyspace(user);
    }

    private void setupDescription(User user) {
        if (hasDescription(user)) {
            oldUserDetailsView.showDescription(user.description().get());
        } else {
            oldUserDetailsView.hideDescription();
        }
    }

    private void setupWebsite(final User user) {
        if (hasWebsite(user)) {
            oldUserDetailsView.showWebsite(user.websiteUrl().get(), user.websiteName().get());
        } else {
            oldUserDetailsView.hideWebsite();
        }
    }

    private void setupDiscogs(final User user) {
        if (hasDiscogs(user)) {
            oldUserDetailsView.showDiscogs(user.discogsName().get());
        } else {
            oldUserDetailsView.hideDiscogs();
        }
    }

    private void setupMyspace(final User user) {
        if (hasMyspace(user)) {
            oldUserDetailsView.showMyspace(user.mySpaceName().get());
        } else {
            oldUserDetailsView.hideMyspace();
        }
    }

    private boolean hasMyspace(User user) {
        return user.mySpaceName().isPresent() && Strings.isNotBlank(user.mySpaceName().get());
    }

    private boolean hasWebsite(User user) {
        return user.websiteUrl().isPresent() && Strings.isNotBlank(user.websiteUrl().get());
    }

    private boolean hasDiscogs(User user) {
        return user.discogsName().isPresent() && Strings.isNotBlank(user.discogsName().get());
    }

    private boolean hasDescription(User user) {
        return user.description().isPresent() && Strings.isNotBlank(user.description().get());
    }

    private class ProfileUserSubscriber extends DefaultSubscriber<User> {
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
        public void onNext(User profileUser) {
            OldUserDetailsPresenter.this.profileUser = profileUser;
            updateViews(profileUser);
            configureEmptyView();
        }
    }
}
