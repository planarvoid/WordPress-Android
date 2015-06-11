package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileHeaderPresenter.ProfileHeaderPresenterFactory;
import static com.soundcloud.android.profile.ProfilePagerRefreshHelper.ProfilePagerRefreshHelperFactory;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.SlidingTabLayout;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class ProfilePresenter extends DefaultLightCycleActivity<AppCompatActivity> {

    private final ProfileHeaderPresenterFactory profileHeaderPresenterFactory;
    private final ProfilePagerRefreshHelperFactory profilePagerRefreshHelperFactory;
    private final ProfileOperations profileOperations;
    private final EventBus eventBus;

    private ViewPager pager;
    private ProfilePagerRefreshHelper refreshHelper;
    private Subscription userSubscription = Subscriptions.empty();
    private Subscription userUpdatedSubscription = Subscriptions.empty();
    private ProfileHeaderPresenter headerPresenter;
    private Urn user;

    private final Func1<EntityStateChangedEvent, Boolean> isProfileUser = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent entityStateChangedEvent) {
            return entityStateChangedEvent.getChangeMap().containsKey(user);
        }
    };

    @Inject
    public ProfilePresenter(ProfilePagerRefreshHelperFactory profilePagerRefreshHelperFactory,
                            ProfileHeaderPresenterFactory profileHeaderPresenterFactory,
                            ProfileOperations profileOperations, EventBus eventBus) {
        this.profilePagerRefreshHelperFactory = profilePagerRefreshHelperFactory;
        this.profileHeaderPresenterFactory = profileHeaderPresenterFactory;
        this.profileOperations = profileOperations;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);

        user = activity.getIntent().getParcelableExtra(ProfileActivity.EXTRA_USER_URN);
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

        refreshUser();

        userUpdatedSubscription = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(isProfileUser)
                .subscribe(new RefreshUserSubscriber());
    }

    public void refreshUser() {
        userSubscription.unsubscribe();
        userSubscription = profileOperations
                .getLocalProfileUser(user)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new UserDetailsSubscriber());
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        pager = null;
        refreshHelper = null;
        userUpdatedSubscription.unsubscribe();
        userSubscription.unsubscribe();
        super.onDestroy(activity);
    }

    private final class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            refreshHelper.setRefreshablePage(position);
        }
    }

    private final class UserDetailsSubscriber extends DefaultSubscriber<ProfileUser> {
        @Override
        public void onNext(ProfileUser profileUser) {
            headerPresenter.setUserDetails(profileUser);
        }
    }

    private final class RefreshUserSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent args) {
            refreshUser();
        }
    }
}