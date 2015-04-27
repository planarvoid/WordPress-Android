package com.soundcloud.android.playlists;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class AddTrackToPlaylistItemTest {

    @Test
    public void addTrackToPlaylistItemImplementsEqualsAndHashcode() {
        EqualsVerifier.forClass(AddTrackToPlaylistItem.class).verify();
    }

}