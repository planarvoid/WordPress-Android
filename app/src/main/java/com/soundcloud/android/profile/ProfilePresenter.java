package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileHeaderPresenter.ProfileHeaderPresenterFactory;
import static com.soundcloud.android.profile.ProfilePagerRefreshHelper.ProfilePagerRefreshHelperFactory;

import com.soundcloud.android.R;
import com.soundcloud.android.model.ParcelableUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.SlidingTabLayout;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class ProfilePresenter extends DefaultLightCycleActivity<AppCompatActivity> implements ProfileUserProvider {

    private final ProfileHeaderPresenterFactory profileHeaderPresenterFactory;
    private final ProfilePagerRefreshHelperFactory profilePagerRefreshHelperFactory;
    private final ProfileOperations profileOperations;

    private ViewPager pager;
    private ProfilePagerRefreshHelper refreshHelper;
    private Subscription userSubscription = Subscriptions.empty();
    private ProfileHeaderPresenter headerPresenter;
    private Urn user;
    private ConnectableObservable<ProfileUser> currentUserObservable;

    @Inject
    public ProfilePresenter(ProfilePagerRefreshHelperFactory profilePagerRefreshHelperFactory,
                            ProfileHeaderPresenterFactory profileHeaderPresenterFactory,
                            ProfileOperations profileOperations) {
        this.profilePagerRefreshHelperFactory = profilePagerRefreshHelperFactory;
        this.profileHeaderPresenterFactory = profileHeaderPresenterFactory;
        this.profileOperations = profileOperations;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);

        user = ParcelableUrn.unpack(ProfileActivity.EXTRA_USER_URN, activity.getIntent().getExtras());
        refreshHelper = profilePagerRefreshHelperFactory.create((MultiSwipeRefreshLayout) activity.findViewById(R.id.str_layout));
        headerPresenter = profileHeaderPresenterFactory.create(activity.findViewById(R.id.profile_header));

        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setAdapter(new ProfilePagerAdapter(activity, headerPresenter, refreshHelper, user));
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(activity.getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        SlidingTabLayout tabIndicator = (SlidingTabLayout) activity.findViewById(R.id.indicator);
        tabIndicator.setViewPager(pager);
        tabIndicator.setOnPageChangeListener(new PageChangeListener());
        refreshHelper.setRefreshablePage(0);

        ConnectableObservable<ProfileUser> loadObservable = getStoredObservable(activity);
        if (loadObservable == null) {
            loadObservable = profileOperations.getUserDetails(user)
                    .observeOn(AndroidSchedulers.mainThread()).replay();
        }
        subscribeAndConnect(loadObservable);
    }

    @SuppressWarnings("unchecked")
    private ConnectableObservable<ProfileUser> getStoredObservable(AppCompatActivity activity) {
        return (ConnectableObservable<ProfileUser>) activity.getLastCustomNonConfigurationInstance();
    }

    @Override
    public Observable<ProfileUser> user() {
        return currentUserObservable;
    }

    public Observable<ProfileUser> refreshUser() {
        final ConnectableObservable<ProfileUser> refreshObservable = profileOperations.updatedUserDetails(user)
                .observeOn(AndroidSchedulers.mainThread())
                .replay();
        subscribeAndConnect(refreshObservable);
        return refreshObservable;
    }

    private void subscribeAndConnect(ConnectableObservable<ProfileUser> observable) {
        currentUserObservable = observable;
        userSubscription.unsubscribe();
        currentUserObservable.subscribe(new UserDetailsSubscriber());
        userSubscription = currentUserObservable.connect();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        pager = null;
        refreshHelper = null;
        userSubscription.unsubscribe();
        super.onDestroy(activity);
    }

    private final class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            refreshHelper.setRefreshablePage(position);
        }
    }

    private class UserDetailsSubscriber extends DefaultSubscriber<ProfileUser> {
        @Override
        public void onNext(ProfileUser profileUser) {
            headerPresenter.setUserDetails(profileUser);
        }
    }
}