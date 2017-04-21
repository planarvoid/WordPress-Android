package com.soundcloud.android.home;

import com.soundcloud.java.collections.Lists;

class DiscoveryCardMapper {

    static DiscoveryCard map(ApiDiscoveryCard apiDiscoveryCard) {
        if (apiDiscoveryCard.selectionCard().isPresent()) {
            final ApiSelectionCard apiSelectionCard = apiDiscoveryCard.selectionCard().get();
            return DiscoveryCard.SelectionCard.create(apiSelectionCard.selectionUrn(),
                                                 apiSelectionCard.queryUrn(),
                                                 apiSelectionCard.style(),
                                                 apiSelectionCard.title(),
                                                 apiSelectionCard.description(),
                                                 apiSelectionCard.selectionPlaylists().transform(apiSelectionPlaylists -> Lists.transform(apiSelectionPlaylists.getCollection(), DiscoveryCardMapper::map)));
        } else if (apiDiscoveryCard.singletonSelectionCard().isPresent()) {
            final ApiSingletonSelectionCard apiSingletonSelectionCard = apiDiscoveryCard.singletonSelectionCard().get();
            return DiscoveryCard.SingletonSelectionCard.create(apiSingletonSelectionCard.selectionUrn(),
                                                          apiSingletonSelectionCard.queryUrn(),
                                                          apiSingletonSelectionCard.style(),
                                                          apiSingletonSelectionCard.title(),
                                                          apiSingletonSelectionCard.description(),
                                                          map(apiSingletonSelectionCard.selectionPlaylist()),
                                                          apiSingletonSelectionCard.socialProof(),
                                                          apiSingletonSelectionCard.socialProofAvatarUrlTemplates());
        } else {
            throw new IllegalStateException("Unexpected card type");
        }
    }

    private static SelectionPlaylist map(ApiSelectionPlaylist apiSelectionPlaylist) {
        return SelectionPlaylist.create(apiSelectionPlaylist.urn(),
                                        apiSelectionPlaylist.artworkUrlTemplate(),
                                        apiSelectionPlaylist.trackCount(),
                                        apiSelectionPlaylist.shortTitle(),
                                        apiSelectionPlaylist.shortSubtitle());
    }
}
