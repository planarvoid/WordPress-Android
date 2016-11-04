package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ActivityReferringEventProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.ScreenStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.collections.PropertySet;
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

import java.util.Collections;

public class ProfilePresenterTest extends AndroidUnitTest {

    private static final int DIVIDER_WIDTH = 55;
    private static final Urn USER_URN = Urn.forUser(123L);

    private ProfilePresenter profilePresenter;

    @Mock private ImageOperations imageOperations;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
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
    @Mock private TrackingStateProvider trackingStateProvider;
    @Mock private EventTracker eventTracker;
    @Mock private ScreenStateProvider screenStateProvider;
    @Mock private ActivityReferringEventProvider referringEventProvider;
    @Mock private EnterScreenDispatcher enterScreenDispatcher;
    @Mock private ScreenProvider screenProvider;
    @Captor private ArgumentCaptor<ViewPager.OnPageChangeListener> onPageChangeListenerCaptor;
    @Captor private ArgumentCaptor<ScreenEvent> screenEventArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();

    private ProfileUser profileUser;
    private Intent intent = new Intent();

    @Before
    public void setUp() throws Exception {
        profileUser = createProfileUser();

        intent.putExtra(ProfileActivity.EXTRA_USER_URN, USER_URN);

        when(screenProvider.getLastScreen()).thenReturn(Screen.USER_MAIN);
        when(activity.getIntent()).thenReturn(intent);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(activity.getResources()).thenReturn(resources);
        when(activity.findViewById(R.id.tab_indicator)).thenReturn(tabLayout);
        when(activity.findViewById(R.id.pager)).thenReturn(viewPager);
        when(activity.findViewById(R.id.str_layout)).thenReturn(swipeRefreshLayout);
        when(activity.findViewById(R.id.profile_header)).thenReturn(headerView);
        when(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width)).thenReturn(DIVIDER_WIDTH);
        when(profileOperations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(profileUser));

        profilePresenter = new ProfilePresenter(profileScrollHelper,
                                                profileHeaderPresenter,
                                                profileOperations,
                                                eventBus,
                                                accountOperations,
                                                eventTracker,
                                                referringEventProvider,
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
    public void setsUserOnHeaderPresenterWhenIntentProvidesUri() throws Exception {
        intent.removeExtra(ProfileActivity.EXTRA_USER_URN);
        intent.setData(Uri.parse("soundcloud://users/" + USER_URN.getNumericId()));

        profilePresenter.onCreate(activity, null);

        verify(profileHeaderPresenter).setUserDetails(profileUser);
    }

    @Test
    public void entityStateChangedEventReloadsUserOnHeaderPresenter() throws Exception {
        profilePresenter.onCreate(activity, null);

        final ProfileUser updatedProfileUser = createProfileUser();
        when(profileOperations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(updatedProfileUser));

        final PropertySet userProperties = PropertySet.from(UserProperty.URN.bind(USER_URN));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromLike(Collections.singletonList(userProperties)));

        verify(profileHeaderPresenter).setUserDetails(updatedProfileUser);
    }

    @Test
    public void entityStateChangedEventForOtherUserDoesntReloadUser() throws Exception {
        profilePresenter.onCreate(activity, null);
        Mockito.reset(profileOperations);

        final PropertySet userProperties = PropertySet.from(UserProperty.URN.bind(Urn.forUser(444)));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromLike(Collections.singletonList(userProperties)));

        verifyZeroInteractions(profileOperations);
    }

    @Test
    public void profilePresenterShouldTrackUserPageViewWhenTabSelected() throws Exception {
        profilePresenter.onCreate(activity, null);
        when(viewPager.getCurrentItem()).thenReturn(ProfilePagerAdapter.TAB_SOUNDS);
        profilePresenter.onEnterScreen(activity);

        verify(eventTracker).trackScreen(screenEventArgumentCaptor.capture(), any(Optional.class));
        assertThat(screenEventArgumentCaptor.getValue().getScreenTag()).isEqualTo(Screen.USER_MAIN.get());
        assertThat(screenEventArgumentCaptor.getValue().getPageUrn()).isEqualTo(USER_URN.toString());
    }

    @Test
    public void profilePresenterShouldTrackYouPageViewWhenTabSelectedOnYourOwnProfile() throws Exception {
        when(accountOperations.isLoggedInUser(USER_URN)).thenReturn(true);

        profilePresenter.onCreate(activity, null);
        when(viewPager.getCurrentItem()).thenReturn(ProfilePagerAdapter.TAB_SOUNDS);
        profilePresenter.onEnterScreen(activity);

        verify(eventTracker).trackScreen(screenEventArgumentCaptor.capture(), any(Optional.class));
        assertThat(screenEventArgumentCaptor.getValue().getScreenTag()).isEqualTo(Screen.YOUR_MAIN.get());
    }

    private ProfileUser createProfileUser() {
        return new ProfileUser(ModelFixtures.create(ApiUser.class).toPropertySet());
    }
}
