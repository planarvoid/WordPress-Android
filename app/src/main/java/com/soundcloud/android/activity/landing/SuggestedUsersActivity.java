package com.soundcloud.android.activity.landing;

import com.actionbarsherlock.view.MenuItem;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.dialog.OnboardSuggestedUsersSyncFragment;
import com.soundcloud.android.fragment.SuggestedUsersCategoriesFragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class SuggestedUsersActivity extends ScActivity implements ScLandingPage {

    public static final String SYNC_DIALOG_FRAGMENT = "sync_dialog_fragment";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.categories_fragment_holder, new OnboardSuggestedUsersSyncFragment())
                    .commit();
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
