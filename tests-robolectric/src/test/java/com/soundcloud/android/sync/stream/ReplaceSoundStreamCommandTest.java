package com.soundcloud.android.sync.stream;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ReplaceSoundStreamCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private ReplaceSoundStreamCommand command;

    @Before
    public void setup() {
        command = new ReplaceSoundStreamCommand(propeller());
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
        assertThat(select(from(Table.SoundStream.name())), counts(count));
    }

    private void expectPromotedTrackCountToBe(int count) {
        assertThat(select(from(Table.PromotedTracks.name())), counts(count));
    }

    private void expectPlaylistRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getPlaylist().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.SoundStream.REPOSTER_ID, streamItem.getReposter().get().getId())
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
    }

}