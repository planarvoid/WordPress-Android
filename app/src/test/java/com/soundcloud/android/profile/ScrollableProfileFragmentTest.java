package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ScrollableProfileFragmentTest extends AndroidUnitTest {

    ScrollableProfileFragment fragment;

    @Mock private View view;
    @Mock private View emptyView;
    @Mock private MultiSwipeRefreshLayout swipeRefreshLayout;

    private ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(0, 0);

    @Before
    public void setUp() throws Exception {
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.str_layout)).thenReturn(swipeRefreshLayout);
        when(emptyView.getLayoutParams()).thenReturn(layoutParams);

        fragment = createFragment();
    }

    @Test
    public void setsPendingEmptyViewHeightAfterViewCreated() {
        fragment.setEmptyViewHeight(50);

        SupportFragmentTestUtil.startFragment(fragment);

        assertThat(layoutParams.height).isEqualTo(50);
        verify(emptyView).requestLayout();
    }

    @Test
    public void setsPendingSwipeToRefreshEnabledAfterViewCreated() {
        fragment.setSwipeToRefreshEnabled(true);

        SupportFragmentTestUtil.startFragment(fragment);

        verify(swipeRefreshLayout).setEnabled(true);
    }

    @NonNull
    private ScrollableProfileFragment createFragment() {
        return new ScrollableProfileFragment() {

            @Nullable
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                return view;
            }

            @Override
            public View[] getRefreshableViews() {
                return new View[0];
            }
        };
    }
}
