package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedTracklistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.ViewGroup;

import java.util.ArrayList;

public class TracksByTag extends ScActivity implements SectionedEndlessAdapter.SectionListener {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tracks_by_tag);

        SectionedTracklistAdapter adp = new SectionedTracklistAdapter(this);
        SectionedEndlessAdapter adpWrap = new SectionedEndlessAdapter(this, adp, true);
        adpWrap.addListener(this);

        ScListView listView = new SectionedListView(this);
        configureList(listView);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(listView);

        adpWrap.configureViews(listView);
        adpWrap.setEmptyViewText(getResources().getString(R.string.empty_list));
        listView.setAdapter(adpWrap, true);

        Intent i = getIntent();
        if (i.hasExtra("tag")) {
            adp.sections.add(new SectionedAdapter.Section(String.format(getString(R.string.list_header_tracks_by_tag,
                    i.getStringExtra("tag"))), Track.class, new ArrayList<Parcelable>(),
                    Request.to(Endpoints.TRACKS).add("linked_partitioning", "1").add("tags", i.getStringExtra("tag"))));
        } else if (i.hasExtra("genre")) {
            adp.sections.add(new SectionedAdapter.Section(String.format(getString(R.string.list_header_tracks_by_genre,
                    i.getStringExtra("genre"))), Track.class, new ArrayList<Parcelable>(),
                    Request.to(Endpoints.TRACKS).add("linked_partitioning", "1").add("genres", i.getStringExtra("genre"))));
        } else throw new IllegalArgumentException("No tag or genre supplied with intent");
    }

    @Override
    public void onResume() {
        super.onResume();
        pageTrack("/tracks_by_tag");
    }

    @Override
    public void onSectionLoaded(SectionedAdapter.Section seection) {
    }
}
