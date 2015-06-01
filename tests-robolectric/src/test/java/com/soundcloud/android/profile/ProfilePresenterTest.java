package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfilePagerRefreshHelper.ProfilePagerRefreshHelperFactory;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.SlidingTabLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ProfilePresenterTest {

    private static final int DIVIDER_WIDTH = 55;

    private ProfilePresenter profilePresenter;

    @Mock private ImageOperations imageOperations;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;
    @Mock private AppCompatActivity activity;
    @Mock private SlidingTabLayout slidingTabLayout;
    @Mock private MultiSwipeRefreshLayout swipeRefreshLayout;
    @Mock private ProfilePagerRefreshHelperFactory profilePagerRefreshHelperFactory;
    @Mock private ProfilePagerRefreshHelper profilePagerRefreshHelper;
    @Mock private ProfilePagerAdapter profilePagerAdapter;
    @Mock private ProfileHeaderPresenter.ProfileHeaderPresenterFactory profileHeaderPresenterFactory;
    @Mock private ProfileHeaderPresenter profileHeaderPresenter;
    @Mock private View headerView;
    @Mock private ViewPager viewPager;
    @Mock private Resources resources;
    @Mock private FragmentManager fragmentManager;
    @Captor private ArgumentCaptor<ViewPager.OnPageChangeListener> onPageChangeListenerCaptor;

    @Before
    public void setUp() throws Exception {
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(activity.getResources()).thenReturn(resources);
        when(activity.findViewById(R.id.sliding_tabs)).thenReturn(slidingTabLayout);
        when(activity.findViewById(R.id.pager)).thenReturn(viewPager);
        when(activity.findViewById(R.id.str_layout)).thenReturn(swipeRefreshLayout);
        when(activity.findViewById(R.id.profile_header)).thenReturn(headerView);
        when(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width)).thenReturn(DIVIDER_WIDTH);
        when(profileHeaderPresenterFactory.create(headerView)).thenReturn(profileHeaderPresenter);
        when(profilePagerRefreshHelperFactory.create(swipeRefreshLayout)).thenReturn(profilePagerRefreshHelper);
        profilePresenter = new ProfilePresenter(profilePagerRefreshHelperFactory, profileHeaderPresenterFactory);
    }

    @Test
    public void configuresViewPagerDividers() throws Exception {
        profilePresenter.onCreate(activity, null);

        verify(viewPager).setPageMarginDrawable(R.drawable.divider_vertical_grey);
        verify(viewPager).setPageMargin(DIVIDER_WIDTH);
    }

    @Test
    public void setsInitialRefreshPage() throws Exception {
        profilePresenter.onCreate(activity, null);

        verify(profilePagerRefreshHelper).setRefreshablePage(0);
    }

    @Test
    public void attachesRefreshWhenPageChanges() throws Exception {
        profilePresenter.onCreate(activity, null);

        verify(slidingTabLayout).setOnPageChangeListener(onPageChangeListenerCaptor.capture());
        onPageChangeListenerCaptor.getValue().onPageSelected(2);

        verify(profilePagerRefreshHelper).setRefreshablePage(2);
    }
}