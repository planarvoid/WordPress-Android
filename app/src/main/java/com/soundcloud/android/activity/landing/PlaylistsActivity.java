package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class PlaylistsActivity extends ScActivity implements ScLandingPage{
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_playlists));
        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content_frame, ScListFragment.newInstance(Content.ME_PLAYLISTS))
                    .commit();
        }
    }

    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_playlists;
    }
}