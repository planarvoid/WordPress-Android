package com.soundcloud.android.discovery;

import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

class DiscoveryFixtures {
    private static final Urn SINGLE_SELECTION_URN = Urn.forSystemPlaylist("upload");
    private static final Urn MULTI_SELECTION_URN = Urn.forSystemPlaylist("chilling");
    private static final Urn SINGLE_ITEM_URN = Urn.forPlaylist(123);
    private static final Urn MULTI_ITEM_URN = Urn.forPlaylist(124);
    static final Optional<Urn> QUERY_URN = Optional.of(new Urn("soundcloud:discovery:123"));
    static final Optional<Urn> PARENT_QUERY_URN = Optional.of(new Urn("soundcloud:discovery:123"));
    static final Optional<String> SINGLE_APP_LINK = Optional.of("soundcloud://playlists/123");
    static final Optional<String> MULTI_APP_LINK = Optional.of("soundcloud://playlists/124");
    static final Optional<String> SINGLE_WEB_LINK = Optional.of("www://soundcloud.com/playlists/123");
    static final Optional<String> MULTI_WEB_LINK = Optional.of("www://soundcloud.com/playlists/124");
    static final SelectionItem SINGLE_SELECTION_ITEM = new SelectionItem(Optional.of(SINGLE_ITEM_URN),
                                                                         SINGLE_SELECTION_URN,
                                                                         Optional.absent(),
                                                                         Optional.absent(),
                                                                         Optional.absent(),
                                                                         Optional.absent(),
                                                                         Optional.absent(),
                                                                         SINGLE_APP_LINK,
                                                                         SINGLE_WEB_LINK);
    static final SelectionItem MULTI_SELECTION_ITEM = new SelectionItem(Optional.of(MULTI_ITEM_URN),
                                                                        MULTI_SELECTION_URN,
                                                                        Optional.absent(),
                                                                        Optional.absent(),
                                                                        Optional.absent(),
                                                                        Optional.absent(),
                                                                        Optional.absent(),
                                                                        MULTI_APP_LINK,
                                                                        MULTI_WEB_LINK);
    static final DiscoveryCard.SingleContentSelectionCard SINGLE_CONTENT_SELECTION_CARD = DiscoveryCard.SingleContentSelectionCard.create(SINGLE_SELECTION_URN,
                                                                                                                                          QUERY_URN,
                                                                                                                                          PARENT_QUERY_URN,
                                                                                                                                          Optional.absent(),
                                                                                                                                          Optional.absent(),
                                                                                                                                          Optional.absent(),
                                                                                                                                          Optional.absent(),
                                                                                                                                          SINGLE_SELECTION_ITEM,
                                                                                                                                          Optional.absent(),
                                                                                                                                          Lists.newArrayList());
    static final DiscoveryCard.MultipleContentSelectionCard MULTI_CONTENT_SELECTION_CARD = DiscoveryCard.MultipleContentSelectionCard.create(MULTI_SELECTION_URN,
                                                                                                                                             QUERY_URN,
                                                                                                                                             PARENT_QUERY_URN,
                                                                                                                                             Optional.absent(),
                                                                                                                                             Optional.absent(),
                                                                                                                                             Optional.absent(),
                                                                                                                                             Optional.absent(),
                                                                                                                                             Lists.newArrayList(MULTI_SELECTION_ITEM));
    static final DiscoveryCard SEARCH_ITEM = DiscoveryCard.forSearchItem();
    static final DiscoveryCardViewModel SEARCH_ITEM_VIEW_MODEL = DiscoveryCardViewModel.SearchCard.INSTANCE;

    static DiscoveryCardViewModel.SingleContentSelectionCard singleContentSelectionCardViewModelWithSelectionItem(SelectionItemViewModel selectionItem) {
        return new DiscoveryCardViewModel.SingleContentSelectionCard(SINGLE_CONTENT_SELECTION_CARD, selectionItem);
    }

    static DiscoveryCardViewModel.SingleContentSelectionCard singleContentSelectionCardViewModel() {
        return new DiscoveryCardViewModel.SingleContentSelectionCard(SINGLE_CONTENT_SELECTION_CARD, new SelectionItemViewModel(SINGLE_CONTENT_SELECTION_CARD.selectionItem(), Optional.absent()));
    }

    static DiscoveryCardViewModel.MultipleContentSelectionCard multiContentSelectionCardViewModel() {
        return new DiscoveryCardViewModel.MultipleContentSelectionCard(MULTI_CONTENT_SELECTION_CARD,
                                                                       Lists.transform(MULTI_CONTENT_SELECTION_CARD.selectionItems(), item -> new SelectionItemViewModel(item, Optional.absent())));
    }

    static SelectionItemViewModel singleSelectionItemViewModel(SelectionItemViewModel.TrackingInfo trackingInfo) {
        return new SelectionItemViewModel(SINGLE_SELECTION_ITEM, Optional.of(trackingInfo));
    }

    static SelectionItemViewModel multiSelectionItemViewModel(SelectionItemViewModel.TrackingInfo trackingInfo) {
        return new SelectionItemViewModel(MULTI_SELECTION_ITEM, Optional.of(trackingInfo));
    }
}
