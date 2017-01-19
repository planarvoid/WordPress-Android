package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.User;
import com.soundcloud.android.util.CondensedNumberFormatter;
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

class UserDetailsPresenter extends DefaultSupportFragmentLightCycle<UserDetailsFragment>
        implements SwipeRefreshLayout.OnRefreshListener {

    private MultiSwipeRefreshLayout refreshLayout;
    private Subscription subscription = RxUtils.invalidSubscription();

    private final UserProfileOperations profileOperations;
    private final UserDetailsView userDetailsView;
    private final CondensedNumberFormatter numberFormatter;

    private Urn userUrn;
    private Observable<User> userDetailsObservable;
    private User profileUser;

    UserDetailsPresenter(UserProfileOperations profileOperations,
                         UserDetailsView userDetailsView,
                         CondensedNumberFormatter numberFormatter) {
        this.profileOperations = profileOperations;
        this.userDetailsView = userDetailsView;
        this.numberFormatter = numberFormatter;
    }

    @Override
    public void onCreate(UserDetailsFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userUrn = fragment.getArguments().getParcelable(ProfileArguments.USER_URN_KEY);

        userDetailsObservable = profileOperations.getLocalAndSyncedProfileUser(userUrn)
                                                 .cache();
    }

    @Override
    public void onViewCreated(final UserDetailsFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        userDetailsView.setView(view);
        userDetailsView.setListener(uri -> fragment.startActivity(new Intent(Intent.ACTION_VIEW, uri)));

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
    public void onDestroyView(UserDetailsFragment fragment) {
        refreshLayout = null;
        userDetailsView.setListener(null);
        userDetailsView.clearViews();
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onRefresh() {
        userDetailsObservable = profileOperations.getSyncedProfileUser(userUrn)
                                                 .cache();
        loadUser();
    }

    private void updateViews(User user) {
        setupFollows(user);
        setupBio(user);
        setupLinks(user);
    }

    private void setupFollows(User user) {
        userDetailsView.setFollowersCount(numberFormatter.format(user.followersCount()));
        userDetailsView.setFollowingsCount(numberFormatter.format(user.followingsCount()));
    }

    private void setupBio(User user) {
        if (hasDescription(user)) {
            userDetailsView.showBio(user.description().get());
        } else {
            userDetailsView.hideBio();
        }
    }

    private void setupLinks(User user) {
        if (user.socialMediaLinks().isEmpty()) {
            userDetailsView.hideLinks();
        } else {
            userDetailsView.showLinks(user.socialMediaLinks());
        }
    }

    private boolean hasDescription(User user) {
        return user.description().isPresent() && Strings.isNotBlank(user.description().get());
    }

    private class ProfileUserSubscriber extends DefaultSubscriber<User> {
        @Override
        public void onCompleted() {
            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
            }
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);

            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
            }
        }

        @Override
        public void onNext(User profileUser) {
            UserDetailsPresenter.this.profileUser = profileUser;
            updateViews(profileUser);
        }
    }
}
