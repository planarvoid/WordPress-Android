package com.soundcloud.android.discovery;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.view.ViewError;
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

    private SelectionItem selectionItem = new SelectionItem(Optional.absent(),
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
                           .assertValue(new DiscoveryResult(discoveryCards, Optional.absent()));
    }

    @Test
    public void discoveryCardsReturnsCardsFromStorageAfterSyncFailure() throws Exception {
        final SyncFailedException throwable = new SyncFailedException();
        setUpDiscoveryCards(SyncResult.error(throwable), Maybe.just(discoveryCards));

        discoveryOperations.discoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(new DiscoveryResult(discoveryCards, Optional.of(ViewError.CONNECTION_ERROR)));
    }

    @Test
    public void discoveryCardsReturnsEmptyCardWithErrorAfterSyncFailureAndStorageIsEmpty() throws Exception {
        final SyncFailedException throwable = new SyncFailedException();
        setUpDiscoveryCards(SyncResult.error(throwable), Maybe.empty());

        discoveryOperations.discoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(new DiscoveryResult(Lists.newArrayList(DiscoveryCard.EmptyCard.create(Optional.of(throwable))), Optional.of(ViewError.CONNECTION_ERROR)));
    }

    @Test
    public void discoveryCardsPerformsRefreshIfSyncWasSuccessfulButResultContainsAnEmptyCard() throws Exception {
        final List<DiscoveryCard> emptyCardList = discoveryCards.subList(0, discoveryCards.size() - 1);
        emptyCardList.add(DiscoveryCard.EmptyCard.create(Optional.absent()));
        setUpDiscoveryCards(SyncResult.synced(), Maybe.just(emptyCardList));
        setUpRefreshDiscoveryCards(SyncResult.synced(), Maybe.just(discoveryCards));

        discoveryOperations.discoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(new DiscoveryResult(discoveryCards, Optional.absent()));

        verify(syncOperations).sync(Syncable.DISCOVERY_CARDS);
    }

    @Test
    public void refreshDiscoveryCardsReturnsCardsFromStorageAfterSyncSuccess() throws Exception {
        setUpRefreshDiscoveryCards(SyncResult.synced(), Maybe.just(discoveryCards));

        discoveryOperations.refreshDiscoveryCards()
                           .test()
                           .assertValue(new DiscoveryResult(discoveryCards, Optional.absent()));
    }

    @Test
    public void refreshDiscoveryCardsReturnsCardsFromStorageAfterSyncFailure() throws Exception {
        final SyncFailedException throwable = new SyncFailedException();
        setUpRefreshDiscoveryCards(SyncResult.error(throwable), Maybe.just(discoveryCards));

        discoveryOperations.refreshDiscoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(new DiscoveryResult(discoveryCards, Optional.of(ViewError.CONNECTION_ERROR)));
    }

    @Test
    public void refreshDiscoveryCardsReturnsEmptyCardWithErrorAfterSyncFailureAndStorageIsEmpty() throws Exception {
        final SyncFailedException throwable = new SyncFailedException();
        SyncResult syncResult = SyncResult.error(throwable);
        setUpRefreshDiscoveryCards(syncResult, Maybe.empty());

        discoveryOperations.refreshDiscoveryCards()
                           .test()
                           .assertNoErrors()
                           .assertValue(new DiscoveryResult(Lists.newArrayList(DiscoveryCard.EmptyCard.create(Optional.of(throwable))), Optional.of(ViewError.CONNECTION_ERROR)));
    }

    private void setUpDiscoveryCards(SyncResult syncResult, Maybe<List<DiscoveryCard>> storageResult) {
        when(syncOperations.syncIfStale(discoveryCardsSyncable)).thenReturn(Single.just(syncResult));
        when(storage.discoveryCards()).thenReturn(storageResult);
    }

    private void setUpRefreshDiscoveryCards(SyncResult syncResult, Maybe<List<DiscoveryCard>> storageResult) {
        when(syncOperations.sync(discoveryCardsSyncable)).thenReturn(Single.just(syncResult));
        when(storage.discoveryCards()).thenReturn(storageResult);
    }
}
