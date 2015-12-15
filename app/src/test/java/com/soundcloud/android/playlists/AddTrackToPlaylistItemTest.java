package com.soundcloud.android.playlists;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class AddTrackToPlaylistItemTest {

    @Test
    public void addTrackToPlaylistItemImplementsEqualsAndHashcode() {
        EqualsVerifier.forClass(AddTrackToPlaylistItem.class).verify();
    }

}
