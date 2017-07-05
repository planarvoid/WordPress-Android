package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.BaseIntegrationTest;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.discovery.systemplaylist.ApiSystemPlaylist;
import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistEntity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import io.reactivex.observers.TestObserver;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DiscoveryStorageIntegrationTest extends BaseIntegrationTest {

    private static final String ARTWORK = "artwork";
    private static final ImageStyle ARTWORK_STYLE = ImageStyle.CIRCULAR;
    private static final int COUNT = 10;
    private static final String TITLE = "title";
    private static final String SUBTITLE = "subtitle";
    private static final String WEB_LINK = "subtitle";
    private static final String APP_LINK = "subtitle";
    private static final Urn SELECTION_URN = Urn.forSystemPlaylist("1");
    private static final Urn PARENT_QUERY_URN = new Urn("soundcloud:discovery:123");
    private DiscoveryReadableStorage discoveryReadableStorage;
    private DiscoveryWritableStorage discoveryWritableStorage;

    public DiscoveryStorageIntegrationTest() {
        super(TestUser.playlistUser);
        discoveryReadableStorage = SoundCloudApplication.getObjectGraph().discoveryReadableStorage();
        discoveryWritableStorage = SoundCloudApplication.getObjectGraph().discoveryWritableStorage();
        discoveryWritableStorage.clearData();
    }

    @Test
    public void insertAndReadSelectionItems() throws Exception {
        final long itemId = discoveryWritableStorage.insertSelectionItem(ApiSelectionItem.create(Urn.forSystemPlaylist("123"), ARTWORK, ARTWORK_STYLE, COUNT, TITLE, SUBTITLE, WEB_LINK, APP_LINK),
                                                                         SELECTION_URN);

        final List<DbModel.SelectionItem> selectionItems = discoveryReadableStorage.selectionItems().test().assertValueCount(1).values().get(0);
        assertThat(selectionItems).hasSize(1);
        assertThat(selectionItems.get(0)._id()).isEqualTo(itemId);
    }

    @Test
    public void insertAndReadSelectionItemsFromLiveObservable() throws Exception {
        final long itemId1 = discoveryWritableStorage.insertSelectionItem(ApiSelectionItem.create(Urn.forSystemPlaylist("123"), ARTWORK, ARTWORK_STYLE, COUNT, TITLE, SUBTITLE, WEB_LINK, APP_LINK),
                                                                          SELECTION_URN);
        final TestObserver<List<DbModel.SelectionItem>> test = discoveryReadableStorage.liveSelectionItems().test();
        test.awaitCount(1);
        test.assertValueCount(1);
        assertThat(test.values().get(0).get(0)._id()).isEqualTo(itemId1);
        final long itemId2 = discoveryWritableStorage.insertSelectionItem(ApiSelectionItem.create(Urn.forSystemPlaylist("124"), ARTWORK, ARTWORK_STYLE, COUNT, TITLE, SUBTITLE, WEB_LINK, APP_LINK),
                                                                          SELECTION_URN);
        test.awaitCount(2);
        test.assertValueCount(2);
        assertThat(test.values().get(1).get(0)._id()).isEqualTo(itemId1);
        assertThat(test.values().get(1).get(1)._id()).isEqualTo(itemId2);
    }

    @Test
    public void insertAndReadDiscoveryCard() throws Exception {
        discoveryWritableStorage.insertApiDiscoveryCards(Fixtures.discoveryCards(2), Optional.of(PARENT_QUERY_URN));
        final List<DiscoveryCard> discoveryCards = discoveryReadableStorage.discoveryCards().test().assertValueCount(1).values().get(0);

        assertThat(discoveryCards).hasSize(2);

        assertThat(discoveryCards.get(0).kind()).isEqualTo(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD);
        assertThat(discoveryCards.get(1).kind()).isEqualTo(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD);
    }

    @Test
    public void insertAndReadDiscoveryCardFromLiveObservable() throws Exception {
        discoveryWritableStorage.insertApiDiscoveryCards(Fixtures.discoveryCards(1), Optional.of(PARENT_QUERY_URN));
        final TestObserver<List<DiscoveryCard>> test = discoveryReadableStorage.liveDiscoveryCards().test();

        test.awaitCount(1);
        test.assertValueCount(1);
        discoveryWritableStorage.insertApiDiscoveryCards(Fixtures.discoveryCards(2), Optional.of(PARENT_QUERY_URN));
        test.awaitCount(2);
        test.assertValueCount(2);
    }

    @Test
    public void insertAndReadSystemPlaylistRemovesOldData() throws Exception {
        ApiSystemPlaylist playlist = Fixtures.SYSTEM_PLAYLIST;
        ApiSystemPlaylist updatedPlaylist = ApiSystemPlaylist.create(playlist.urn(),
                                                                     playlist.trackCount(),
                                                                     playlist.lastUpdated(),
                                                                     Optional.of("updated title"),
                                                                     Optional.of("updated description"),
                                                                     playlist.artworkUrlTemplate(),
                                                                     playlist.trackingFeatureName(),
                                                                     new ModelCollection<>(Collections.singletonList(new ApiTrack(Urn.forTrack(456L)))));
        discoveryWritableStorage.storeSystemPlaylist(playlist);
        discoveryWritableStorage.storeSystemPlaylist(updatedPlaylist);
        final SystemPlaylistEntity systemPlaylistEntity = discoveryReadableStorage.systemPlaylistEntity(playlist.urn()).test().assertValueCount(1).values().get(0);

        assertThat(systemPlaylistEntity).isEqualTo(map(updatedPlaylist));
    }

    private SystemPlaylistEntity map(ApiSystemPlaylist apiSystemPlaylist) {
        return SystemPlaylistEntity.create(
                apiSystemPlaylist.urn(),
                apiSystemPlaylist.tracks().getQueryUrn(),
                apiSystemPlaylist.title(),
                apiSystemPlaylist.description(),
                apiSystemPlaylist.tracks().transform(ApiTrack::getUrn).getCollection(),
                apiSystemPlaylist.lastUpdated(),
                apiSystemPlaylist.artworkUrlTemplate(),
                apiSystemPlaylist.trackingFeatureName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        discoveryWritableStorage.clearData();
        super.tearDown();
    }
}
