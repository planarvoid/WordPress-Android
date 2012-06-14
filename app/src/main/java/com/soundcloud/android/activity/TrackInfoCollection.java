package com.soundcloud.android.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

public abstract class TrackInfoCollection extends ScListActivity implements SectionedEndlessAdapter.SectionListener, FetchTrackTask.FetchTrackListener {
    Track mTrack;
    TrackInfoBar mTrackInfoBar;
    SectionedListView mListView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.track_info_collection);

        mTrack = Track.fromIntent(getIntent(), getContentResolver());
        mTrackInfoBar = ((TrackInfoBar) findViewById(R.id.track_info_bar));
        mTrackInfoBar.display(mTrack, true, -1, true, getCurrentUserId());
        mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTrack(Playable.PlayInfo.forTracks(mTrack), true, false);
            }
        });

        SectionedAdapter adapter = createSectionedAdapter();
        SectionedEndlessAdapter adapterWrapper = new SectionedEndlessAdapter(this, adapter, true);
        adapterWrapper.addListener(this);

        mListView = new SectionedListView(this);
        configureList(mListView);
        mListView.setFadingEdgeLength(0);
        ((ViewGroup) findViewById(R.id.listHolder)).addView(mListView);

        adapterWrapper.configureViews(mListView);
        adapterWrapper.setEmptyViewText(R.string.empty_list);
        mListView.setAdapter(adapterWrapper, true);

        adapter.sections.add(createSection());

        if (!mTrack.full_track_info_loaded) {
            if (AndroidUtils.isTaskFinished(mTrack.load_info_task)) {
                mTrack.load_info_task = new FetchTrackTask(getApp(), mTrack.id);
            }
            mTrack.load_info_task.addListener(this);
            if (AndroidUtils.isTaskPending(mTrack.load_info_task)) {
                mTrack.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mTrack.id));
            }
        }

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            mListView.getWrapper().restoreState(mPreviousState);
        }
    }

    protected abstract SectionedAdapter createSectionedAdapter();

    abstract protected SectionedAdapter.Section createSection();

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mListView != null) {
            return  mListView.getWrapper().saveState();
        }
        return null;
    }

    @Override
    public void onSectionLoaded(SectionedAdapter.Section section) {
    }

    @Override
    public void onSuccess(Track track, String action) {
        ((TrackInfoBar) findViewById(R.id.track_info_bar)).display(track, true, -1, false, getCurrentUserId());
    }

    @Override
    public void onError(long trackId) {
    }

    @Override
    public void onDataConnectionChanged(boolean isConnected){
        super.onDataConnectionChanged(isConnected);
        if (isConnected) mTrackInfoBar.onConnected();
    }
}
