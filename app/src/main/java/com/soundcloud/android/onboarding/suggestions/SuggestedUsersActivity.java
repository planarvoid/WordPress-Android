package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class SuggestedUsersActivity extends SuggestedUsersBaseActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) {
            final SuggestedUsersCategoriesFragment suggestedUsersCategoriesFragment = new SuggestedUsersCategoriesFragment();
            suggestedUsersCategoriesFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(getContentHolderViewId(), suggestedUsersCategoriesFragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            EventBus.SCREEN_ENTERED.publish(Screen.ONBOARDING_MAIN.get());
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
