package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.TrackInfoBar;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public abstract class TrackInfoCollection extends ScActivity implements   FetchTrackTask.FetchTrackListener {
    Track mTrack;
    TrackInfoBar mTrackInfoBar;
    //SectionedListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_info_collection);

        mTrack = Track.fromIntent(getIntent(), getContentResolver());
        mTrackInfoBar = ((TrackInfoBar) findViewById(R.id.track_info_bar));
        mTrackInfoBar.display(mTrack, true, -1, true, getCurrentUserId());
        mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayUtils.playTrack(TrackInfoCollection.this, PlayInfo.forTracks(mTrack));
            }
        });


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.listHolder, ScListFragment.newInstance(getContentUri())).commit();
        }


        if (!mTrack.full_track_info_loaded) {
            if (AndroidUtils.isTaskFinished(mTrack.load_info_task)) {
                mTrack.load_info_task = new FetchTrackTask(getApp(), mTrack.id);
            }
            mTrack.load_info_task.addListener(this);
            if (AndroidUtils.isTaskPending(mTrack.load_info_task)) {
                mTrack.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mTrack.id));
            }
        }
    }

    protected abstract Uri getContentUri();

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
