package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileHeaderPresenter.ProfileHeaderPresenterFactory;
import static com.soundcloud.android.profile.ProfilePagerAdapter.TAB_FOLLOWERS;
import static com.soundcloud.android.profile.ProfilePagerAdapter.TAB_FOLLOWINGS;
import static com.soundcloud.android.profile.ProfilePagerAdapter.TAB_INFO;
import static com.soundcloud.android.profile.ProfilePagerAdapter.TAB_SOUNDS;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ActivityReferringEventProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import javax.inject.Inject;

class ProfilePresenter extends ActivityLightCycleDispatcher<RootActivity>
        implements EnterScreenDispatcher.Listener {

    final @LightCycle ProfileScrollHelper scrollHelper;
    final @LightCycle ActivityReferringEventProvider referringEventProvider;
    final @LightCycle EnterScreenDispatcher enterScreenDispatcher;
    private final ScreenProvider screenProvider;
    private final ProfileHeaderPresenterFactory profileHeaderPresenterFactory;
    private final UserProfileOperations profileOperations;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final EventTracker eventTracker;

    private ViewPager pager;
    private Subscription userSubscription = RxUtils.invalidSubscription();
    private Subscription userUpdatedSubscription = RxUtils.invalidSubscription();
    private ProfileHeaderPresenter headerPresenter;
    private Urn user;

    private final Func1<EntityStateChangedEvent, Boolean> isProfileUser = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent entityStateChangedEvent) {
            return entityStateChangedEvent.getChangeMap().containsKey(user);
        }
    };

    @Inject
    public ProfilePresenter(ProfileScrollHelper scrollHelper,
                            ProfileHeaderPresenterFactory profileHeaderPresenterFactory,
                            UserProfileOperations profileOperations,
                            EventBus eventBus,
                            AccountOperations accountOperations,
                            EventTracker eventTracker,
                            ActivityReferringEventProvider referringEventProvider,
                            EnterScreenDispatcher enterScreenDispatcher,
                            ScreenProvider screenProvider) {
        this.scrollHelper = scrollHelper;
        this.profileHeaderPresenterFactory = profileHeaderPresenterFactory;
        this.profileOperations = profileOperations;
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.eventTracker = eventTracker;
        this.referringEventProvider = referringEventProvider;
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.screenProvider = screenProvider;
        this.enterScreenDispatcher.setListener(this);
        LightCycles.bind(this);
    }

    @Override
    public void onCreate(RootActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);

        user = getUserUrnFromIntent(activity.getIntent());

        activity.setTitle(accountOperations.isLoggedInUser(user) ? R.string.side_menu_you : R.string.side_menu_profile);

        headerPresenter = profileHeaderPresenterFactory.create(activity, user);

        pager = (ViewPager) activity.findViewById(R.id.pager);

        pager.setAdapter(new ProfilePagerAdapter(activity, user, accountOperations.isLoggedInUser(user), scrollHelper,
                                                 (SearchQuerySourceInfo) activity.getIntent()
                                                                                 .getParcelableExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)));
        pager.setCurrentItem(ProfilePagerAdapter.TAB_SOUNDS);
        pager.addOnPageChangeListener(enterScreenDispatcher);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(activity.getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_indicator);
        tabLayout.setupWithViewPager(pager);

        refreshUser();

        userUpdatedSubscription = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                                          .filter(isProfileUser)
                                          .subscribe(new RefreshUserSubscriber());
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        int position = pager.getCurrentItem();

        if (accountOperations.isLoggedInUser(user)) {
            eventTracker.trackScreen(ScreenEvent.create(getYourScreen(position)), referringEventProvider.getReferringEvent());
        } else {
            eventTracker.trackScreen(ScreenEvent.create(getOtherScreen(position), Urn.forUser(user.getNumericId())), referringEventProvider.getReferringEvent());
        }
    }

    private Screen getYourScreen(int position) {
        switch (position) {
            case TAB_INFO:
                return Screen.YOUR_INFO;
            case TAB_SOUNDS:
                return Screen.YOUR_MAIN;
            case TAB_FOLLOWINGS:
                return Screen.YOUR_FOLLOWINGS;
            case TAB_FOLLOWERS:
                return Screen.YOUR_FOLLOWERS;
            default:
                return Screen.UNKNOWN;
        }
    }

    private Screen getOtherScreen(int position) {
        switch (position) {
            case TAB_INFO:
                return Screen.USER_INFO;
            case TAB_SOUNDS:
                return Screen.USER_MAIN;
            case TAB_FOLLOWINGS:
                return Screen.USER_FOLLOWINGS;
            case TAB_FOLLOWERS:
                return Screen.USER_FOLLOWERS;
            default:
                return Screen.UNKNOWN;
        }
    }

    private Urn getUserUrnFromIntent(Intent intent) {
        if (intent.hasExtra(ProfileActivity.EXTRA_USER_URN)) {
            return intent.getParcelableExtra(ProfileActivity.EXTRA_USER_URN);
        } else if (intent.getData() != null) {
            return Urn.forUser(UriUtils.getLastSegmentAsLong(intent.getData()));
        } else {
            throw new IllegalStateException("User identifier not provided to Profile activity");
        }
    }

    private void refreshUser() {
        userSubscription.unsubscribe();
        userSubscription = profileOperations
                .getLocalProfileUser(user)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new UserDetailsSubscriber());
    }

    @Override
    public void onDestroy(RootActivity activity) {
        pager = null;
        userUpdatedSubscription.unsubscribe();
        userSubscription.unsubscribe();
        super.onDestroy(activity);
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
