package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayableChangedEventTest {

    @Test
    public void shouldCreateEventForLike() {
        Playable playable = new Playlist(123);
        playable.likes_count = 5;

        PlayableChangedEvent event = PlayableChangedEvent.forLike(playable, true);
        expect(event.getUrn()).toEqual(Urn.forPlaylist(123));
        expect(event.getChangeSet()).toEqual(
                PropertySet.from(PlayableProperty.IS_LIKED.bind(true), PlayableProperty.LIKES_COUNT.bind(5)));
    }

    @Test
    public void shouldCreateEventForRepost() {
        Playable playable = new Playlist(123);
        playable.reposts_count = 5;

        PlayableChangedEvent event = PlayableChangedEvent.forRepost(playable, true);
        expect(event.getUrn()).toEqual(Urn.forPlaylist(123));
        expect(event.getChangeSet()).toEqual(
                PropertySet.from(PlayableProperty.IS_REPOSTED.bind(true), PlayableProperty.REPOSTS_COUNT.bind(5)));
    }
}