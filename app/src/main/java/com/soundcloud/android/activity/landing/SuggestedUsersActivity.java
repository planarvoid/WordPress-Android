package com.soundcloud.android.activity.landing;

import com.actionbarsherlock.view.MenuItem;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.SuggestedUsersCategoriesFragment;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.SyncStateManager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class SuggestedUsersActivity extends ScActivity implements ScLandingPage {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_suggested_users));
        setContentView(R.layout.suggested_users_activity);

        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.categories_fragment_holder, new SuggestedUsersCategoriesFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.finish) {
            new SyncStateManager().forceToStale(Content.ME_SOUND_STREAM);
            startActivity(new Intent(Actions.STREAM));
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.suggested_users;
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_suggested_users;
    }
}
