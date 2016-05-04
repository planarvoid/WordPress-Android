package com.soundcloud.android.search.suggestions;

class SuggestionHighlight {
    private final int start;
    private final int end;

    SuggestionHighlight(int start, int end) {
        this.start = start;
        this.end = end;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }
}
