package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.propeller.PropertySet;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public abstract class PlayableInteractionActivity extends ScActivity {

    public static final String PROPERTY_SET_EXTRA = "extra";
    public static final String EXTRA_INTERACTION_TYPE = "com.soundcloud.android.activity_type";

    protected Activity.Type interaction;
    protected PropertySet propertySet;

    @Inject PlayablePresenter playablePresenter;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.playable_interaction_activity);

        if (!getIntent().hasExtra(EXTRA_INTERACTION_TYPE)) {
            throw new IllegalArgumentException("need to pass in EXTRA_INTERACTION_TYPE");
        }

        interaction = (Activity.Type) getIntent().getSerializableExtra(EXTRA_INTERACTION_TYPE);
        propertySet =  getIntent().getParcelableExtra(PROPERTY_SET_EXTRA);
        playablePresenter.setPlayableRowView(findViewById(R.id.playable_bar))
                .setArtwork((ImageView) findViewById(R.id.icon), ApiImageSize.getListItemImageSize(this))
                .setPlayable(propertySet);

        if (bundle == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.listHolder, ScListFragment.newInstance(getContentUri(), getCurrentScreen())).commit();
        }
    }

    //xxx hack
    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    protected abstract Screen getCurrentScreen();

    protected abstract Uri getContentUri();
}
