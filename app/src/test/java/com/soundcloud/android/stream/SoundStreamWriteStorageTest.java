package com.soundcloud.android.stream;

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
        storage.replaceStreamItems(Arrays.asList(streamItem));
        expectTrackPostItemInserted(streamItem);
    }

    @Test
    public void shouldStoreTrackRepostMetadataFromApiTrackRepost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.trackRepost();
        storage.replaceStreamItems(Arrays.asList(streamItem));
        expectTrackRepostItemInserted(streamItem);
    }

    @Test
    public void shouldStorePlaylistPostMetadataFromApiPlaylistPost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistPost();
        storage.replaceStreamItems(Arrays.asList(streamItem));
        expectPlaylistPostItemInserted(streamItem);
    }

    @Test
    public void shouldStorePlaylistRepostMetadataFromApiPlaylistRepost() {
        final ApiStreamItem streamItem = ApiStreamItemFixtures.playlistRepost();
        storage.replaceStreamItems(Arrays.asList(streamItem));
        expectPlaylistRepostItemInserted(streamItem);
    }

    @Test
    public void shouldReplaceExistingStreamItem() {
        final ApiStreamItem trackPost = ApiStreamItemFixtures.trackPost();
        storage.replaceStreamItems(Arrays.asList(trackPost));
        expectTrackPostItemInserted(trackPost);

        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        storage.replaceStreamItems(Arrays.asList(playlistRepost));
        expectPlaylistRepostItemInserted(playlistRepost);
        expectStreamItemCountToBe(1);
    }

    @Test
    public void shouldStoreAllStreamItemMetadatas() {
        final ApiStreamItem trackPost = ApiStreamItemFixtures.trackPost();
        final ApiStreamItem trackRepost = ApiStreamItemFixtures.trackRepost();
        final ApiStreamItem playlistPost = ApiStreamItemFixtures.playlistPost();
        final ApiStreamItem playlistRepost = ApiStreamItemFixtures.playlistRepost();
        storage.replaceStreamItems(Arrays.asList(
                trackPost,
                trackRepost,
                playlistPost,
                playlistRepost
        ));
        expectTrackPostItemInserted(trackPost);
        expectTrackRepostItemInserted(trackRepost);
        expectPlaylistPostItemInserted(playlistPost);
        expectPlaylistRepostItemInserted(playlistRepost);
    }

    private void expectStreamItemCountToBe(int count){
        assertThat(select(from(Table.SOUNDSTREAM.name)), counts(count));
    }

    private void expectTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SOUNDSTREAM.name)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getTrack().get().getCreatedAt().getTime())
        ), counts(1));
    }

    private void expectTrackRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SOUNDSTREAM.name)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getTrack().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereEq(TableColumns.SoundStream.REPOSTER_ID, streamItem.getReposter().get().getId())
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
    }

    private void expectPlaylistPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SOUNDSTREAM.name)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getPlaylist().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                        .where(TableColumns.SoundStream.REPOSTER_ID + " IS NULL")
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getPlaylist().get().getCreatedAt().getTime())
        ), counts(1));
    }

    private void expectPlaylistRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(Table.SOUNDSTREAM.name)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, streamItem.getPlaylist().get().getId())
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.SoundStream.REPOSTER_ID, streamItem.getReposter().get().getId())
                        .whereEq(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime())
        ), counts(1));
    }

}