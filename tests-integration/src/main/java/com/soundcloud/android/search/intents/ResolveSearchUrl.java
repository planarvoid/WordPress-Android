package com.soundcloud.android.search.intents;

import com.soundcloud.android.screens.search.SearchPlaylistTagsScreen;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchUrl extends SearchIntentsBase {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/search/sounds"));
    }

    public void testSearchUrlResolution() {
        SearchPlaylistTagsScreen tagsScreen = new SearchPlaylistTagsScreen(solo);
        assertEquals("Playlist tags screen should be visible", true, tagsScreen.isVisible());
    }

}
