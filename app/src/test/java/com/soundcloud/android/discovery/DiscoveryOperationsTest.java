package com.soundcloud.android.discovery;

import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryOperationsTest {

    @Mock private NewSyncOperations syncOperations;
    @Mock private DiscoveryReadableStorage storage;

    private DiscoveryOperations discoveryOperations;

    private final Scheduler scheduler = Schedulers.trampoline();
    private final Syncable discoveryCardsSyncable = Syncable.DISCOVERY_CARDS;

    private SelectionItem selectionItem = SelectionItem.create(Optional.absent(),
                                                               Urn.forSystemPlaylist("upload"),
                                                               Optional.absent(),
                                                               Optional.absent(),
                                                               Optional.absent(),
                                                               Optional.absent(),
                                                               Optional.absent(),
                                                               Optional.absent(),
                                                               Optional.absent());

    private final DiscoveryCard.MultipleContentSelectionCard multiCard =
            DiscoveryCard.MultipleContentSelectionCard.create(Urn.forSystemPlaylist("123"),
                                                              Optional.absent(),
                                                              Optional.absent(),
                                                              Optional.absent(),
                                                              Optional.absent(),
                                                              Optional.absent(),
                                                              Optional.absent(),
                                                              Collections.singletonList(selectionItem));

    private final List<DiscoveryCard> discoveryCards = Lists.newArrayList(multiCard);

    @Before
    public void setUp() throws Exception {
        discoveryOperations = new DiscoveryOperations(syncOperations, storage, scheduler);
    }

    @Test
    public void discoveryCardsReturnsCardsFromStorageAfterSyncSuccess() throws Exception {
        setUpDiscoveryCards(SyncResult.synced(), Maybe.just(discoveryCards));

        discoveryOperations.discoveryCards()
                           .test()
                           .assertComplete()
                           .assertValue(discoveryCards);
    }

    @Test
    public void discoveryCardsReturnsCardsFromStorageAfterSyncFailure() throws Exception {
        setUpDiscoveryCards(SyncResult.error(new Exception("Relevant error message")), Maybe.just(discoveryCards));

        discoveryOperations.discoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(discoveryCards);
    }

    @Test
    public void discoveryCardsReturnsEmptyCardWithErrorAfterSyncFailureAndStorageIsEmpty() throws Exception {
        SyncResult syncResult = SyncResult.error(new Exception("Relevant error message"));
        setUpDiscoveryCards(syncResult, Maybe.empty());

        discoveryOperations.discoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(Lists.newArrayList(DiscoveryCard.EmptyCard.create(syncResult.throwable())));
    }

    @Test
    public void refreshDiscoveryCardsReturnsCardsFromStorageAfterSyncSuccess() throws Exception {
        setUpRefreshDiscoveryCards(SyncResult.synced(), Maybe.just(discoveryCards));

        discoveryOperations.refreshDiscoveryCards()
                           .test()
                           .assertValue(discoveryCards);
    }

    @Test
    public void refreshDiscoveryCardsReturnsCardsFromStorageAfterSyncFailure() throws Exception {
        setUpRefreshDiscoveryCards(SyncResult.error(new Exception("Relevant error message")), Maybe.just(discoveryCards));

        discoveryOperations.refreshDiscoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(discoveryCards);
    }

    @Test
    public void refreshDiscoveryCardsReturnsEmptyCardWithErrorAfterSyncFailureAndStorageIsEmpty() throws Exception {
        SyncResult syncResult = SyncResult.error(new Exception("Relevant error message"));
        setUpRefreshDiscoveryCards(syncResult, Maybe.empty());

        discoveryOperations.refreshDiscoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(Lists.newArrayList(DiscoveryCard.EmptyCard.create(syncResult.throwable())));
    }

    private void setUpDiscoveryCards(SyncResult syncResult, Maybe<List<DiscoveryCard>> storageResult) {
        when(syncOperations.lazySyncIfStale(discoveryCardsSyncable)).thenReturn(Single.just(syncResult));
        when(storage.discoveryCards()).thenReturn(storageResult);
    }

    private void setUpRefreshDiscoveryCards(SyncResult syncResult, Maybe<List<DiscoveryCard>> storageResult) {
        when(syncOperations.sync(discoveryCardsSyncable)).thenReturn(Single.just(syncResult));
        when(storage.discoveryCards()).thenReturn(storageResult);
    }
}
