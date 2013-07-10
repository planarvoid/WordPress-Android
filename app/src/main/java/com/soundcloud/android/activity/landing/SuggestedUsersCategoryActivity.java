package com.soundcloud.android.activity.landing;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.SuggestedUsersCategoryFragment;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.operations.following.FollowingOperations;

import android.os.Bundle;

public class SuggestedUsersCategoryActivity extends ScActivity {

    private Category mCategory;
    private SuggestedUsersCategoryFragment mCategoryFragment;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.frame_layout_holder);
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
    protected int getSelectedMenuId() {
        return 0;
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
            invalidateOptionsMenu();
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
