package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.view.WorkspaceView;
import com.soundcloud.android.view.tour.Comment;
import com.soundcloud.android.view.tour.Finish;
import com.soundcloud.android.view.tour.Follow;
import com.soundcloud.android.view.tour.Record;
import com.soundcloud.android.view.tour.Share;
import com.soundcloud.android.view.tour.Start;
import com.soundcloud.android.view.tour.TourLayout;

import android.app.Activity;
import android.os.Bundle;

public class Tour extends Activity {

    private WorkspaceView mWorkspaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tour);

        mWorkspaceView = (WorkspaceView) findViewById(R.id.tour_view);
        mWorkspaceView.addView(new Start(this));
        mWorkspaceView.addView(new Record(this));
        mWorkspaceView.addView(new Share(this));
        mWorkspaceView.addView(new Follow(this));
        mWorkspaceView.addView(new Comment(this));
        mWorkspaceView.addView(new Finish(this));
        mWorkspaceView.initWorkspace(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication) getApplication()).trackPage("/tour/" +
                ((TourLayout) mWorkspaceView.getScreenAt(mWorkspaceView.getCurrentScreen())).getClass().getSimpleName());
    }
}
