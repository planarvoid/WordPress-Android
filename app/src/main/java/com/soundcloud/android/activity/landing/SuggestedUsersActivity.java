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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_suggested_users));
        setContentView(R.layout.suggested_users_onboard);


        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.list_holder, new SuggestedUsersCategoriesFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done){
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
        Bundle args = new Bundle();
        args.putParcelable(SuggestedUsersCategoryFragment.KEY_CATEGORY, category);

        SuggestedUsersCategoryFragment fragment = new SuggestedUsersCategoryFragment();
        fragment.setArguments(args);
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                .beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left,
                R.anim.slide_in_from_left, R.anim.slide_out_to_right);
        fragmentTransaction
                .replace(R.id.list_holder, fragment)
                .addToBackStack("category")
                .commit();
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.who_to_follow;
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_suggested_users;
    }
}
