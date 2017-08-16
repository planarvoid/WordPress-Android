package com.soundcloud.android.playback;

import static com.soundcloud.android.model.Urn.forPlaylist;
import static com.soundcloud.android.model.Urn.forTrack;
import static com.soundcloud.android.model.Urn.forUser;
import static com.soundcloud.android.playback.PlaybackContext.Bucket.LISTENING_HISTORY;
import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static com.soundcloud.java.optional.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.optional.Optional;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayQueueStorageTest extends StorageIntegrationTest {

    private static final Urn RELATED_ENTITY = Urn.forTrack(987L);

    private static final PlaybackContext PLAYBACK_CONTEXT = PlaybackContext.builder()
                                                                           .bucket(LISTENING_HISTORY)
                                                                           .query(of("some filter"))
                                                                           .urn(of(Urn.forPlaylist(321L)))
                                                                           .build();

    private static final PlaybackContext BUCKET_ONLY_PLAYBACK_CONTEXT = PlaybackContext.builder()
                                                                                       .bucket(LISTENING_HISTORY)
                                                                                       .query(Optional.absent())
                                                                                       .urn(Optional.absent())
                                                                                       .build();

    private PlayQueueStorage storage;

    @Before
    public void setUp() throws Exception {
        PlayQueueDatabaseOpenHelper databaseOpenHelper = new PlayQueueDatabaseOpenHelper(RuntimeEnvironment.application);
        PlayQueueDatabase playQueueDatabase = new PlayQueueDatabase(databaseOpenHelper, Schedulers.trampoline());
        storage = new PlayQueueStorage(playQueueDatabase);
    }

    @Test
    public void shouldInsertPlayQueue() {
        TrackQueueItem playableQueueItem1 = new TrackQueueItem.Builder(forTrack(123L))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .played(true)
                .build();

        PlaylistQueueItem playableQueueItem2 = new PlaylistQueueItem.Builder(forPlaylist(456L))
                .fromSource("source2", "version2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .played(false)
                .build();

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Arrays.asList(playableQueueItem1,
                                                                         playableQueueItem2));

        storage.store(playQueue);

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertPlayQueueItemsEqual(playableQueueItem1, playQueueItems.get(0));
        assertPlayQueueItemsEqual(playableQueueItem2, playQueueItems.get(1));
    }

    @Test
    public void shouldReplaceItemsInPlayQueue() {

        PlayableQueueItem playableQueueItem1 = new TrackQueueItem.Builder(forTrack(123L))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .played(true)
                .build();

        PlayableQueueItem playableQueueItem2 = new TrackQueueItem.Builder(forTrack(123L))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .played(true)
                .build();

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Arrays.asList(playableQueueItem1, playableQueueItem2));

        storage.store(playQueue);

        PlayableQueueItem playableQueueItem3 = new TrackQueueItem.Builder(forTrack(123L))
                .fromSource("source3", "version3", new Urn("sourceUrn3"), new Urn("queryUrn3"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .played(false)
                .build();

        playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(playableQueueItem3));

        storage.store(playQueue);

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertEquals(1, playQueueItems.size());
        assertPlayQueueItemsEqual(playableQueueItem3, playQueueItems.get(0));
    }

    @Test
    public void shouldSavePlayableQueueItems() {
        final PlayableQueueItem playableQueueItem1 = new TrackQueueItem.Builder(forTrack(1), forUser(1))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        final PlayableQueueItem playableQueueItem2 = new TrackQueueItem.Builder(forTrack(2), forUser(2))
                .fromSource("source2", "version2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        final PlayQueueItem nonPlayableItem = PlayQueueItem.EMPTY;

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Arrays.asList(playableQueueItem1,
                                                                         playableQueueItem2,
                                                                         nonPlayableItem));

        storage.store(playQueue);

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertEquals(2, playQueueItems.size());
        assertPlayQueueItemsEqual(playableQueueItem1, playQueueItems.get(0));
        assertPlayQueueItemsEqual(playableQueueItem2, playQueueItems.get(1));
    }

    @Test
    public void shouldDeleteAllPlayQueueItems() {
        TrackQueueItem playableQueueItem = new TrackQueueItem.Builder(forTrack(123L), forUser(123L))
                .fromSource("source",
                            "source_version",
                            new Urn("sourceUrn"),
                            new Urn("queryUrn"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(playableQueueItem));
        storage.store(playQueue);

        storage.clear().test();

        assertTrue(storage.load().blockingGet().isEmpty());
    }

    @Test
    public void shouldLoadAllPlayQueueItems() {
        final PlayableQueueItem expectedItem1 = new TrackQueueItem.Builder(forTrack(123L), forUser(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .build();

        final PlayableQueueItem expectedItem2 = new PlaylistQueueItem.Builder(forPlaylist(456L))
                .fromSource("source", "source_version 2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .build();

        storage.store(PlayQueue.fromPlayQueueItems(Arrays.asList(expectedItem1, expectedItem2)));

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertEquals(2, playQueueItems.size());
        assertPlayQueueItemsEqual(expectedItem1, playQueueItems.get(0));
        assertPlayQueueItemsEqual(expectedItem2, playQueueItems.get(1));
    }

    @Test
    public void shouldSavePlayQueueItemsWithReposter() {
        PlayableWithReposter playableWithReposter = PlayableWithReposter.create(forTrack(123L), Optional.of(forUser(456L)));
        final PlayableQueueItem expectedItem = new TrackQueueItem.Builder(playableWithReposter)
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .build();
        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(expectedItem));
        storage.store(playQueue);

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertEquals(forUser(456L), ((PlayableQueueItem)playQueueItems.get(0)).getReposter());
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutRelatedEntities() {
        final PlayableQueueItem expectedItem = new TrackQueueItem.Builder(forTrack(123L), forTrack(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(expectedItem));
        storage.store(playQueue);

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertPlayQueueItemsEqual(expectedItem, playQueueItems.get(0));
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutSourceOrQueryUrn() {
        final PlayableQueueItem expectedItem = new TrackQueueItem.Builder(forTrack(123L), forTrack(123L))
                // From a source with no source_urn or query_urn
                .fromSource("source", "source_version")
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(expectedItem));
        storage.store(playQueue);

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertPlayQueueItemsEqual(expectedItem, playQueueItems.get(0));
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutPlaybackContextUrnOrQuery() {
        final PlayableQueueItem expectedItem = new TrackQueueItem.Builder(forTrack(123L), forTrack(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .relatedEntity(RELATED_ENTITY)
                .withPlaybackContext(BUCKET_ONLY_PLAYBACK_CONTEXT)
                .build();
        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(expectedItem));
        storage.store(playQueue);

        List<PlayQueueItem> playQueueItems = storage.load().blockingGet();
        assertPlayQueueItemsEqual(expectedItem, playQueueItems.get(0));
    }


}
