package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;

public class ProfilePresenterTest extends AndroidUnitTest {

    private static final int DIVIDER_WIDTH = 55;
    private static final Urn USER_URN = Urn.forUser(123L);

    private ProfilePresenter profilePresenter;

    @Mock private RootActivity activity;
    @Mock private TabLayout tabLayout;
    @Mock private MultiSwipeRefreshLayout swipeRefreshLayout;
    @Mock private ProfileHeaderPresenter profileHeaderPresenter;
    @Mock private UserProfileOperations profileOperations;
    @Mock private View headerView;
    @Mock private ViewPager viewPager;
    @Mock private Resources resources;
    @Mock private FragmentManager fragmentManager;
    @Mock private ProfileScrollHelper profileScrollHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private EventTracker eventTracker;
    @Mock private Optional<ReferringEvent> referringEvent;
    @Mock private EnterScreenDispatcher enterScreenDispatcher;
    @Mock private ScreenProvider screenProvider;
    @Mock private ProfileConfig profileConfig;
    @Captor private ArgumentCaptor<ScreenEvent> screenEventArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();

    private UserItem profileUser;
    private Intent intent = new Intent();

    @Before
    public void setUp() throws Exception {
        profileUser = createProfileUser();

        Urns.writeToIntent(intent, ProfileActivity.EXTRA_USER_URN, USER_URN);

        when(screenProvider.getLastScreen()).thenReturn(Screen.USER_MAIN);
        when(activity.getIntent()).thenReturn(intent);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(activity.getResources()).thenReturn(resources);
        when(activity.findViewById(R.id.tab_indicator_fixed)).thenReturn(tabLayout);
        when(activity.findViewById(R.id.pager)).thenReturn(viewPager);
        when(activity.findViewById(R.id.str_layout)).thenReturn(swipeRefreshLayout);
        when(activity.findViewById(R.id.profile_header)).thenReturn(headerView);
        when(activity.getReferringEvent()).thenReturn(referringEvent);
        when(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width)).thenReturn(DIVIDER_WIDTH);
        when(profileOperations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(profileUser));

        profilePresenter = new ProfilePresenter(profileScrollHelper,
                                                profileConfig,
                                                profileHeaderPresenter,
                                                profileOperations,
                                                eventBus,
                                                accountOperations,
                                                eventTracker,
                                                enterScreenDispatcher
        );
    }

    @Test
    public void configuresViewPagerDividers() throws Exception {
        profilePresenter.onCreate(activity, null);

        verify(viewPager).setPageMarginDrawable(R.drawable.divider_vertical_grey);
        verify(viewPager).setPageMargin(DIVIDER_WIDTH);
    }

    @Test
    public void setsUserOnHeaderPresenter() throws Exception {
        profilePresenter.onCreate(activity, null);

        verify(profileHeaderPresenter).setUserDetails(profileUser);
    }

    @Test
    public void setTitleIfProfileBanner() throws Exception {
        when(profileConfig.showProfileBanner()).thenReturn(true);

        profilePresenter.onCreate(activity, null);

        verify(activity).setTitle(R.string.side_menu_profile);
        verify(activity).setTitle(profileUser.user().username());
    }

    @Test
    public void keepTitleIfNotProfileBanner() throws Exception {
        when(profileConfig.showProfileBanner()).thenReturn(false);

        profilePresenter.onCreate(activity, null);

        verify(activity).setTitle(R.string.side_menu_profile);
    }

    @Test
    public void setsUserOnHeaderPresenterWhenIntentProvidesUri() throws Exception {
        intent.removeExtra(ProfileActivity.EXTRA_USER_URN);
        intent.setData(Uri.parse("soundcloud://users/" + USER_URN.getNumericId()));

        profilePresenter.onCreate(activity, null);

        verify(profileHeaderPresenter).setUserDetails(profileUser);
    }

    @Test
    public void entityStateChangedEventReloadsUserOnHeaderPresenter() throws Exception {
        profilePresenter.onCreate(activity, null);

        final UserItem updatedProfileUser = UserFixtures.userItem(UserFixtures.userBuilder().username("updated-name").build());
        when(profileOperations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(updatedProfileUser));

        final User user = UserFixtures.userBuilder().urn(USER_URN).build();
        eventBus.publish(EventQueue.USER_CHANGED,
                         UserChangedEvent.forUpdate(user));

        verify(profileHeaderPresenter).setUserDetails(updatedProfileUser);
    }

    @Test
    public void entityStateChangedEventForOtherUserDoesntReloadUser() throws Exception {
        profilePresenter.onCreate(activity, null);
        Mockito.reset(profileOperations);

        final User user = UserFixtures.userBuilder().urn(Urn.forUser(444)).build();
        eventBus.publish(EventQueue.USER_CHANGED, UserChangedEvent.forUpdate(user));

        verifyZeroInteractions(profileOperations);
    }

    private UserItem createProfileUser() {
        return UserFixtures.userItem();
    }
}
