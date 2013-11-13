package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.storage.provider.Content;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class TracksByTag extends ScActivity {

    public static final String EXTRA_TAG = "tag";
    private static final String FILTER_TAG = "filter.tag";
    private static final String FILTER_GENRE = "filter.genre";
    public static final String EXTRA_GENRE = "genre";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        if (savedInstanceState == null) {
            Uri contentUri = Content.TRACK_SEARCH.uri;
            if (i.hasExtra("tag")) {
                setTitle(getString(R.string.list_header_tracks_by_tag, i.getStringExtra(EXTRA_TAG)));
                //TODO: discovery currently have a server side bug where searching for upper case tags results in 503s
                final String searchTag = i.getStringExtra(EXTRA_TAG).toLowerCase();
                contentUri = contentUri.buildUpon().appendQueryParameter(FILTER_TAG, searchTag).build();
            } else if (i.hasExtra("genre")) {
                setTitle(getString(R.string.list_header_tracks_by_genre, i.getStringExtra(EXTRA_GENRE)));
                contentUri = contentUri.buildUpon().appendQueryParameter(FILTER_GENRE, i.getStringExtra(EXTRA_GENRE)).build();
            }
            getSupportFragmentManager().beginTransaction().add(getContentHolderViewId(), ScListFragment.newInstance(contentUri)).commit();
        }
    }
}
