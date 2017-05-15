package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.BaseIntegrationTest;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.model.Urn;
import io.reactivex.observers.TestObserver;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DiscoveryStorageIntegrationTest extends BaseIntegrationTest {

    private static final String ARTWORK = "artwork";
    private static final int COUNT = 10;
    private static final String TITLE = "title";
    private static final String SUBTITLE = "subtitle";
    private static final String WEB_LINK = "subtitle";
    private static final String APP_LINK = "subtitle";
    private DiscoveryReadableStorage discoveryReadableStorage;
    private DiscoveryWritableStorage discoveryWritableStorage;

    public DiscoveryStorageIntegrationTest() {
        super(TestUser.playlistUser);
        discoveryReadableStorage = SoundCloudApplication.getObjectGraph().discoveryReadableStorage();
        discoveryWritableStorage = SoundCloudApplication.getObjectGraph().discoveryWritableStorage();
        discoveryWritableStorage.cleanUp();
    }

    @Test
    public void insertAndReadSelectionItems() throws Exception {
        final long itemId = discoveryWritableStorage.insertSelectionItem(ApiSelectionItem.create(new Urn("soundcloud:system_playlist:123"), ARTWORK, COUNT, TITLE, SUBTITLE, WEB_LINK, APP_LINK), 2);

        final List<DbModel.SelectionItem> selectionItems = discoveryReadableStorage.selectionItems().test().assertValueCount(1).values().get(0);
        assertThat(selectionItems).hasSize(1);
        assertThat(selectionItems.get(0)._id()).isEqualTo(itemId);
    }

    @Test
    public void insertAndReadSelectionItemsFromLiveObservable() throws Exception {
        final long itemId1 = discoveryWritableStorage.insertSelectionItem(ApiSelectionItem.create(new Urn("soundcloud:system_playlist:123"), ARTWORK, COUNT, TITLE, SUBTITLE, WEB_LINK, APP_LINK), 2);
        final TestObserver<List<DbModel.SelectionItem>> test = discoveryReadableStorage.liveSelectionItems().test();
        test.awaitCount(1);
        test.assertValueCount(1);
        assertThat(test.values().get(0).get(0)._id()).isEqualTo(itemId1);
        final long itemId2 = discoveryWritableStorage.insertSelectionItem(ApiSelectionItem.create(new Urn("soundcloud:system_playlist:124"), ARTWORK, COUNT, TITLE, SUBTITLE, WEB_LINK, APP_LINK), 2);
        test.awaitCount(2);
        test.assertValueCount(2);
        assertThat(test.values().get(1).get(0)._id()).isEqualTo(itemId1);
        assertThat(test.values().get(1).get(1)._id()).isEqualTo(itemId2);
    }

    @Test
    public void insertAndReadDiscoveryCard() throws Exception {
        discoveryWritableStorage.insertApiDiscoveryCards(Fixtures.discoveryCards(2));
        final List<DiscoveryCard> discoveryCards = discoveryReadableStorage.discoveryCards().test().assertValueCount(1).values().get(0);

        assertThat(discoveryCards).hasSize(2);

        assertThat(discoveryCards.get(0).kind()).isEqualTo(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD);
        assertThat(discoveryCards.get(1).kind()).isEqualTo(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD);
    }

    @Test
    public void insertAndReadDiscoveryCardFromLiveObservable() throws Exception {
        discoveryWritableStorage.insertApiDiscoveryCards(Fixtures.discoveryCards(1));
        final TestObserver<List<DiscoveryCard>> test = discoveryReadableStorage.liveDiscoveryCards().test();

        test.awaitCount(1);
        test.assertValueCount(1);
        discoveryWritableStorage.insertApiDiscoveryCards(Fixtures.discoveryCards(2));
        test.awaitCount(2);
        test.assertValueCount(2);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        discoveryWritableStorage.cleanUp();
        super.tearDown();
    }
}
