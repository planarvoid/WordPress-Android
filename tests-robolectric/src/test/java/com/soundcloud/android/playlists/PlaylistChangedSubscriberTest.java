package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.activities.ActivitiesAdapter;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.api.legacy.model.activities.PlaylistActivity;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistChangedSubscriberTest {

    PlaylistChangedSubscriber receiver;

    @Test
    public void shouldHandlePlaylistActivityChange() throws IOException {
        PublicApiPlaylist playlist = new PublicApiPlaylist(123L);
        playlist.setTrackCount(10);

        PlaylistActivity playlistActivity = new PlaylistActivity();
        playlistActivity.playlist = playlist;

        ActivitiesAdapter baseAdapter = new ActivitiesAdapter(Content.ME_SOUND_STREAM.uri);
        receiver = new PlaylistChangedSubscriber(baseAdapter);
        baseAdapter.addItems(Arrays.<Activity>asList(playlistActivity));

        receiver.onNext(createPlaylistChangedEvent());

        expect(playlist.getTrackCount()).toEqual(30);
    }

    private EntityStateChangedEvent createPlaylistChangedEvent() {
        PropertySet propertySet = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forPlaylist(123L)),
                PlaylistProperty.TRACK_COUNT.bind(30)
        );
        return EntityStateChangedEvent.fromTrackAddedToPlaylist(propertySet);
    }
}
