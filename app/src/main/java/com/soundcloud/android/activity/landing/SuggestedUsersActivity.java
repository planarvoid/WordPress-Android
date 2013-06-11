package com.soundcloud.android.activity.landing;

import com.actionbarsherlock.view.MenuItem;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.SuggestedUsersCategoriesFragment;
import com.soundcloud.android.fragment.SuggestedUsersCategoryFragment;
import com.soundcloud.android.fragment.listeners.SuggestedUsersFragmentListener;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.SyncStateManager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

public class SuggestedUsersActivity extends ScActivity implements ScLandingPage, SuggestedUsersFragmentListener {

    private SuggestedUsersCategoryFragment mCategoryFragment;
    private boolean mDualScreen;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_suggested_users));
        setContentView(R.layout.suggested_users_activity);

        mDualScreen = getResources().getBoolean(R.bool.has_two_panels);
        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.categories_fragment_holder, new SuggestedUsersCategoriesFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done) {
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
    public void onCategorySelected(Category category) {
        if (mDualScreen) {
            Bundle args = new Bundle();
            args.putParcelable(Category.EXTRA, category);

            final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            SuggestedUsersCategoryFragment fragment = new SuggestedUsersCategoryFragment();
            fragment.setArguments(args);
            fragmentTransaction.replace(R.id.users_fragment_holder, fragment);
            fragmentTransaction.commit();

        } else {
            final Intent intent = new Intent(this, SuggestedUsersCategoryActivity.class);
            intent.putExtra(Category.EXTRA, category);
            startActivity(intent);
        }
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
