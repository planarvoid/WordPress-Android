package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItemRenderer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;

public class RecommendedTrackItemRendererTest extends AndroidUnitTest {

    private RecommendedTrackItemRenderer renderer;

    @Mock TrackItemRenderer trackItemRenderer;

    @Rule public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        renderer = new RecommendedTrackItemRenderer(trackItemRenderer);
    }

    @Test
    public void delegateCreateItemViewToTrackItemRenderer() {
        ViewGroup viewGroup = mock(ViewGroup.class);
        renderer.createItemView(viewGroup);

        verify(trackItemRenderer).createItemView(viewGroup);
    }

    @Test
    public void bindItemViewSetClickListener() {
        View view = mock(View.class);
        when(view.findViewById(R.id.track_list_item)).thenReturn(view);

        renderer.bindItemView(0, view, Collections.singletonList(mock(RecommendedTrackItem.class)));

        verify(view).setOnClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void mustThrowExceptionWhenSettingNullClickListener() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Click listener must not be null");

        renderer.setOnRecommendedTrackClickListener(null);
    }
}