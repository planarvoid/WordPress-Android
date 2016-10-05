package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem.createTrackWithContext;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayQueueUIItemMapperTest extends AndroidUnitTest {
    private static final PlaybackContext EMPTY_CONTEXT = PlaybackContext.create(PlaySessionSource.EMPTY);

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(345L);
    private static final PlaybackContext PLAYLIST_CONTEXT = PlaybackContext.builder()
                                                                           .bucket(PlaybackContext.Bucket.PLAYLIST)
                                                                           .urn(Optional.of(PLAYLIST_URN))
                                                                           .query(Optional.<String>absent())
                                                                           .build();

    private static final Urn PROFILE_URN = Urn.forUser(999L);
    private static final PlaybackContext PROFILE_CONTEXT = PlaybackContext.builder()
                                                                          .bucket(PlaybackContext.Bucket.PROFILE)
                                                                          .urn(Optional.of(PROFILE_URN))
                                                                          .query(Optional.<String>absent())
                                                                          .build();

    @Mock PlayQueueManager playQueueManager;

    private PlayQueueUIItemMapper mapper;

    @Before
    public void setUp() throws Exception {
        when(playQueueManager.isShuffled()).thenReturn(false);
        mapper = new PlayQueueUIItemMapper(context(), playQueueManager);
    }

    @Test
    public void returnsEmptyWhenEmpty() {
        final List<TrackAndPlayQueueItem> noTracks = Collections.emptyList();
        final Map<Urn, String> noUrnTitles = Collections.emptyMap();

        assertThat(mapper.call(noTracks, noUrnTitles)).isEmpty();
    }

    @Test
    public void addHeaderWhenAtLeastOneTrack() {
        final List<TrackAndPlayQueueItem> aTrack = singletonList(trackAndPlayQueueItem(Urn.forTrack(123L),
                                                                                       EMPTY_CONTEXT));
        final Map<Urn, String> noUrnTitles = Collections.emptyMap();

        final List<PlayQueueUIItem> uiItems = mapper.call(aTrack, noUrnTitles);
        assertThat(uiItems).hasSize(2);

        assertThat(uiItems.get(0).isHeader()).isTrue();
        assertThat(uiItems.get(1).isTrack()).isTrue();
    }

    @Test
    public void addHeaderWithTitle() {
        final List<TrackAndPlayQueueItem> aTrack = singletonList(trackAndPlayQueueItem(Urn.forTrack(123L),
                                                                                       PLAYLIST_CONTEXT));
        final Map<Urn, String> urnTitle = singletonMap(PLAYLIST_URN, "some title");

        final List<PlayQueueUIItem> uiItems = mapper.call(aTrack, urnTitle);
        assertThat(uiItems).hasSize(2);

        final HeaderPlayQueueUIItem header = header(uiItems, 0);
        assertThat(header.getContentTitle()).isEqualTo(Optional.of("some title"));
    }

    @Test
    public void addHeaderWithTitleWhenSameContext() {
        final TrackAndPlayQueueItem track1 = trackAndPlayQueueItem(Urn.forTrack(123L), PLAYLIST_CONTEXT);
        final TrackAndPlayQueueItem track2 = trackAndPlayQueueItem(Urn.forTrack(789L), PLAYLIST_CONTEXT);
        final List<TrackAndPlayQueueItem> tracks = asList(track1, track2);

        final Map<Urn, String> urnTitle = singletonMap(PLAYLIST_URN, "some title");

        final List<PlayQueueUIItem> uiItems = mapper.call(tracks, urnTitle);

        assertThat(uiItems).hasSize(3);
        assertThat(uiItems.get(0).isHeader()).isTrue();
        assertThat((track(uiItems, 1)).getTrackItem().getUrn()).isEqualTo(Urn.forTrack(123L));
        assertThat((track(uiItems, 2)).getTrackItem().getUrn()).isEqualTo(Urn.forTrack(789L));
    }

    @Test
    public void addMultipleHeadersWithTitleWhenMultipleContext() {
        final TrackAndPlayQueueItem track1 = trackAndPlayQueueItem(Urn.forTrack(123L), PLAYLIST_CONTEXT);
        final TrackAndPlayQueueItem track2 = trackAndPlayQueueItem(Urn.forTrack(789L), PROFILE_CONTEXT);
        final List<TrackAndPlayQueueItem> tracks = asList(track1, track2);

        final Map<Urn, String> urnTitle = new HashMap<>();
        urnTitle.put(PLAYLIST_URN, "some playlist");
        urnTitle.put(PROFILE_URN, "some profile");

        final List<PlayQueueUIItem> uiItems = mapper.call(tracks, urnTitle);

        assertThat(uiItems).hasSize(4);
        assertThat(header(uiItems, 0).getContentTitle()).isEqualTo(Optional.of("some playlist"));
        assertThat(track(uiItems, 1).getTrackItem().getUrn()).isEqualTo(Urn.forTrack(123L));
        assertThat((header(uiItems, 2)).getContentTitle()).isEqualTo(Optional.of("some profile"));
        assertThat((track(uiItems, 3)).getTrackItem().getUrn()).isEqualTo(Urn.forTrack(789L));
    }

    @Test
    public void doesNotAddHeadersWhenShuffled() {
        when(playQueueManager.isShuffled()).thenReturn(true);
        final List<TrackAndPlayQueueItem> aTrack = singletonList(trackAndPlayQueueItem(Urn.forTrack(123L),
                                                                                       EMPTY_CONTEXT));
        final Map<Urn, String> noUrnTitles = Collections.emptyMap();

        final List<PlayQueueUIItem> uiItems = mapper.call(aTrack, noUrnTitles);

        assertThat(uiItems).hasSize(1);
        assertThat(uiItems.get(0).isTrack()).isTrue();
    }

    private static TrackPlayQueueUIItem track(List<PlayQueueUIItem> uiItems, int index) {
        return (TrackPlayQueueUIItem) uiItems.get(index);
    }

    private static HeaderPlayQueueUIItem header(List<PlayQueueUIItem> uiItems, int index) {
        return (HeaderPlayQueueUIItem) uiItems.get(index);
    }

    private static TrackAndPlayQueueItem trackAndPlayQueueItem(Urn track, PlaybackContext playbackContext) {
        return new TrackAndPlayQueueItem(trackItem(track), createTrackWithContext(track, playbackContext));
    }

    private static TrackItem trackItem(Urn track) {
        return new TrackItem(TestPropertySets.expectedTrackForListItem(track));
    }
}
