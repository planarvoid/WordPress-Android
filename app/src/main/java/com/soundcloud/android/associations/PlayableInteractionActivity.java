package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.playback.views.PlayablePresenter;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public abstract class PlayableInteractionActivity extends ScActivity {

    public static final String EXTRA_INTERACTION_TYPE = "com.soundcloud.android.activity_type";

    protected Activity.Type mInteraction;
    protected Playable mPlayable;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.playable_interaction_activity);

        if (!getIntent().hasExtra(EXTRA_INTERACTION_TYPE)) {
            throw new IllegalArgumentException("need to pass in EXTRA_INTERACTION_TYPE");
        }

        mInteraction = (Activity.Type) getIntent().getSerializableExtra(EXTRA_INTERACTION_TYPE);
        mPlayable = getPlayableFromIntent(getIntent());

        new PlayablePresenter(this)
                .setPlayableRowView(findViewById(R.id.playable_bar))
                .setArtwork((ImageView) findViewById(R.id.icon), ImageSize.getListItemImageSize(this))
                .setPlayable(mPlayable);


        if (bundle == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.listHolder, ScListFragment.newInstance(getContentUri(), getCurrentScreen())).commit();
        }
    }

    protected abstract Screen getCurrentScreen();

    //xxx hack
    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    protected abstract Uri getContentUri();

    protected abstract Playable getPlayableFromIntent(Intent intent);
}
