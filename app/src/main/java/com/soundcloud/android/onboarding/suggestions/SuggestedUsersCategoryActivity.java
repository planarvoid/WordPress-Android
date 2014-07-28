package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class SuggestedUsersCategoryActivity extends ScActivity {

    private Category category;
    private SuggestedUsersCategoryFragment categoryFragment;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.container_layout);
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
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            if (category.isFacebookCategory()) {
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.ONBOARDING_FACEBOOK.get());
            } else {
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.ONBOARDING_GENRE.get());
            }
        }
    }

    protected ActionBarController createActionBarController() {
        return new ActionBarController(this, eventBus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (category.isFollowed(new FollowingOperations().getFollowedUserIds())){
            menu.findItem(R.id.menu_select_all).setVisible(false);
        } else {
            menu.findItem(R.id.menu_deselect_all).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final long itemId = item.getItemId();
        if (itemId == R.id.menu_select_all || itemId == R.id.menu_deselect_all) {
            categoryFragment.toggleFollowings(itemId == R.id.menu_select_all);
            supportInvalidateOptionsMenu();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.suggested_users_category;
    }
}
