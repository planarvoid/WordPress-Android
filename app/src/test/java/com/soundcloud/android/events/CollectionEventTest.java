package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class CollectionEventTest {

    @Test
    public void createsClearEvent() {
        final CollectionEvent event = CollectionEvent.forClearFilter();

        assertThat(event.clickName()).isEqualTo(CollectionEvent.ClickName.CLEAR);
    }

    @Test
    public void createsSetEventWithSort() {
        PlaylistsOptions options = PlaylistsOptions.builder()
                                                   .sortByTitle(true)
                                                   .build();

        final CollectionEvent event = CollectionEvent.forFilter(options);

        assertThat(event.clickName()).isEqualTo(CollectionEvent.ClickName.SET);
        assertThat(event.target().get()).isEqualTo(CollectionEvent.Target.SORT_TITLE);
    }

    @Test
    public void createsSetEventWithAllFilter() {
        PlaylistsOptions options = PlaylistsOptions.builder()
                                                   .build();

        final CollectionEvent event = CollectionEvent.forFilter(options);

        assertThat(event.clickName()).isEqualTo(CollectionEvent.ClickName.SET);
        assertThat(event.object().get()).isEqualTo(CollectionEvent.FilterTag.ALL.toString());
    }

    @Test
    public void createsSetEventWithFilter() {
        PlaylistsOptions options = PlaylistsOptions.builder()
                                                   .showLikes(true)
                                                   .build();

        final CollectionEvent event = CollectionEvent.forFilter(options);

        assertThat(event.clickName()).isEqualTo(CollectionEvent.ClickName.SET);
        assertThat(event.object().get()).isEqualTo(CollectionEvent.FilterTag.LIKED.toString());
    }

    @Test
    public void createsRecentlyPlayedNavigationEvent() {
        Urn urn = Urn.forPlaylist(123);
        final CollectionEvent event = CollectionEvent.forRecentlyPlayed(urn, Screen.COLLECTIONS);

        assertThat(event.clickName()).isEqualTo(CollectionEvent.ClickName.ITEM_NAVIGATION);
        assertThat(event.object().get()).isEqualTo(urn.toString());
        assertThat(event.pageName()).isEqualTo(Screen.COLLECTIONS.get());
    }
}
