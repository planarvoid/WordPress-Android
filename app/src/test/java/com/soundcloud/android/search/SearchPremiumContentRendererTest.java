package com.soundcloud.android.search;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.android.view.adapters.UserItemRenderer;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Collections;
import java.util.List;

public class SearchPremiumContentRendererTest extends AndroidUnitTest {

    private SearchPremiumContentRenderer renderer;

    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlaylistItemRenderer playlistRenderer;
    @Mock private UserItemRenderer userRenderer;

    private View premiumItemView;
    private View trackItemView;
    private View playListItemView;
    private View userItemView;


    @Before
    public void setUp() {
        final FrameLayout frameLayout = new FrameLayout(context());
        premiumItemView = LayoutInflater.from(context()).inflate(R.layout.search_premium_item, frameLayout, false);
        trackItemView = LayoutInflater.from(context()).inflate(R.layout.track_list_item, frameLayout, false);
        playListItemView = LayoutInflater.from(context()).inflate(R.layout.playlist_list_item, frameLayout, false);
        userItemView = LayoutInflater.from(context()).inflate(R.layout.user_list_item, frameLayout, false);

        when(trackRenderer.createItemView(any(ViewGroup.class))).thenReturn(trackItemView);
        when(playlistRenderer.createItemView(any(ViewGroup.class))).thenReturn(playListItemView);
        when(userRenderer.createItemView(any(ViewGroup.class))).thenReturn(userItemView);

        renderer = new SearchPremiumContentRenderer(trackRenderer, playlistRenderer, userRenderer, resources());
    }

    @Test
    public void shouldHideViewsAfterCreation() {
        renderer.createItemView(new FrameLayout(context()));

        assertThat(trackItemView.getVisibility()).isEqualTo(GONE);
        assertThat(playListItemView.getVisibility()).isEqualTo(GONE);
        assertThat(userItemView.getVisibility()).isEqualTo(GONE);

        verify(trackRenderer).createItemView(any(ViewGroup.class));
        verify(playlistRenderer).createItemView(any(ViewGroup.class));
        verify(userRenderer).createItemView(any(ViewGroup.class));
    }

    @Test
    public void shouldBindItemViewTrack() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forTrack(123L)));

        assertThat(trackItemView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(playListItemView.getVisibility()).isEqualTo(GONE);
        assertThat(userItemView.getVisibility()).isEqualTo(GONE);
        verify(trackRenderer).bindItemView(eq(0), eq(trackItemView), anyList());
    }

    @Test
    public void shouldBindItemViewPlaylist() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forPlaylist(123L)));

        assertThat(playListItemView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(trackItemView.getVisibility()).isEqualTo(GONE);
        assertThat(userItemView.getVisibility()).isEqualTo(GONE);
        verify(playlistRenderer).bindItemView(eq(0), eq(playListItemView), anyList());
    }

    @Test
    public void shouldBindItemViewUser() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forUser(123L)));

        assertThat(userItemView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(playListItemView.getVisibility()).isEqualTo(GONE);
        assertThat(trackItemView.getVisibility()).isEqualTo(GONE);
        verify(userRenderer).bindItemView(eq(0), eq(userItemView), anyList());
    }

    @Test
    public void shouldSetClickListenersToListItemViews() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forTrack(123L)));
    }

    private List<SearchPremiumItem> buildSearchPremiumItem(Urn urn) {
        final List<PropertySet> propertySets = Collections.singletonList(PropertySet.create().put(EntityProperty.URN, urn));
        return Collections.singletonList(new SearchPremiumItem(propertySets));
    }
}
