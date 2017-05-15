package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryReadableStorageTest
{
    @Mock DiscoveryDatabase discoveryDatabase;
    private DiscoveryReadableStorage discoveryReadableStorage;

    @Before
    public void setUp() throws Exception {
        discoveryReadableStorage = new DiscoveryReadableStorage(discoveryDatabase);
    }

    @Test
    public void retrievesCard() throws Exception {
        final Urn cardUrn = new Urn("soundcloud:system_playlist:123");
        final long cardId = 1;
        initDiscoveryCardTable(cardUrn, cardId);
        final Urn selectionItemUrn = new Urn("soundcloud:playlist:123");
        initSelectionItemTable(cardId, selectionItemUrn);

        final List<DiscoveryCard> discoveryCards = discoveryReadableStorage.discoveryCards().test().assertValueCount(1).values().get(0);

        assertThat(discoveryCards).hasSize(1);
        assertThat(discoveryCards.get(0).kind()).isEqualTo(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD);
        final DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard = (DiscoveryCard.SingleContentSelectionCard) discoveryCards.get(0);
        assertThat(singleContentSelectionCard.selectionUrn()).isEqualTo(cardUrn);
    }

    private void initSelectionItemTable(Long cardId, Urn selectionItemUrn) {
        final DbModel.SelectionItem selectionItem = DbModel.SelectionItem.CREATOR.create(1, cardId, selectionItemUrn, null, null, null, null, null, null);
        when(discoveryDatabase.selectList(DbModel.SelectionItem.SELECT_ALL_FOR_DISCOVERY_CARDS, DbModel.SelectionItem.MAPPER)).thenReturn(Single.just(Lists.newArrayList(selectionItem)));
    }

    private void initDiscoveryCardTable(Urn cardUrn, long cardId) {
        final DbModel.SingleContentSelectionCard singleContentSelectionDbCard = DbModel.SingleContentSelectionCard.CREATOR.create(cardId,
                                                                                                                                  cardUrn,
                                                                                                                                  null,
                                                                                                                                  null,
                                                                                                                                  null,
                                                                                                                                  null,
                                                                                                                                  null,
                                                                                                                                  Lists.newArrayList());
        final DbModel.FullDiscoveryCard card = DbModel.FullDiscoveryCard.CREATOR.create(singleContentSelectionDbCard, null);

        when(discoveryDatabase.selectList(DbModel.DiscoveryCard.SELECT_ALL, DbModel.FullDiscoveryCard.MAPPER)).thenReturn(Single.just(Lists.newArrayList(card)));
    }
}
