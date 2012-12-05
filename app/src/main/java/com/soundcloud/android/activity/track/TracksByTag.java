package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.adapter.TrackInfoBar;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class TracksByTag extends ScActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracks_by_tag);

        Intent i = getIntent();


        if (savedInstanceState == null) {
            Uri contentUri = Content.TRACKS.uri;
            if (i.hasExtra("tag")) {
                setTitle(getString(R.string.list_header_tracks_by_tag, i.getStringExtra("tag")));
                contentUri = contentUri.buildUpon().appendQueryParameter("tags",i.getStringExtra("tag")).build();
            }else if (i.hasExtra("genre")) {
                setTitle(getString(R.string.list_header_tracks_by_genre, i.getStringExtra("genre")));
                contentUri = contentUri.buildUpon().appendQueryParameter("genres",i.getStringExtra("genre")).build();
            }
            getSupportFragmentManager().beginTransaction().add(R.id.listHolder,ScListFragment.newInstance(contentUri)).commit();
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
