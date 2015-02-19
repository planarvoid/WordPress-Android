package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.settings.SettingsActivity;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import javax.inject.Inject;

public class ActionBarController extends DefaultLightCycleActivity<ActionBarActivity> {
    protected EventBus eventBus;

    @Inject
    protected ActionBarController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public boolean onOptionsItemSelected(ActionBarActivity activity, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(activity, SearchActivity.class);
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromSearchAction());
                return true;
            case R.id.action_settings:
                startActivity(activity, SettingsActivity.class);
                return true;
            case R.id.action_record:
                startActivity(activity, RecordActivity.class);
                return true;
            case R.id.action_who_to_follow:
                startActivity(activity, WhoToFollowActivity.class);
                return true;
            case R.id.action_activity:
                startActivity(activity, ActivitiesActivity.class);
                return true;
            default:
                return false;
        }
    }

    private void startActivity(FragmentActivity activity, Class target) {
        activity.startActivity(new Intent(activity, target));
    }
}
