package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.search.CombinedSearchActivity;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

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
    public ActionBarController(@NotNull ActionBarOwner owner, @NotNull EventBus eventBus) {
        this.owner = owner;
        this.activity = owner.getActivity();
        this.eventBus = eventBus;
    }

    public void onResume() {
        /** nop, used by {@link NowPlayingActionBarController#onResume()} ()} **/
    }

    public void onPause() {
        /** nop, used by {@link NowPlayingActionBarController#onPause()} ()} **/
    }

    public void onDestroy() {
        /** nop, used by {@link SearchActionBarController#onDestroy()} ()} **/
    }

    /**
     * This must be passed through by the activity in order to configure based on search state
     */
    public void onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = owner.getActivity().getSupportActionBar();
        setActionBarDefaultOptions(actionBar);
        final int menuResourceId = owner.getMenuResourceId();
        if (menuResourceId > 0) owner.getActivity().getMenuInflater().inflate(menuResourceId, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(CombinedSearchActivity.class);
                eventBus.publish(EventQueue.UI, UIEvent.fromSearchAction());
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

    protected void setActionBarDefaultOptions(ActionBar actionBar) {
        owner.restoreActionBar();
    }
}
