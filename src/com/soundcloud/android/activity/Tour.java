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
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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

        final Button btnDone = (Button) findViewById(R.id.btn_done);
        final Button btnDoneDark = (Button) findViewById(R.id.btn_done_dark);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnDoneDark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mWorkspaceView.setOnScreenChangeListener(new WorkspaceView.OnScreenChangeListener() {
            @Override
            public void onScreenChanged(View newScreen, int newScreenIndex) {
                ((RadioButton) ((RadioGroup) findViewById(R.id.rdo_tour_step)).getChildAt(newScreenIndex)).setChecked(true);
                if (newScreenIndex < mWorkspaceView.getScreenCount()-1){
                    btnDone.setVisibility(View.GONE);
                    btnDoneDark.setVisibility(View.VISIBLE);
                } else {
                    btnDone.setVisibility(View.VISIBLE);
                    btnDoneDark.setVisibility(View.GONE);
                }

            }

            @Override
            public void onScreenChanging(View newScreen, int newScreenIndex) {
            }

            @Override
            public void onNextScreenVisible(View newScreen, int newScreenIndex) {
            }
        }, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication) getApplication()).trackPage("/tour/" +
                ((TourLayout) mWorkspaceView.getScreenAt(mWorkspaceView.getCurrentScreen())).getClass().getSimpleName());
    }
}
