package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.collection.PlaylistsOptions;
import org.junit.Test;

public class CollectionEventTest {

    @Test
    public void createsClearEvent() {
        final CollectionEvent event = CollectionEvent.forClearFilter();

        assertThat(event.getKind()).isEqualTo(CollectionEvent.KIND_CLEAR);
    }

    @Test
    public void createsSetEventWithSort() {
        PlaylistsOptions options = PlaylistsOptions.builder()
                .sortByTitle(true)
                .build();

        final CollectionEvent event = CollectionEvent.forFilter(options);

        assertThat(event.getKind()).isEqualTo(CollectionEvent.KIND_SET);
        assertThat(event.get(CollectionEvent.KEY_TARGET)).isEqualTo(CollectionEvent.SORT_TITLE);
    }

    @Test
    public void createsSetEventWithAllFilter() {
        PlaylistsOptions options = PlaylistsOptions.builder()
                .build();

        final CollectionEvent event = CollectionEvent.forFilter(options);

        assertThat(event.getKind()).isEqualTo(CollectionEvent.KIND_SET);
        assertThat(event.get(CollectionEvent.KEY_OBJECT)).isEqualTo(CollectionEvent.FILTER_ALL);
    }

    @Test
    public void createsSetEventWithFilter() {
        PlaylistsOptions options = PlaylistsOptions.builder()
                .showLikes(true)
                .build();

        final CollectionEvent event = CollectionEvent.forFilter(options);

        assertThat(event.getKind()).isEqualTo(CollectionEvent.KIND_SET);
        assertThat(event.get(CollectionEvent.KEY_OBJECT)).isEqualTo(CollectionEvent.FILTER_LIKED);
    }

}
