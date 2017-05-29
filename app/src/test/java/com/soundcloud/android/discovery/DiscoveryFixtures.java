package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

class DiscoveryFixtures {
    private static final Urn SINGLE_SELECTION_URN = Urn.forSystemPlaylist("upload");
    private static final Urn MULTI_SELECTION_URN = Urn.forSystemPlaylist("chilling");
    private static final Urn SINGLE_ITEM_URN = Urn.forPlaylist(123);
    private static final Urn MULTI_ITEM_URN = Urn.forPlaylist(124);
    static final Optional<Urn> QUERY_URN = Optional.of(new Urn("soundcloud:discovery:123"));
    static final Optional<String> SINGLE_APP_LINK = Optional.of("soundcloud://playlists/123");
    static final Optional<String> MULTI_APP_LINK = Optional.of("soundcloud://playlists/124");
    static final Optional<String> SINGLE_WEB_LINK = Optional.of("www://soundcloud.com/playlists/123");
    static final Optional<String> MULTI_WEB_LINK = Optional.of("www://soundcloud.com/playlists/124");
    static final SelectionItem SINGLE_SELECTION_ITEM = SelectionItem.create(Optional.of(SINGLE_ITEM_URN),
                                                                            SINGLE_SELECTION_URN,
                                                                            Optional.absent(),
                                                                            Optional.absent(),
                                                                            Optional.absent(),
                                                                            Optional.absent(),
                                                                            Optional.absent(),
                                                                            SINGLE_APP_LINK,
                                                                            SINGLE_WEB_LINK);
    static final SelectionItem MULTI_SELECTION_ITEM = SelectionItem.create(Optional.of(MULTI_ITEM_URN),
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
                                                                                                                                          Optional.absent(),
                                                                                                                                          Optional.absent(),
                                                                                                                                          Optional.absent(),
                                                                                                                                          SINGLE_SELECTION_ITEM,
                                                                                                                                          Optional.absent(),
                                                                                                                                          Lists.newArrayList());
    static final DiscoveryCard.MultipleContentSelectionCard MULTI_CONTENT_SELECTION_CARD = DiscoveryCard.MultipleContentSelectionCard.create(MULTI_SELECTION_URN,
                                                                                                                                             QUERY_URN,
                                                                                                                                             Optional.absent(),
                                                                                                                                             Optional.absent(),
                                                                                                                                             Optional.absent(),
                                                                                                                                             Lists.newArrayList(MULTI_SELECTION_ITEM));
    static final DiscoveryCard SEARCH_ITEM = DiscoveryCard.forSearchItem();
}
