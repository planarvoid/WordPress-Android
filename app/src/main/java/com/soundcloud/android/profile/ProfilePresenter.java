package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileModule.PROFILE_SCROLL_HELPER;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.User;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Named;

class ProfilePresenter extends ActivityLightCycleDispatcher<RootActivity>
        implements EnterScreenDispatcher.Listener {

    final @LightCycle ProfileScrollHelper scrollHelper;
    final @LightCycle EnterScreenDispatcher enterScreenDispatcher;
    final @LightCycle ProfileHeaderPresenter headerPresenter;
    private final UserProfileOperations profileOperations;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final EventTracker eventTracker;
    private final FeatureFlags featureFlags;

    private ViewPager pager;
    private UserProfilePagerAdapter adapter;
    private Subscription userSubscription = RxUtils.invalidSubscription();
    private Subscription userUpdatedSubscription = RxUtils.invalidSubscription();

    private Urn user;

    @Inject
    ProfilePresenter(@Named(PROFILE_SCROLL_HELPER) ProfileScrollHelper scrollHelper,
                     ProfileHeaderPresenter profileHeaderPresenter,
                     UserProfileOperations profileOperations,
                     EventBus eventBus,
                     AccountOperations accountOperations,
                     EventTracker eventTracker,
                     EnterScreenDispatcher enterScreenDispatcher,
                     FeatureFlags featureFlags) {
        this.scrollHelper = scrollHelper;
        this.headerPresenter = profileHeaderPresenter;
        this.profileOperations = profileOperations;
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.eventTracker = eventTracker;
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.enterScreenDispatcher.setListener(this);
        this.featureFlags = featureFlags;
    }

    @Override
    public void onCreate(RootActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);

        user = ProfileActivity.getUserUrnFromIntent(activity.getIntent());

        activity.setTitle(accountOperations.isLoggedInUser(user) ? R.string.side_menu_you : R.string.side_menu_profile);

        pager = (ViewPager) activity.findViewById(R.id.pager);

        boolean alignedProfile = featureFlags.isEnabled(Flag.ALIGNED_USER_INFO);

        if (alignedProfile) {
            adapter = new ProfilePagerAdapter(activity,
                                              user,
                                              accountOperations.isLoggedInUser(user),
                                              activity.getIntent()
                                                             .getParcelableExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO));
            pager.setAdapter(adapter);
            pager.setCurrentItem(ProfilePagerAdapter.TAB_SOUNDS);
        } else {
            adapter = new OldProfilePagerAdapter(activity,
                                                 user,
                                                 accountOperations.isLoggedInUser(user),
                                                 scrollHelper,
                                                 activity.getIntent()
                                                                 .getParcelableExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO));
            pager.setAdapter(adapter);
            pager.setCurrentItem(OldProfilePagerAdapter.TAB_SOUNDS);
        }

        pager.addOnPageChangeListener(enterScreenDispatcher);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(activity.getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabLayout tabLayout = (TabLayout) activity.findViewById(alignedProfile ? R.id.tab_indicator_fixed : R.id.tab_indicator_scrollable);
        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.setupWithViewPager(pager);

        refreshUser();

        userUpdatedSubscription = new CompositeSubscription(eventBus.queue(EventQueue.USER_CHANGED)
                                                                    .filter(entityStateChangedEvent -> entityStateChangedEvent.changeMap().containsKey(user))
                                                                    .subscribe(event -> refreshUser()),
                                                            eventBus.queue(EventQueue.FOLLOWING_CHANGED)
                                                                    .filter(event -> event.urn().equals(user))
                                                                    .subscribe(event -> refreshUser()));
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        int position = pager.getCurrentItem();

        if (accountOperations.isLoggedInUser(user)) {
            eventTracker.trackScreen(ScreenEvent.create(adapter.getYourScreen(position)), activity.getReferringEvent());
        } else {
            eventTracker.trackScreen(ScreenEvent.create(adapter.getOtherScreen(position), Urn.forUser(user.getNumericId())), activity.getReferringEvent());
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

    private final class UserDetailsSubscriber extends DefaultSubscriber<User> {
        @Override
        public void onNext(User profileUser) {
            headerPresenter.setUserDetails(profileUser);
        }
    }

}
