package com.soundcloud.android.sync.commands;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PromotedFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class StoreSoundStreamCommandTest extends StorageIntegrationTest {

    protected StoreCommand<Iterable<ApiStreamItem>> getStorage() {
        return new StoreSoundStreamCommand(propeller());
    }

    @Test
    public void shouldStoreTrackPostMetadataFromApiTrackPost() throws Exception {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.trackPost();
        getStorage().with(Arrays.asList(streamItem)).call();
        expectTrackPostItemInserted(streamItem);
        databaseAssertions().assertTrackInserted(streamItem.getTrack().get());
    }


    @Test
    public void shouldStoreTrackRepostMetadataFromApiTrackRepost() throws Exception {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.trackRepost();
        getStorage().with(Arrays.asList(streamItem)).call();
        expectTrackRepostItemInserted(streamItem);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
    }

    @Test
    public void shouldStorePlaylistPostMetadataFromApiPlaylistPost() throws Exception {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistPost();
        getStorage().with(Arrays.asList(streamItem)).call();
        expectPlaylistPostItemInserted(streamItem);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
    }

    @Test
    public void shouldStorePlaylistRepostMetadataFromApiPlaylistRepost() throws Exception {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistRepost();
        getStorage().with(Arrays.asList(streamItem)).call();
        expectPlaylistRepostItemInserted(streamItem);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
    }

    @Test
    public void shouldStorePromotionMetadataFromApiPromotedTrackWithPromoter() throws Exception {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        getStorage().with(Arrays.asList(streamItem)).call();
        expectPromotedTrackPostItemInserted(streamItem);
        databaseAssertions().assertPromotionInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
        databaseAssertions().assertUserInserted(apiUser);
    }

    @Test
    public void shouldStorePromotionMetadataFromApiPromotedTrackWithoutPromoter() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        getStorage().with(Arrays.asList(streamItem)).call();
        databaseAssertions().assertPromotionWithoutPromoterInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
    }

    @Test
    public void shouldStoreAllStreamItemsWithDependencies() throws Exception {
        final ApiStreamItem trackPost = ApiStreamItemFixtures.trackPost();
        final ApiStreamItem trackRepost = ApiStreamItemFixtures.trackRepost();
        final ApiStreamItem playlistPost = ApiStreamItemFixtures.playlistPost();
        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        getStorage().with(Arrays.asList(
                trackPost,
                trackRepost,
                playlistPost,
                playlistRepost
        )).call();

        expectTrackPostItemInserted(trackPost);
        databaseAssertions().assertTrackWithUserInserted(trackPost.getTrack().get());

        expectTrackRepostItemInserted(trackRepost);
        databaseAssertions().assertTrackWithUserInserted(trackRepost.getTrack().get());

        expectPlaylistPostItemInserted(playlistPost);
        databaseAssertions().assertPlaylistWithUserInserted(playlistPost.getPlaylist().get());

        expectPlaylistRepostItemInserted(playlistRepost);
        databaseAssertions().assertPlaylistWithUserInserted(playlistRepost.getPlaylist().get());
    }

    protected void expectStreamItemCountToBe(int count){
        assertThat(select(from(Table.SoundStream.name())), counts(count));
    }

    protected void expectTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getTrack().get().getCreatedAt().getTime())
        ), counts(1));
    }

    protected void expectPromotedTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
                        .whereEq(TableColumns.SoundStream.CREATED_AT, Long.MAX_VALUE)
                        .whereNotNull(TableColumns.SoundStream.PROMOTED_ID)
        ), counts(1));
    }

    protected void expectTrackRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereEq(TableColumns.SoundStream.REPOSTER_ID, streamItem.getReposter().get().getId())
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
    }

    protected void expectPlaylistPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getPlaylist().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getPlaylist().get().getCreatedAt().getTime())
        ), counts(1));
    }

    protected void expectPlaylistRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SoundStream.name())
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getPlaylist().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.SoundStream.REPOSTER_ID, streamItem.getReposter().get().getId())
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
    }
}