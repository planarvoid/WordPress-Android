package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class TrackFavoriters extends ScActivity implements SectionedEndlessAdapter.SectionListener, LoadTrackInfoTask.LoadTrackInfoListener {

    Track mTrack;
    SectionedListView mListView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.track_favoriters);

        Intent i = getIntent();
        if (!i.hasExtra("track_id")) throw new IllegalArgumentException("No track id supplied with intent");
        mTrack = getApp().getTrackFromCache(i.getLongExtra("track_id", 0));

        // overly cautious, should never happen
        if (mTrack == null) return;

        ((TrackInfoBar) findViewById(R.id.track_info_bar)).display(mTrack, true, -1, true);
        findViewById(R.id.track_info_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTrack(mTrack, true);
            }
        });

        SectionedUserlistAdapter userAdapter = new SectionedUserlistAdapter(this);
        SectionedEndlessAdapter userAdapterWrapper = new SectionedEndlessAdapter(this, userAdapter, true);
        userAdapterWrapper.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);
        mListView.setFadingEdgeLength(0);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);


        userAdapterWrapper.configureViews(mListView);
        userAdapterWrapper.setEmptyViewText(getResources().getString(R.string.empty_list));
        mListView.setAdapter(userAdapterWrapper, true);

        userAdapter.sections.add(
                new SectionedAdapter.Section(getString(R.string.list_header_track_favoriters),
                        User.class, new ArrayList<Parcelable>(), Request.to(Endpoints.TRACK_FAVORITERS, mTrack.id)));

        if (!mTrack.info_loaded) {
            if (CloudUtils.isTaskFinished(mTrack.load_info_task)) {
                mTrack.load_info_task = new LoadTrackInfoTask(getApp(), mTrack.id, true, true);
            }

            mTrack.load_info_task.setListener(this);
            if (CloudUtils.isTaskPending(mTrack.load_info_task)) {
                mTrack.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mTrack.id));
            }
        }

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
        if (mTrack.user != null && TextUtils.isEmpty(mTrack.user.username)) {
            trackPage(mTrack.pageTrack("favorites"));
        }
    }

    @Override
    public void onSectionLoaded(SectionedAdapter.Section section) {
    }

    @Override
    public void onTrackInfoLoaded(Track track) {
        ((TrackInfoBar) findViewById(R.id.track_info_bar)).display(track, true, -1, false);
    }

    @Override
    public void onTrackInfoError(long trackId) {
    }
}
