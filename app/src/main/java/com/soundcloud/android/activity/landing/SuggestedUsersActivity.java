package com.soundcloud.android.activity.landing;

import com.actionbarsherlock.view.MenuItem;
import com.soundcloud.android.R;
import com.soundcloud.android.fragment.SuggestedUsersCategoriesFragment;

import android.content.Intent;
import android.os.Bundle;

public class SuggestedUsersActivity extends SuggestedUsersBaseActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) {
            final SuggestedUsersCategoriesFragment suggestedUsersCategoriesFragment = new SuggestedUsersCategoriesFragment();
            suggestedUsersCategoriesFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.holder, suggestedUsersCategoriesFragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.finish) {
            startActivity(new Intent(this, SuggestedUsersSyncActivity.class));
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.suggested_users;
    }
}
