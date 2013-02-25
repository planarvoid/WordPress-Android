package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.view.adapter.PlayableBar;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public abstract class PlayableInteractionActivity extends ScActivity {

    public static final String EXTRA_INTERACTION_TYPE = "com.soundcloud.android.activity_type";

    protected Activity.Type mInteraction;
    protected Playable mPlayable;
    protected PlayableBar mPlayableInfoBar;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.track_info_collection);

        if (!getIntent().hasExtra(EXTRA_INTERACTION_TYPE)) {
            throw new IllegalArgumentException("need to pass in EXTRA_INTERACTION_TYPE");
        }

        mInteraction = (Activity.Type) getIntent().getSerializableExtra(EXTRA_INTERACTION_TYPE);
        mPlayable = getPlayableFromIntent(getIntent());

        mPlayableInfoBar = ((PlayableBar) findViewById(R.id.playable_bar));
        mPlayableInfoBar.display(mPlayable);

        if (bundle == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.listHolder, ScListFragment.newInstance(getContentUri())).commit();
        }
    }

    //xxx hack
    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    protected abstract Uri getContentUri();

    protected abstract Playable getPlayableFromIntent(Intent intent);

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    @Override
    public void onDataConnectionChanged(boolean isConnected){
        super.onDataConnectionChanged(isConnected);
        if (isConnected) mPlayableInfoBar.onConnected();
    }
}
