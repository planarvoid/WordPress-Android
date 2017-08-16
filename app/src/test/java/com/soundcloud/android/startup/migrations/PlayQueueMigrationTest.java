package com.soundcloud.android.startup.migrations;

import static com.soundcloud.android.model.Urn.forPlaylist;
import static com.soundcloud.android.model.Urn.forTrack;
import static com.soundcloud.android.model.Urn.forUser;
import static com.soundcloud.android.playback.PlaybackContext.Bucket.LISTENING_HISTORY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.soundcloud.java.optional.Optional.of;


import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.OldPlayQueueStorage;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueStorage;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.PlaylistQueueItem;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import java.util.ArrayList;

public class PlayQueueMigrationTest extends StorageIntegrationTest {

    private static final PlaybackContext PLAYBACK_CONTEXT = PlaybackContext.builder()
                                                                           .bucket(LISTENING_HISTORY)
                                                                           .query(of("some filter"))
                                                                           .urn(of(Urn.forPlaylist(321L)))
                                                                           .build();

    private static final PlayableQueueItem EXPECTED_ITEM_2 = new PlaylistQueueItem.Builder(forPlaylist(456L))
            .fromSource("source", "source_version 2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
            .withPlaybackContext(PLAYBACK_CONTEXT)
            .relatedEntity(Urn.forTrack(987L))
            .build();

    private static final PlayableQueueItem EXPECTED_ITEM_1 = new TrackQueueItem.Builder(forTrack(123L), forUser(123L))
            .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
            .withPlaybackContext(PLAYBACK_CONTEXT)
            .relatedEntity(Urn.forTrack(987L))
            .build();


    @Mock private PlayQueueStorage newStorage;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    @Before
    public void setUp() throws Exception {
        when(sharedPreferences.edit()).thenReturn(editor);
    }

    @Test
    public void migratesOldItemsToNewStorage() throws Exception {
        ArrayList<PlayQueueItem> playQueueItems = new ArrayList<>();
        playQueueItems.add(EXPECTED_ITEM_1);
        playQueueItems.add(EXPECTED_ITEM_2);

        OldPlayQueueStorage oldStorage = new OldPlayQueueStorage(propellerRxV2());
        oldStorage.store(PlayQueue.fromPlayQueueItems(playQueueItems)).test();

        MigrationEngine migrationEngine = new MigrationEngine(748, sharedPreferences, new PlayQueueMigration(oldStorage, newStorage, Schedulers.trampoline()));
        migrationEngine.migrate();

        verify(newStorage).store(PlayQueue.fromPlayQueueItems(playQueueItems));
    }
}