package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;

import android.content.res.Resources;

public enum SearchType {
    ALL(R.string.search_type_all, Screen.SEARCH_EVERYTHING, true),
    TRACKS(R.string.search_type_tracks, Screen.SEARCH_TRACKS, false),
    USERS(R.string.search_type_people, Screen.SEARCH_USERS, false),
    ALBUMS(R.string.search_type_albums, Screen.SEARCH_ALBUMS, false),
    PLAYLISTS(R.string.search_type_playlists, Screen.SEARCH_PLAYLISTS, false);

    private final int resource;
    private final Screen screen;
    private final boolean publishSearchSubmissionEvent;

    SearchType(int resource, Screen screen, boolean publishSearchSubmissionEvent) {
        this.resource = resource;
        this.screen = screen;
        this.publishSearchSubmissionEvent = publishSearchSubmissionEvent;
    }

    String getPageTitle(Resources resources) {
        return resources.getString(resource);
    }

    boolean shouldPublishSearchSubmissionEvent() {
        return publishSearchSubmissionEvent;
    }

    Screen getScreen() {
        return screen;
    }
}
