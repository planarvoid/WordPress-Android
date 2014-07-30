package com.soundcloud.android.view.screen;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class ScreenPresenterTest {

    private ScreenPresenter presenter;

    @Mock private ActionBarActivity activity;
    @Mock private LayoutInflater inflater;
    @Mock private View layout;
    @Mock private ViewGroup container;
    @Mock private View content;


    @Before
    public void setUp() throws Exception {
        presenter = new ScreenPresenter();
        presenter.attach(activity);

        when(activity.getLayoutInflater()).thenReturn(inflater);
        when(layout.findViewById(R.id.container)).thenReturn(container);
    }

    @Test
    public void shouldRequestActionBarOverlayFeatureOnSettingLayout() {
        presenter.setBaseLayout();

        verify(activity).supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
    }

    @Test
    public void shouldSetActivityLayoutOnSetBaseLayout() {
        when(inflater.inflate(R.layout.base, null)).thenReturn(layout);

        presenter.setBaseLayout();

        verify(activity).setContentView(layout);
    }

    @Test
    public void shouldSetActivityLayoutOnSetBaseDrawerLayout() {
        when(inflater.inflate(R.layout.base_with_drawer, null)).thenReturn(layout);

        presenter.setBaseDrawerLayout();

        verify(activity).setContentView(layout);
    }

    @Test
    public void shouldAddContentToContainerOnSetBaseLayoutWithContent() {
        when(inflater.inflate(R.layout.profile_content, null)).thenReturn(content);
        when(inflater.inflate(anyInt(), any(ViewGroup.class))).thenReturn(layout);

        presenter.setBaseLayoutWithContent(R.layout.profile_content);

        verify(container).addView(layout);
    }

    @Test
    public void shouldAddContentToContainerOnSetBaseDrawerLayoutWithContent() {
        when(inflater.inflate(R.layout.profile_content, null)).thenReturn(content);
        when(inflater.inflate(anyInt(), any(ViewGroup.class))).thenReturn(layout);

        presenter.setBaseDrawerLayoutWithContent(R.layout.profile_content);

        verify(container).addView(layout);
    }
}