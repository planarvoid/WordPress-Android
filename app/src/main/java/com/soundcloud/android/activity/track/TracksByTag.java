package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.view.ScListView;

import android.os.Bundle;

public class TracksByTag extends ScListActivity {
    private ScListView mListView;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tracks_by_tag);

        /*
        SectionedAdapter adp = new SectionedAdapter(this);
        SectionedEndlessAdapter adpWrap = new SectionedEndlessAdapter(this, adp, true);
        adpWrap.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);

        adpWrap.configureViews(mListView);
        adpWrap.setEmptyViewText(R.string.empty_list);
        mListView.setAdapter(adpWrap, true);

        Intent i = getIntent();
        if (i.hasExtra("tag")) {
            adp.sections.add(new SectionedAdapter.Section(
                    getString(R.string.list_header_tracks_by_tag, i.getStringExtra("tag")),
                    Track.class,
                    new ArrayList<Parcelable>(),
                    null,
                    Request.to(Endpoints.TRACKS).add("linked_partitioning", "1").add("tags", i.getStringExtra("tag"))));
        } else if (i.hasExtra("genre")) {
            adp.sections.add(new SectionedAdapter.Section(
                    getString(R.string.list_header_tracks_by_genre, i.getStringExtra("genre")),
                    Track.class,
                    new ArrayList<Parcelable>(),
                    null,
                    Request.to(Endpoints.TRACKS).add("linked_partitioning", "1").add("genres", i.getStringExtra("genre"))));
        } else throw new IllegalArgumentException("No tag or genre supplied with intent");

        mPreviousState = (Object[]) getLastCustomNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
        */
    }

}
