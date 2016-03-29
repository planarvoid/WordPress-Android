package com.soundcloud.android.search.suggestions;

abstract class SuggestionItem {

    enum Kind {
        SearchItem, TrackItem, UserItem
    }

    private final Kind kind;

    SuggestionItem(Kind kind) {
        this.kind = kind;
    }

    Kind getKind() {
        return kind;
    }
}
