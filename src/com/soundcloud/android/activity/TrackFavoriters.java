package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.NewConnectionTask;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class TrackFavoriters extends ScActivity implements SectionedEndlessAdapter.SectionListener {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.track_favoriters);

        Intent i = getIntent();
        if (!i.hasExtra("track")) throw new IllegalArgumentException("No track supplied with intent");
        final Track track = i.getParcelableExtra("track");

        ((TrackInfoBar) findViewById(R.id.track_info_bar)).display(track, true, -1, false);
        findViewById(R.id.track_info_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTrack(track, true);
            }
        });

        SectionedUserlistAdapter userAdapter = new SectionedUserlistAdapter(this);
        SectionedEndlessAdapter userAdapterWrapper = new SectionedEndlessAdapter(this, userAdapter, true);
        userAdapterWrapper.addListener(this);

        ScListView listView = new SectionedListView(this);
        configureList(listView);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(listView);

        userAdapterWrapper.configureViews(listView);
        userAdapterWrapper.setEmptyViewText(getResources().getString(R.string.empty_list));
        listView.setAdapter(userAdapterWrapper, true);

        //TODO real endpoint
        userAdapter.sections.add(
                new SectionedAdapter.Section(getString(R.string.list_header_track_favoriters),
                        User.class, new ArrayList<Parcelable>(), Request.to("/tracks/%d/favoriters", track.id)));
    }

    @Override
    public void onResume() {
        super.onResume();
        pageTrack("/track_favoriters");
    }

    @Override
    public void onSectionLoaded(SectionedAdapter.Section section) {
    }
}
