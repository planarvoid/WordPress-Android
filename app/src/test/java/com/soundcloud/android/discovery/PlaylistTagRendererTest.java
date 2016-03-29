package com.soundcloud.android.discovery;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
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
    @Mock private View itemView;

    @Before
    public void setUp() {
        renderer = new PlaylistTagRenderer(playlistTagsPresenter);
    }

    @Test
    public void rendererDisplayTagsWhenBindingItemView() {
        List<String> recentTags = Arrays.asList("#dub", "#hardcore");
        List<String> playListTags = Arrays.asList("#rock", "#metal");
        PlaylistDiscoveryItem discoveryItem = new PlaylistDiscoveryItem(playListTags, recentTags);

        renderer.bindItemView(0, itemView, Collections.singletonList(discoveryItem));

        verify(playlistTagsPresenter).displayPopularTags(itemView, playListTags);
        verify(playlistTagsPresenter).displayRecentTags(itemView, recentTags);
    }

    @Test
    public void doesNotRenderRecentTagsIfEmpty() {
        List<String> recentTags = Collections.emptyList();
        List<String> playListTags = Arrays.asList("#rock", "#metal");
        PlaylistDiscoveryItem discoveryItem = new PlaylistDiscoveryItem(playListTags, recentTags);

        renderer.bindItemView(0, itemView, Collections.singletonList(discoveryItem));

        verify(playlistTagsPresenter).displayPopularTags(itemView, playListTags);
        verify(playlistTagsPresenter, never()).displayRecentTags(same(itemView), anyList());
    }

    @Test
    public void doesNotRenderPopularTagsIfEmpty() {
        List<String> recentTags = Collections.emptyList();
        List<String> playListTags = Collections.emptyList();
        PlaylistDiscoveryItem discoveryItem = new PlaylistDiscoveryItem(playListTags, recentTags);

        renderer.bindItemView(0, itemView, Collections.singletonList(discoveryItem));

        verify(playlistTagsPresenter, never()).displayPopularTags(itemView, playListTags);
        verify(playlistTagsPresenter, never()).displayRecentTags(same(itemView), anyList());
    }

    @Test
    public void setPlayListTagsClickListener() {
        PlaylistTagsPresenter.Listener listener = mock(PlaylistTagsPresenter.Listener.class);

        renderer.setOnTagClickListener(listener);

        verify(playlistTagsPresenter).setListener(listener);
    }
}