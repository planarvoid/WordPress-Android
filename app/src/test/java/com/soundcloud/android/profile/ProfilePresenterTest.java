package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.ProfileScrollHelper;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.collections.PropertySet;
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
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class ProfilePresenterTest extends AndroidUnitTest {

    private static final int DIVIDER_WIDTH = 55;
    private static final Urn USER_URN = Urn.forUser(123L);

    private ProfilePresenter profilePresenter;

    @Mock private ImageOperations imageOperations;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private AppCompatActivity activity;
    @Mock private TabLayout tabLayout;
    @Mock private MultiSwipeRefreshLayout swipeRefreshLayout;
    @Mock private ProfileHeaderPresenter.ProfileHeaderPresenterFactory profileHeaderPresenterFactory;
    @Mock private ProfileHeaderPresenter profileHeaderPresenter;
    @Mock private UserProfileOperations profileOperations;
    @Mock private View headerView;
    @Mock private ViewPager viewPager;
    @Mock private Resources resources;
    @Mock private FragmentManager fragmentManager;
    @Mock private ProfileScrollHelper profileScrollHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureFlags featureFlags;
    @Captor private ArgumentCaptor<ViewPager.OnPageChangeListener> onPageChangeListenerCaptor;

    private TestEventBus eventBus = new TestEventBus();

    private ProfileUser profileUser;
    private Intent intent = new Intent();

    @Before
    public void setUp() throws Exception {
        profileUser = createProfileUser();

        intent.putExtra(ProfileActivity.EXTRA_USER_URN, USER_URN);

        when(activity.getIntent()).thenReturn(intent);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(activity.getResources()).thenReturn(resources);
        when(activity.findViewById(R.id.tab_indicator)).thenReturn(tabLayout);
        when(activity.findViewById(R.id.pager)).thenReturn(viewPager);
        when(activity.findViewById(R.id.str_layout)).thenReturn(swipeRefreshLayout);
        when(activity.findViewById(R.id.profile_header)).thenReturn(headerView);
        when(featureFlags.isEnabled(Flag.FEATURE_PROFILE_NEW_TABS)).thenReturn(false);
        when(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width)).thenReturn(DIVIDER_WIDTH);
        when(profileHeaderPresenterFactory.create(activity, USER_URN)).thenReturn(profileHeaderPresenter);
        when(profileOperations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(profileUser));

        profilePresenter = new ProfilePresenter(profileScrollHelper, profileHeaderPresenterFactory,
                profileOperations, eventBus, accountOperations, featureFlags);
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
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(userProperties));

        verify(profileHeaderPresenter).setUserDetails(updatedProfileUser);
    }

    @Test
    public void entityStateChangedEventForOtherUserDoesntReloadUser() throws Exception {
        profilePresenter.onCreate(activity, null);
        Mockito.reset(profileOperations);

        final PropertySet userProperties = PropertySet.from(UserProperty.URN.bind(Urn.forUser(444)));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(userProperties));

        verifyZeroInteractions(profileOperations);
    }

    private ProfileUser createProfileUser() {
        return new ProfileUser(ModelFixtures.create(ApiUser.class).toPropertySet());
    }
}
