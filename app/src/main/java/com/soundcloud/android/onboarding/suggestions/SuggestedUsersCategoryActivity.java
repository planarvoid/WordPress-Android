package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

public class SuggestedUsersCategoryActivity extends ScActivity {

    private Category category;
    private SuggestedUsersCategoryFragment categoryFragment;

    @Inject @LightCycle ActionBarController actionBarController;
    @Inject FollowingOperations followingOperations;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!getIntent().hasExtra(Category.EXTRA)) {
            finish();
        } else {
            category = getIntent().getParcelableExtra(Category.EXTRA);
            if (state == null) {
                categoryFragment = new SuggestedUsersCategoryFragment();
                categoryFragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.container, categoryFragment)
                        .commit();
            } else {
                categoryFragment = (SuggestedUsersCategoryFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.suggested_users_category, menu);
        if (category.isFollowed(followingOperations.getFollowedUserIds())) {
            menu.findItem(R.id.menu_select_all).setVisible(false);
        } else {
            menu.findItem(R.id.menu_deselect_all).setVisible(false);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            if (category.isFacebookCategory()) {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.ONBOARDING_FACEBOOK));
            } else {
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.ONBOARDING_GENRE));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO : move this to action bar controller ?
        final long itemId = item.getItemId();
        if (itemId == R.id.menu_select_all || itemId == R.id.menu_deselect_all) {
            categoryFragment.toggleFollowings(itemId == R.id.menu_select_all);
            supportInvalidateOptionsMenu();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
