package com.soundcloud.android.discovery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.search.PlaylistTagsPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistTagRendererTest {

    private PlaylistTagRenderer renderer;

    @Mock PlaylistTagsPresenter playlistTagsPresenter;

    @Before
    public void setUp() {
        renderer = new PlaylistTagRenderer(playlistTagsPresenter);
    }

    @Test
    public void rendererDisplayTagsWhenBindingItemView() {
        View itemView = mock(View.class);
        PlaylistDiscoveryItem discoveryItem = mock(PlaylistDiscoveryItem.class);
        List<String> playListTags = Arrays.asList("#rock", "#metal");
        when(discoveryItem.getPopularTags()).thenReturn(playListTags);
        when(discoveryItem.getRecentTags()).thenReturn(playListTags);

        renderer.bindItemView(0, itemView, Collections.singletonList(discoveryItem));

        verify(discoveryItem).getPopularTags();
        verify(discoveryItem).getPopularTags();
        verify(playlistTagsPresenter).displayRecentTags(itemView, playListTags);
    }

    @Test
    public void setPlayListTagsClickListener() {
        PlaylistTagsPresenter.Listener listener = mock(PlaylistTagsPresenter.Listener.class);

        renderer.setOnTagClickListener(listener);

        verify(playlistTagsPresenter).setListener(listener);
    }
}