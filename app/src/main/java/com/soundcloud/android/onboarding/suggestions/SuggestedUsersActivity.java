package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.android.main.ScActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

public class SuggestedUsersActivity extends ScActivity {
    @Inject @LightCycle ActionBarController actionBarController;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getMenuInflater().inflate(R.menu.suggested_users, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.ONBOARDING_MAIN));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO : move to action bar controller ?
        if (item.getItemId() == R.id.finish) {
            startActivity(new Intent(this, SuggestedUsersSyncActivity.class));
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.onboardingComplete());
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
