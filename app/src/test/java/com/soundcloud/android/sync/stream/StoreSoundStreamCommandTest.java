package com.soundcloud.android.sync.stream;

import static com.soundcloud.android.storage.Table.PromotedTracks;
import static com.soundcloud.android.storage.Table.SoundStream;
import static com.soundcloud.android.storage.TableColumns.SoundStream.CREATED_AT;
import static com.soundcloud.android.storage.TableColumns.SoundStream.PROMOTED_ID;
import static com.soundcloud.android.storage.TableColumns.SoundStream.REPOSTER_ID;
import static com.soundcloud.android.storage.TableColumns.SoundStream.SOUND_ID;
import static com.soundcloud.android.storage.TableColumns.SoundStream.SOUND_TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.lang.Long.MAX_VALUE;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiPromotedPlaylist;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PromotedFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.inject.Provider;
import java.util.Arrays;

public class StoreSoundStreamCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private StoreSoundStreamCommand command;

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

        command = new StoreSoundStreamCommand(propeller(), new SoundStreamInsertTransactionFactory(
                storeUsersCommandProvider,
                storeTracksCommandProvider,
                storePlaylistsCommandProvider
        ));
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
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        command.call(Arrays.asList(streamItem));
        expectPromotedTrackPostItemInserted(streamItem);
        databaseAssertions().assertPromotionInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
        databaseAssertions().assertUserInserted(apiUser);
    }

    @Test
    public void storesPromotionMetadataFromApiPromotedPlaylistWithPromoter() {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        command.call(Arrays.asList(streamItem));
        expectPromotedPlaylistPostItemInserted(streamItem);
        databaseAssertions().assertPromotionInserted(apiPromotedPlaylist);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
        databaseAssertions().assertUserInserted(apiUser);
    }

    @Test
    public void storesPromotionMetadataFromApiPromotedTrackWithoutPromoter() {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        command.call(Arrays.asList(streamItem));
        databaseAssertions().assertPromotionWithoutPromoterInserted(apiPromotedTrack);
        databaseAssertions().assertTrackWithUserInserted(streamItem.getTrack().get());
    }

    @Test
    public void storesPromotionMetadataFromApiPromotedPlaylistWithoutPromoter() {
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        command.call(Arrays.asList(streamItem));
        databaseAssertions().assertPromotionWithoutPromoterInserted(apiPromotedPlaylist);
        databaseAssertions().assertPlaylistWithUserInserted(streamItem.getPlaylist().get());
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
                ApiStreamItemFixtures.promotedTrackWithoutPromoter(),
                ApiStreamItemFixtures.trackPost()));

        command.call(Arrays.asList(ApiStreamItemFixtures.playlistPost()));

        expectStreamItemCountToBe(2);
        expectPromotedTrackItemCountToBe(0);
    }

    @Test
    public void removePromotedTracksOnlyIfTheyCorrespondToPromotedIdForStream() {
        testFixtures().insertPromotedTrackMetadata(123L, System.currentTimeMillis());

        command.call(Arrays.asList(
                ApiStreamItemFixtures.promotedTrackWithoutPromoter(),
                ApiStreamItemFixtures.trackPost()));

        expectStreamItemCountToBe(2);
        expectPromotedTrackItemCountToBe(2);
    }

    private void expectStreamItemCountToBe(int count) {
        assertThat(select(from(SoundStream.name()))).counts(count);
    }

    private void expectPromotedTrackItemCountToBe(int count) {
        assertThat(select(from(PromotedTracks.name()))).counts(count);
    }

    private void expectTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(SoundStream.name())
                                  .whereEq(SOUND_ID, streamItem.getTrack().get().getId())
                                  .whereEq(SOUND_TYPE, TYPE_TRACK)
                                  .whereNull(REPOSTER_ID)
                                  .whereEq(CREATED_AT, streamItem.getCreatedAtTime())
        )).counts(1);
    }

    private void expectPromotedTrackPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(SoundStream.name())
                                  .whereEq(SOUND_ID, streamItem.getTrack().get().getId())
                                  .whereEq(SOUND_TYPE, TYPE_TRACK)
                                  .whereNull(REPOSTER_ID)
                                  .whereEq(CREATED_AT, MAX_VALUE)
                                  .whereNotNull(PROMOTED_ID)
        )).counts(1);
    }

    private void expectPromotedPlaylistPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(SoundStream.name())
                                  .whereEq(SOUND_ID, streamItem.getPlaylist().get().getId())
                                  .whereEq(SOUND_TYPE, TYPE_PLAYLIST)
                                  .whereNull(REPOSTER_ID)
                                  .whereEq(CREATED_AT, MAX_VALUE)
                                  .whereNotNull(PROMOTED_ID)
        )).counts(1);
    }

    private void expectTrackRepostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(SoundStream.name())
                                  .whereEq(SOUND_ID, streamItem.getTrack().get().getId())
                                  .whereEq(SOUND_TYPE, TYPE_TRACK)
                                  .whereEq(REPOSTER_ID, streamItem.getReposter().get().getId())
                                  .whereEq(CREATED_AT, streamItem.getCreatedAtTime())
        )).counts(1);
    }

    private void expectPlaylistPostItemInserted(ApiStreamItem streamItem) {
        assertThat(select(from(SoundStream.name())
                                  .whereEq(SOUND_ID, streamItem.getPlaylist().get().getId())
                                  .whereEq(SOUND_TYPE, TYPE_PLAYLIST)
                                  .whereNull(REPOSTER_ID)
                                  .whereEq(CREATED_AT, streamItem.getCreatedAtTime())
        )).counts(1);
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
