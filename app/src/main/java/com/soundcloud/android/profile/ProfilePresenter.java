package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileHeaderPresenter.ProfileHeaderPresenterFactory;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class ProfilePresenter extends ActivityLightCycleDispatcher<AppCompatActivity> {

    final @LightCycle ProfileScrollHelper scrollHelper;
    private final ProfileHeaderPresenterFactory profileHeaderPresenterFactory;
    private final UserProfileOperations profileOperations;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final FeatureFlags featureFlags;

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
                            FeatureFlags featureFlags) {
        this.scrollHelper = scrollHelper;
        this.profileHeaderPresenterFactory = profileHeaderPresenterFactory;
        this.profileOperations = profileOperations;
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.featureFlags = featureFlags;
        LightCycles.bind(this);
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);

        user = getUserUrnFromIntent(activity.getIntent());

        activity.setTitle(accountOperations.isLoggedInUser(user) ? R.string.side_menu_you : R.string.side_menu_profile);

        headerPresenter = profileHeaderPresenterFactory.create(activity, user);

        pager = (ViewPager) activity.findViewById(R.id.pager);

        if (featureFlags.isEnabled(Flag.FEATURE_PROFILE_NEW_TABS)) {
            pager.setAdapter(new ProfilePagerAdapter(activity, user, accountOperations.isLoggedInUser(user), scrollHelper,
                    (SearchQuerySourceInfo) activity.getIntent().getParcelableExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)));
            pager.setCurrentItem(ProfilePagerAdapter.TAB_SOUNDS);
        } else {
            pager.setAdapter(new LegacyProfilePagerAdapter(activity, user, accountOperations.isLoggedInUser(user), scrollHelper,
                    (SearchQuerySourceInfo) activity.getIntent().getParcelableExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)));
            pager.setCurrentItem(LegacyProfilePagerAdapter.TAB_POSTS);
        }

        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(activity.getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_indicator);
        tabLayout.setupWithViewPager(pager);

        refreshUser();

        userUpdatedSubscription = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(isProfileUser)
                .subscribe(new RefreshUserSubscriber());
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
