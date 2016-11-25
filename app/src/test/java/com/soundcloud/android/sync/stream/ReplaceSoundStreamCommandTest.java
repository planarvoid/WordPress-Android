package com.soundcloud.android.sync.stream;

import static com.soundcloud.android.storage.Table.PromotedTracks;
import static com.soundcloud.android.storage.Table.SoundStream;
import static com.soundcloud.android.storage.TableColumns.SoundStream.CREATED_AT;
import static com.soundcloud.android.storage.TableColumns.SoundStream.REPOSTER_ID;
import static com.soundcloud.android.storage.TableColumns.SoundStream.SOUND_ID;
import static com.soundcloud.android.storage.TableColumns.SoundStream.SOUND_TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.inject.Provider;
import java.util.Arrays;

public class ReplaceSoundStreamCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private ReplaceSoundStreamCommand command;

    @Before
    public void setup() {
        Provider<StoreUsersCommand> storeUsersCommandProvider = providerOf(new StoreUsersCommand(
                propeller()));
        Provider<StoreTracksCommand> storeTracksCommandProvider = providerOf(new StoreTracksCommand(
                propeller(),
                new StoreUsersCommand(propeller())));
        Provider<StorePlaylistsCommand> storePlaylistsCommandProvider = providerOf(new StorePlaylistsCommand(
                propeller(),
                new StoreUsersCommand(propeller())));

        command = new ReplaceSoundStreamCommand(propeller(), new SoundStreamReplaceTransactionFactory(
                storeUsersCommandProvider,
                storeTracksCommandProvider,
                storePlaylistsCommandProvider));
    }

    @Test
    public void replacesExistingStreamItem() {
        testFixtures().insertStreamTrackPost(ApiStreamItemFixtures.trackPost());
        expectStreamItemCountToBe(1);

        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        command.call(Arrays.asList(playlistRepost));
        expectPlaylistRepostItemInserted(playlistRepost);
        expectStreamItemCountToBe(1);
    }

    @Test
    public void removesExistingPromotedTrackMetadata() {
        testFixtures().insertPromotedStreamTrack(1000L);
        expectPromotedTrackCountToBe(1);

        command.call(Arrays.asList(ApiStreamItemFixtures.trackPost()));
        expectPromotedTrackCountToBe(0);
    }

    private void expectStreamItemCountToBe(int count) {
        assertThat(select(from(SoundStream.name()))).counts(count);
    }

    private void expectPromotedTrackCountToBe(int count) {
        assertThat(select(from(PromotedTracks.name()))).counts(count);
    }

    private void expectPlaylistRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(SoundStream.name())
                                  .whereEq(SOUND_ID, streamItem.getPlaylist().get().getId())
                                  .whereEq(SOUND_TYPE, TYPE_PLAYLIST)
                                  .whereEq(REPOSTER_ID, streamItem.getReposter().get().getId())
                                  .whereEq(CREATED_AT, streamItem.getCreatedAtTime())
        )).counts(1);
    }

}
