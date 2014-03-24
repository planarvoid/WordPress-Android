package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Category;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class SuggestedUsersCategoryActivity extends ScActivity {

    private Category mCategory;
    private SuggestedUsersCategoryFragment mCategoryFragment;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.container_layout);
        if (!getIntent().hasExtra(Category.EXTRA)) {
            finish();
        } else {
            mCategory = getIntent().getParcelableExtra(Category.EXTRA);
            if (state == null) {
                mCategoryFragment = new SuggestedUsersCategoryFragment();
                mCategoryFragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.holder, mCategoryFragment)
                        .commit();
            } else {
                mCategoryFragment = (SuggestedUsersCategoryFragment) getSupportFragmentManager().findFragmentById(R.id.holder);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            if (mCategory.isFacebookCategory()) {
                mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.ONBOARDING_FACEBOOK.get());
            } else {
                mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.ONBOARDING_GENRE.get());
            }
        }
    }

    protected ActionBarController createActionBarController() {
        return new ActionBarController(this, mEventBus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mCategory.isFollowed(new FollowingOperations().getFollowedUserIds())){
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
            mCategoryFragment.toggleFollowings(itemId == R.id.menu_select_all);
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
