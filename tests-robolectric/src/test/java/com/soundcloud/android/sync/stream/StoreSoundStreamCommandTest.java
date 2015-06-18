package com.soundcloud.android.sync.stream;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PromotedFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class StoreSoundStreamCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private StoreSoundStreamCommand command;

    @Before
    public void setup() {
        command = new StoreSoundStreamCommand(propeller());
    }

    @Test
    public void storesTrackPostMetadataFromApiTrackPost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.trackPost();
        command.call(Arrays.asList(streamItem));
        expectTrackPostItemInserted(streamItem);
        databaseAssertions().assertTrackInserted(streamItem.getTrack().get());
    }

    @Test
    public void storesTrackRepostMetadataFromApiTrackRepost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.trackRepost();
        command.call(Arrays.asList(streamItem));
        expectTrackRepostItemInserted(streamItem);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
    }

    @Test
    public void storesPlaylistPostMetadataFromApiPlaylistPost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistPost();
        command.call(Arrays.asList(streamItem));
        expectPlaylistPostItemInserted(streamItem);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
    }

    @Test
    public void storesPlaylistRepostMetadataFromApiPlaylistRepost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistRepost();
        command.call(Arrays.asList(streamItem));
        expectPlaylistRepostItemInserted(streamItem);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
    }

    @Test
    public void storesPromotionMetadataFromApiPromotedTrackWithPromoter() {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        command.call(Arrays.asList(streamItem));
        expectPromotedTrackPostItemInserted(streamItem);
        databaseAssertions().assertPromotionInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
        databaseAssertions().assertUserInserted(apiUser);
    }

    @Test
    public void storesPromotionMetadataFromApiPromotedTrackWithoutPromoter() {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        command.call(Arrays.asList(streamItem));
        databaseAssertions().assertPromotionWithoutPromoterInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
    }

    @Test
    public void storesAllStreamItemsWithDependencies() {
        final ApiStreamItem trackPost = ApiStreamItemFixtures.trackPost();
        final ApiStreamItem trackRepost = ApiStreamItemFixtures.trackRepost();
        final ApiStreamItem playlistPost = ApiStreamItemFixtures.playlistPost();
        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        command.call(Arrays.asList(
                trackPost,
                trackRepost,
                playlistPost,
                playlistRepost
        ));

        expectTrackPostItemInserted(trackPost);
        databaseAssertions().assertTrackWithUserInserted(trackPost.getTrack().get());

        expectTrackRepostItemInserted(trackRepost);
        databaseAssertions().assertTrackWithUserInserted(trackRepost.getTrack().get());

        expectPlaylistPostItemInserted(playlistPost);
        databaseAssertions().assertPlaylistWithUserInserted(playlistPost.getPlaylist().get());

        expectPlaylistRepostItemInserted(playlistRepost);
        databaseAssertions().assertPlaylistWithUserInserted(playlistRepost.getPlaylist().get());
    }

    @Test
    public void removesPromotedTracksFromStreamBeforeStoringNewItems() {
        command.call(Arrays.asList(
                ApiStreamItemFixtures.promotedStreamItemWithoutPromoter(),
                ApiStreamItemFixtures.trackPost()));

        command.call(Arrays.asList(ApiStreamItemFixtures.playlistPost()));

        expectStreamItemCountToBe(2);
        expectPromotedTrackItemCountToBe(0);
    }

    @Test
    public void removePromotedTracksOnlyIfTheyCorrespondToPromotedIdForStream() {
        testFixtures().insertPromotedTrackMetadata(123L, System.currentTimeMillis());

        command.call(Arrays.asList(
                ApiStreamItemFixtures.promotedStreamItemWithoutPromoter(),
                ApiStreamItemFixtures.trackPost()));

        expectStreamItemCountToBe(2);
        expectPromotedTrackItemCountToBe(2);
    }

    private void expectStreamItemCountToBe(int count) {
        assertThat(select(from(Table.SoundStream.name())), counts(count));
    }

    private void expectPromotedTrackItemCountToBe(int count) {
        assertThat(select(from(Table.PromotedTracks.name())), counts(count));
    }

    private void expectTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereNull(TableColumns.SoundStream.REPOSTER_ID)
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
    }

    private void expectPromotedTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereNull(TableColumns.SoundStream.REPOSTER_ID)
                        .whereEq(TableColumns.SoundStream.CREATED_AT, Long.MAX_VALUE)
                        .whereNotNull(TableColumns.SoundStream.PROMOTED_ID)
        ), counts(1));
    }

    private void expectTrackRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereEq(TableColumns.SoundStream.REPOSTER_ID, streamItem.getReposter().get().getId())
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
    }

    private void expectPlaylistPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getPlaylist().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                        .whereNull(TableColumns.SoundStream.REPOSTER_ID)
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
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
