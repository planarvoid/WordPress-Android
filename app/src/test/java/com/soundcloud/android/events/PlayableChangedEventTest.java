package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistUrn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayableChangedEventTest {

    @Test
    public void shouldCreateEventForLike() {
        PlaylistUrn urn = Urn.forPlaylist(123L);
        int likesCount = 5;

        PlayableChangedEvent event = PlayableChangedEvent.forLike(urn, true, likesCount);
        expect(event.getUrn()).toEqual(Urn.forPlaylist(123));
        expect(event.getChangeSet()).toEqual(
                PropertySet.from(PlayableProperty.IS_LIKED.bind(true), PlayableProperty.LIKES_COUNT.bind(5)));
    }

    @Test
    public void shouldCreateEventForRepost() {
        PlaylistUrn urn = Urn.forPlaylist(123L);
        int repostCount = 5;

        PlayableChangedEvent event = PlayableChangedEvent.forRepost(urn, true, repostCount);
        expect(event.getUrn()).toEqual(Urn.forPlaylist(123));
        expect(event.getChangeSet()).toEqual(
                PropertySet.from(PlayableProperty.IS_REPOSTED.bind(true), PlayableProperty.REPOSTS_COUNT.bind(5)));
    }
}