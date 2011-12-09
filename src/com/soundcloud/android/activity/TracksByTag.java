package com.soundcloud.android.activity;

import com.soundcloud.android.Consts;
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
    private String mTrackingPath;
    private ScListView mListView;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tracks_by_tag);

        SectionedTracklistAdapter adp = new SectionedTracklistAdapter(this);
        SectionedEndlessAdapter adpWrap = new SectionedEndlessAdapter(this, adp, true);
        adpWrap.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);

        adpWrap.configureViews(mListView);
        adpWrap.setEmptyViewText(getResources().getString(R.string.empty_list));
        mListView.setAdapter(adpWrap, true);

        Intent i = getIntent();
        if (i.hasExtra("tag")) {
            adp.sections.add(new SectionedAdapter.Section(getString(R.string.list_header_tracks_by_tag,
                    i.getStringExtra("tag")), Track.class, new ArrayList<Parcelable>(),
                    null, Request.to(Endpoints.TRACKS).add("linked_partitioning", "1").add("tags", i.getStringExtra("tag"))));

            mTrackingPath = Consts.Tracking.TRACKS_BY_TAG + i.getStringExtra("tag");
        } else if (i.hasExtra("genre")) {
            adp.sections.add(new SectionedAdapter.Section(getString(R.string.list_header_tracks_by_genre,
                    i.getStringExtra("genre")), Track.class, new ArrayList<Parcelable>(),
                    null, Request.to(Endpoints.TRACKS).add("linked_partitioning", "1").add("genres", i.getStringExtra("genre"))));
            mTrackingPath = Consts.Tracking.TRACKS_BY_GENRE + i.getStringExtra("genre");
        } else throw new IllegalArgumentException("No tag or genre supplied with intent");

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mListView != null) {
            return  mListView.getWrapper().saveState();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        trackPage(mTrackingPath);
    }

    @Override
    public void onSectionLoaded(SectionedAdapter.Section seection) {
    }
}
