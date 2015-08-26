package com.soundcloud.android.utils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.profile.ProfileScreen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class ProfileScrollHelperTest extends AndroidUnitTest {

    private static final int VIEW_PAGER_HEIGHT = 100;
    private static final int APP_BAR_TOTAL_RANGE = 40;

    private ProfileScrollHelper profileScrollHelper;

    @Mock private AppCompatActivity activity;
    @Mock private AppBarLayout appBarLayout;
    @Mock private ViewPager viewPager;

    @Mock private ProfileScreen profileScreen1;
    @Mock private ProfileScreen profileScreen2;

    @Captor private ArgumentCaptor<AppBarLayout.OnOffsetChangedListener> offsetChangedCaptor;

    @Before
    public void setUp() throws Exception {
        when(activity.findViewById(R.id.appbar)).thenReturn(appBarLayout);
        when(activity.findViewById(R.id.pager)).thenReturn(viewPager);
        profileScrollHelper = new ProfileScrollHelper();

        when(viewPager.getHeight()).thenReturn(VIEW_PAGER_HEIGHT);
        when(appBarLayout.getTotalScrollRange()).thenReturn(APP_BAR_TOTAL_RANGE);
    }

    @Test
    public void disablesSwipeToRefreshWhenOffsetLessThanZeroAfterOnStart() {
        profileScrollHelper.onStart(activity);
        profileScrollHelper.addProfileCollection(profileScreen1);
        profileScrollHelper.addProfileCollection(profileScreen2);

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, -1);

        verify(profileScreen1).setSwipeToRefreshEnabled(false);
        verify(profileScreen2).setSwipeToRefreshEnabled(false);
    }

    @Test
    public void disablesSwipeToRefreshWhenOffsetLessThanZeroAfterRemovingRefreshableScreen() {
        profileScrollHelper.onStart(activity);
        profileScrollHelper.addProfileCollection(profileScreen1);
        profileScrollHelper.addProfileCollection(profileScreen2);
        profileScrollHelper.removeProfileScreen(profileScreen2);

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, -1);

        verify(profileScreen1).setSwipeToRefreshEnabled(false);
        verify(profileScreen2, never()).setSwipeToRefreshEnabled(false);
    }

    @Test
    public void enablesSwipeToRefreshWhenOffsetEqualToZeroAfterOnStart() {
        profileScrollHelper.onStart(activity);
        profileScrollHelper.addProfileCollection(profileScreen1);
        profileScrollHelper.addProfileCollection(profileScreen2);

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, 0);

        verify(profileScreen1, times(2)).setSwipeToRefreshEnabled(true);
        verify(profileScreen2, times(2)).setSwipeToRefreshEnabled(true);
    }

    @Test
    public void setsEmptyViewsToBeAvailableHeight() {
        profileScrollHelper.onStart(activity);
        profileScrollHelper.addProfileCollection(profileScreen1);
        profileScrollHelper.addProfileCollection(profileScreen2);

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        final int offset = -23;
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, offset);

        final int availableHeight = VIEW_PAGER_HEIGHT - APP_BAR_TOTAL_RANGE - offset;
        verify(profileScreen1).setEmptyViewHeight(availableHeight);
        verify(profileScreen2).setEmptyViewHeight(availableHeight);
    }

    @Test
    public void removesListenerInOnStop() {
        profileScrollHelper.onStart(activity);
        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        profileScrollHelper.onStop(activity);

        verify(appBarLayout).removeOnOffsetChangedListener(offsetChangedCaptor.getValue());
    }
}