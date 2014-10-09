package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.search.SearchActivity;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

public class ActionBarController {
    @NotNull
    protected ActionBarOwner owner;
    @NotNull
    protected Activity activity;
    @NotNull
    protected EventBus eventBus;

    public interface ActionBarOwner {
        @NotNull
        ActionBarActivity getActivity();

        int getMenuResourceId();

        void restoreActionBar();
    }

    protected ActionBarController(@NotNull ActionBarOwner owner, @NotNull EventBus eventBus) {
        this.owner = owner;
        this.activity = owner.getActivity();
        this.eventBus = eventBus;
    }

    public void onDestroy() {
        /** nop, used by {@link SearchActionBarController#onDestroy()} ()} **/
    }

    /**
     * This must be passed through by the activity in order to configure based on search state
     */
    public void onCreateOptionsMenu(Menu menu) {
        owner.restoreActionBar();
        final int menuResourceId = owner.getMenuResourceId();
        if (menuResourceId > 0) {
            owner.getActivity().getMenuInflater().inflate(menuResourceId, menu);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(SearchActivity.class);
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromSearchAction());
                return true;
            case R.id.action_settings:
                startActivity(SettingsActivity.class);
                return true;
            case R.id.action_record:
                startActivity(RecordActivity.class);
                return true;
            case R.id.action_who_to_follow:
                startActivity(WhoToFollowActivity.class);
                return true;
            case R.id.action_activity:
                startActivity(ActivitiesActivity.class);
                return true;
            default:
                return false;
        }
    }

    public boolean isVisible() {
        return owner.getActivity().getSupportActionBar().isShowing();
    }

    public void setVisible(boolean isVisible) {
        ActionBar actionBar = owner.getActivity().getSupportActionBar();
        if (isVisible) {
            actionBar.show();
        } else {
            actionBar.hide();
        }
    }

    private void startActivity(Class target) {
        owner.getActivity().startActivity(new Intent(owner.getActivity(), target));
    }

    public static class Factory {

        private final EventBus eventBus;

        @Inject
        public Factory(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        public ActionBarController create(ActionBarOwner owner) {
            return new ActionBarController(owner, eventBus);
        }
    }
}
