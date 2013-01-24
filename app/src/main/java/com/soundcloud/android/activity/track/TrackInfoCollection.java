package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.TrackInfoBar;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public abstract class TrackInfoCollection extends ScActivity implements FetchModelTask.Listener<Track> {
    Track mTrack;
    TrackInfoBar mTrackInfoBar;
    //SectionedListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_info_collection);

        mTrack = Track.fromIntent(getIntent(), getContentResolver());
        mTrackInfoBar = ((TrackInfoBar) findViewById(R.id.track_info_bar));
        mTrackInfoBar.display(mTrack, -1, true, true, true);
        mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if it comes from a mention, might not have a user
                if (mTrack.user != null) PlayUtils.playTrack(TrackInfoCollection.this, PlayInfo.forTracks(mTrack));
            }
        });


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.listHolder, ScListFragment.newInstance(getContentUri())).commit();
        }


        mTrack.refreshInfoAsync(getApp(),this);
    }

    //xxx hack
    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    protected abstract Uri getContentUri();

    @Override
    public void onSuccess(Track track) {
        ((TrackInfoBar) findViewById(R.id.track_info_bar)).display(track, -1, true, false, true);
    }

    @Override
    public void onError(Object context) {
    }

    @Override
    public void onDataConnectionChanged(boolean isConnected){
        super.onDataConnectionChanged(isConnected);
        if (isConnected) mTrackInfoBar.onConnected();
    }
}
