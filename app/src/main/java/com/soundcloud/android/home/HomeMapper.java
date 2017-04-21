package com.soundcloud.android.home;

import com.soundcloud.java.collections.Lists;

class HomeMapper {

    static HomeCard map(ApiHomeCard apiHomeCard) {
        if (apiHomeCard.selectionCard().isPresent()) {
            final ApiSelectionCard apiSelectionCard = apiHomeCard.selectionCard().get();
            return HomeCard.SelectionCard.create(apiSelectionCard.selectionUrn(),
                                                 apiSelectionCard.queryUrn(),
                                                 apiSelectionCard.style(),
                                                 apiSelectionCard.title(),
                                                 apiSelectionCard.description(),
                                                 apiSelectionCard.selectionPlaylists().transform(apiSelectionPlaylists -> Lists.transform(apiSelectionPlaylists.getCollection(), HomeMapper::map)));
        } else if (apiHomeCard.singletonSelectionCard().isPresent()) {
            final ApiSingletonSelectionCard apiSingletonSelectionCard = apiHomeCard.singletonSelectionCard().get();
            return HomeCard.SingletonSelectionCard.create(apiSingletonSelectionCard.selectionUrn(),
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
