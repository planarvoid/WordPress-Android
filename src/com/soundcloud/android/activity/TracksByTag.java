package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedTracklistAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class TracksByTag extends ScActivity implements SectionedEndlessAdapter.SectionListener {
    private ScListView mListView;
    private SectionedTracklistAdapter adp;
    private SectionedEndlessAdapter adpWrap;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tracks_by_tag);

        Intent i = getIntent();
        if (!i.hasExtra("tag")) throw new IllegalArgumentException("No tag supplied with intent");
        final String tag = i.getStringExtra("tag");

        //((TextView) findViewById(R.id.listHeader)).setText("Tracks that have the tag " + tag);

        adp = new SectionedTracklistAdapter(this);
        adpWrap = new SectionedEndlessAdapter(this, adp, true);
        adpWrap.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);

        adpWrap.configureViews(mListView);
        adpWrap.setEmptyViewText(getResources().getString(R.string.empty_list));
        mListView.setAdapter(adpWrap,true);

        adp.sections.add(
                new SectionedAdapter.Section(String.format(getString(R.string.list_header_tracks_by_tag, tag)),
                        Track.class, new ArrayList<Parcelable>(),
                        Request.to(Endpoints.TRACKS).add("tags",tag).add("linked_partitioning", "1")));

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
