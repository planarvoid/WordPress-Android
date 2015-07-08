package com.soundcloud.android.utils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

public class CollapsingScrollHelperTest extends AndroidUnitTest {

    private CollapsingScrollHelper collapsingScrollHelper;

    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private AppBarLayout appBarLayout;
    @Mock private SwipeRefreshLayout swipeRefreshLayout;
    @Captor private ArgumentCaptor<AppBarLayout.OnOffsetChangedListener> offsetChangedCaptor;

    @Before
    public void setUp() throws Exception {
        when(view.findViewById(R.id.str_layout)).thenReturn(swipeRefreshLayout);
        when(view.findViewById(R.id.appbar)).thenReturn(appBarLayout);
        collapsingScrollHelper = new CollapsingScrollHelper();
    }

    @Test
    public void disablesSwipeToRefreshWhenOffsetLessThanZeroAfterOnViewCreated() {
        collapsingScrollHelper.onViewCreated(fragment, view, null);

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, -1);

        verify(swipeRefreshLayout).setEnabled(false);
    }

    @Test
    public void disablesSwipeToRefreshWhenOffsetEqualToZeroAfterOnViewCreated() {
        collapsingScrollHelper.onViewCreated(fragment, view, null);

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, 0);

        verify(swipeRefreshLayout).setEnabled(true);
    }

    @Test
    public void removesListenerInOnDestroyView() {
        collapsingScrollHelper.onViewCreated(fragment, view, null);
        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        collapsingScrollHelper.onDestroyView(fragment);

        verify(appBarLayout).removeOnOffsetChangedListener(offsetChangedCaptor.getValue());
    }
}