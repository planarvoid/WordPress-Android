package com.soundcloud.android.activity.landing;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class SuggestedUsers extends ScActivity implements ScLandingPage{
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_suggested_users));

        int listHolderId;
        if (getIntent().getBooleanExtra(Consts.Keys.WAS_SIGNUP,false)){
            setContentView(R.layout.suggested_users_onboard);
            listHolderId = R.id.list_holder;
            findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LocalCollection.forceToStale(Content.ME_SOUND_STREAM.uri, getContentResolver());
                    startActivity(new Intent(Actions.STREAM));
                    finish();
                }
            });

        } else {
            listHolderId = mRootView.getContentHolderId();
        }

        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(listHolderId, ScListFragment.newInstance(Content.SUGGESTED_USERS))
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
        return R.id.nav_suggested_users;
    }
}
