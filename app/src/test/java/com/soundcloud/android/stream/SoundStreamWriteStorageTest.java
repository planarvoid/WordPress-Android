package com.soundcloud.android.stream;

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

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamWriteStorageTest extends StorageIntegrationTest {
    private SoundStreamWriteStorage storage;

    @Before
    public void setup() {
        storage = new SoundStreamWriteStorage(propeller());
    }

    @Test
    public void shouldStoreTrackPostMetadataFromApiTrackPost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.trackPost();
        storage.insertStreamItems(Arrays.asList(streamItem));
        expectTrackPostItemInserted(streamItem);
        databaseAssertions().assertTrackInserted(streamItem.getTrack().get());
    }

    @Test
    public void shouldStoreTrackRepostMetadataFromApiTrackRepost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.trackRepost();
        storage.insertStreamItems(Arrays.asList(streamItem));
        expectTrackRepostItemInserted(streamItem);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
    }

    @Test
    public void shouldStorePlaylistPostMetadataFromApiPlaylistPost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistPost();
        storage.insertStreamItems(Arrays.asList(streamItem));
        expectPlaylistPostItemInserted(streamItem);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
    }

    @Test
    public void shouldStorePlaylistRepostMetadataFromApiPlaylistRepost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistRepost();
        storage.insertStreamItems(Arrays.asList(streamItem));
        expectPlaylistRepostItemInserted(streamItem);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
    }

    @Test
    public void shouldStorePromotionMetadataFromApiPromotedTrackWithPromoter() {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        storage.insertStreamItems(Arrays.asList(streamItem));
        expectPromotedTrackPostItemInserted(streamItem);
        databaseAssertions().assertPromotionInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
        databaseAssertions().assertUserInserted(apiUser);
    }

    @Test
    public void shouldStorePromotionMetadataFromApiPromotedTrackWithoutPromoter() {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        storage.insertStreamItems(Arrays.asList(streamItem));
        databaseAssertions().assertPromotionWithoutPromoterInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
    }

    @Test
    public void shouldReplaceExistingStreamItem() {
        testFixtures().insertStreamTrackPost(ApiStreamItemFixtures.trackPost());
        expectStreamItemCountToBe(1);

        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        storage.replaceStreamItems(Arrays.asList(playlistRepost));
        expectPlaylistRepostItemInserted(playlistRepost);
        expectStreamItemCountToBe(1);
    }

    @Test
    public void shouldClearSoundStreamItems() {
        testFixtures().insertStreamTrackPost(ApiStreamItemFixtures.trackPost());
        expectStreamItemCountToBe(1);

        storage.clear();
        expectStreamItemCountToBe(0);
    }

    @Test
    public void shouldStoreAllStreamItemsWithDependencies() {
        final ApiStreamItem trackPost = ApiStreamItemFixtures.trackPost();
        final ApiStreamItem trackRepost = ApiStreamItemFixtures.trackRepost();
        final ApiStreamItem playlistPost = ApiStreamItemFixtures.playlistPost();
        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        storage.insertStreamItems(Arrays.asList(
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

    private void expectStreamItemCountToBe(int count){
        assertThat(select(from(Table.SoundStream.name())), counts(count));
    }

    private void expectTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getTrack().get().getCreatedAt().getTime())
        ), counts(1));
    }

    private void expectPromotedTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
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
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getPlaylist().get().getCreatedAt().getTime())
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