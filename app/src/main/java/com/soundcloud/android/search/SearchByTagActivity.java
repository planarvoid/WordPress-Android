package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.provider.Content;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class SearchByTagActivity extends ScActivity {

    public static final String EXTRA_TAG = "tag";
    private static final String FILTER_KEY = "filter.genre_or_tag";
    public static final String EXTRA_GENRE = "genre";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        if (savedInstanceState == null) {

            Uri.Builder contentUriBuilder = Content.TRACK_SEARCH.uri.buildUpon().appendQueryParameter("q", "*");
            if (i.hasExtra("tag")) {
                final String tag = i.getStringExtra(EXTRA_TAG);
                setTitle(getString(R.string.list_header_tracks_by_tag, tag));
                addFragment(contentUriBuilder.appendQueryParameter(FILTER_KEY, tag).build());

            } else if (i.hasExtra("genre")) {
                final String genre = i.getStringExtra(EXTRA_GENRE);
                setTitle(getString(R.string.list_header_tracks_by_genre, genre));
                addFragment(contentUriBuilder.appendQueryParameter(FILTER_KEY, genre).build());
            }
        }
    }

    private void addFragment(Uri contentUri) {
        getSupportFragmentManager().beginTransaction().add(getContentHolderViewId(), ScListFragment.newInstance(contentUri, Screen.SEARCH_BY_TAG)).commit();
    }
}
