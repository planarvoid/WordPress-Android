package com.soundcloud.android.discovery;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

class Fixtures {
    private static int idCounter = 0;

    private static final String ARTWORK = "https://i1.sndcdn.com/artworks-000148470532-wq5g7k-{size}.jpg";
    private static final int TRACK_COUNT = 9;
    private static final String SHORT_TITLE = "Chill Playlist";
    private static final String URN_VALUE = "soundcloud:playlists:%d";
    private static final String SHORT_SUBTITLE = "willywacker";
    private static final String WEB_LINK = "http://soundcloud.com/system_playlist/soundcloud:playlists:96836877";
    private static final String APP_LINK = "soundcloud://system_playlist/soundcloud:playlists:96836877";

    private static class MultipleSelectionCard {

        static final String SELECTION_URN = "soundcloud:selections:the-upload";
        static final String QUERY_URN = "soundcloud:queries:3rgt3trbg3b3t3rbt3r";
        static final String STYLE = "go_plus";
        static final String TITLE = "Playlists for Chilling";
        static final String DESCRIPTION = "Some \uD83D\uDEC0\uD83C\uDF34\uD83C\uDF0A marketing copy goes here.";

    }

    private static class SingleSelectionCard {

        static final String SELECTION_URN = "soundcloud:selections:new-release:soundcloud:playlists:%d";
        static final String QUERY_URN = "soundcloud:queries:%d";
        static final String STYLE = "go_plus";
        static final String TITLE = "New Release from Little Simz";
        static final String DESCRIPTION = "Stillness In Wonderland";
        static final String AVATAR_URL = "https://i1.sndcdn.com/artworks-000136596659-7rdy0i-{size}.jpg";

    }

    public static List<ApiDiscoveryCard> discoveryCards(int size) {
        final List<ApiDiscoveryCard> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(i % 2 == 0 ? singleContentSelectionDiscoveryCard() : multipleContentSelectionDiscoveryCard());
        }
        return result;
    }

    private static ApiDiscoveryCard singleContentSelectionDiscoveryCard() {
        return ApiDiscoveryCard.create(singleContentSelectionCard(), null);
    }

    private static ApiDiscoveryCard multipleContentSelectionDiscoveryCard() {
        return ApiDiscoveryCard.create(null, multipleContentSelectionCard());
    }

    private static ApiMultipleContentSelectionCard multipleContentSelectionCard() {
        return ApiMultipleContentSelectionCard.create(new Urn(MultipleSelectionCard.SELECTION_URN),
                                                      MultipleSelectionCard.STYLE,
                                                      MultipleSelectionCard.TITLE,
                                                      MultipleSelectionCard.DESCRIPTION,
                                                      new ModelCollection<>(Lists.newArrayList(expectedPlaylist()),
                                                                            Collections
                                                                                    .emptyMap(),
                                                                            new Urn(MultipleSelectionCard.QUERY_URN)));
    }

    private static ApiSingleContentSelectionCard singleContentSelectionCard() {
        return ApiSingleContentSelectionCard.create(incrementalUrn(SingleSelectionCard.SELECTION_URN),
                                                    incrementalUrn(SingleSelectionCard.QUERY_URN),
                                                    SingleSelectionCard.STYLE,
                                                    SingleSelectionCard.TITLE,
                                                    SingleSelectionCard.DESCRIPTION,
                                                    null,
                                                    expectedPlaylist(),
                                                    Lists.newArrayList(SingleSelectionCard.AVATAR_URL));
    }

    private static ApiSelectionItem expectedPlaylist() {
        return ApiSelectionItem.create(incrementalUrn(URN_VALUE), ARTWORK, TRACK_COUNT, SHORT_TITLE, SHORT_SUBTITLE, WEB_LINK, APP_LINK);
    }

    private static Urn incrementalUrn(String urnValue) {
        return new Urn(String.format(Locale.getDefault(), urnValue, id()));
    }

    private static int id() {
        idCounter++;
        return idCounter;
    }
}
