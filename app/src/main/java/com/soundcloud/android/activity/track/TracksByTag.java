package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class TracksByTag extends ScActivity {

    public static final String EXTRA_TAG = "tag";
    private static final String FILTER_TAG = "filter.tag";
    private static final String FILTER_GENRE = "filter.genre";
    public static final String EXTRA_GENRE = "genre";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracks_by_tag);

        Intent i = getIntent();
        if (savedInstanceState == null) {
            Uri contentUri = Content.TRACK_SEARCH.uri;
            if (i.hasExtra("tag")) {
                setTitle(getString(R.string.list_header_tracks_by_tag, i.getStringExtra(EXTRA_TAG)));
                contentUri = contentUri.buildUpon().appendQueryParameter(FILTER_TAG, i.getStringExtra(EXTRA_TAG)).build();
            } else if (i.hasExtra("genre")) {
                setTitle(getString(R.string.list_header_tracks_by_genre, i.getStringExtra(EXTRA_GENRE)));
                contentUri = contentUri.buildUpon().appendQueryParameter(FILTER_GENRE, i.getStringExtra(EXTRA_GENRE)).build();
            }
            getSupportFragmentManager().beginTransaction().add(R.id.listHolder, ScListFragment.newInstance(contentUri)).commit();
        }
    }

    //xxx hack
    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }


    @Override
    public void onResume() {
        super.onResume();
        // TODO TRACKING
    }


}
