package com.soundcloud.android.view.screen;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.AppNavigationExperiment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@RunWith(MockitoJUnitRunner.class)
public class BaseLayoutHelperTest {

    private BaseLayoutHelper helper;

    @Mock private AppCompatActivity activity;
    @Mock private LayoutInflater inflater;
    @Mock private View layout;
    @Mock private ViewGroup container;
    @Mock private View content;
    @Mock private AppNavigationExperiment appNavigationExperiment;
    @Mock private TelescopeLayoutWrapper telescopeLayoutWrapper;

    @Before
    public void setUp() throws Exception {
        helper = new BaseLayoutHelper(appNavigationExperiment, telescopeLayoutWrapper);

        when(appNavigationExperiment.isBottomNavigationEnabled()).thenReturn(false);
        when(activity.getLayoutInflater()).thenReturn(inflater);
        when(layout.findViewById(R.id.container)).thenReturn(container);
        when(telescopeLayoutWrapper.wrapLayoutIfNecessary(activity, layout)).thenReturn(layout);
    }

    @Test
    public void shouldSetActivityLayoutOnSetBaseLayout() {
        when(inflater.inflate(R.layout.base, null)).thenReturn(layout);

        helper.setBaseLayout(activity);

        verify(activity).setContentView(layout);
    }

    @Test
    public void shouldSetActivityLayoutOnSetBaseLayoutWithMargins() {
        when(inflater.inflate(R.layout.base_with_margins, null)).thenReturn(layout);

        helper.setBaseLayoutWithMargins(activity);

        verify(activity).setContentView(layout);
    }

    @Test
    public void shouldSetToolbarAsActionBarForBaseLayout() {
        Toolbar toolbar = mock(Toolbar.class);
        when(inflater.inflate(R.layout.base, null)).thenReturn(layout);
        when(activity.findViewById(R.id.toolbar_id)).thenReturn(toolbar);

        helper.setBaseLayout(activity);

        verify(activity).setSupportActionBar(toolbar);
    }

    @Test
    public void shouldNotSetToolbarAsActionBarForTabLayout() {
        when(inflater.inflate(R.layout.main_with_tabs, null)).thenReturn(layout);

        helper.setMainLayout(activity);

        verify(activity, never()).setSupportActionBar(any(Toolbar.class));
    }

    @Test
    public void shouldAddContentToContainerOnSetBaseLayoutWithContent() {
        when(inflater.inflate(R.layout.base, null)).thenReturn(layout);
        when(inflater.inflate(R.layout.profile, null)).thenReturn(content);

        helper.setBaseLayoutWithContent(activity, R.layout.profile);

        verify(container).addView(content);
    }

}
