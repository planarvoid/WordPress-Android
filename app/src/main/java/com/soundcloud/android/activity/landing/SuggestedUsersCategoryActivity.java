package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.SuggestedUsersCategoryFragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class SuggestedUsersCategoryActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (getResources().getBoolean(R.bool.has_two_panels)) {
            finish();
        } else {
            setTitle(getString(R.string.side_menu_suggested_users));
            setContentView(R.layout.suggested_users_category_activity);

            if (state == null) {
                final SuggestedUsersCategoryFragment fragment = new SuggestedUsersCategoryFragment();
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.users_fragment_holder, fragment)
                        .commit();
            }
        }
    }

    @Override
    public void setContentView(View layout) {
        super.setContentView(layout);
        layout.setBackgroundColor(Color.WHITE);
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
