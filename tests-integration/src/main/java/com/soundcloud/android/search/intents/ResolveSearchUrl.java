package com.soundcloud.android.search.intents;

import com.soundcloud.android.screens.search.PlaylistTagsScreen;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchUrl extends SearchIntentsBase {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/search/sounds"));
    }

    public void testSearchUrlResolution() {
        PlaylistTagsScreen tagsScreen = new PlaylistTagsScreen(solo);
        assertEquals("Playlist tags screen should be visible", true, tagsScreen.isVisible());
    }

}
