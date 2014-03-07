package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
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
    protected ActionBarOwner mOwner;
    @NotNull
    protected Activity mActivity;

    public interface ActionBarOwner {
        @NotNull
        ActionBarActivity getActivity();
        int getMenuResourceId();
        void restoreActionBar();
    }

    public ActionBarController(@NotNull ActionBarOwner owner) {
        mOwner = owner;
        mActivity = owner.getActivity();
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
        ActionBar actionBar = mOwner.getActivity().getSupportActionBar();
        setActionBarDefaultOptions(actionBar);
        final int menuResourceId = mOwner.getMenuResourceId();
        if (menuResourceId > 0) mOwner.getActivity().getMenuInflater().inflate(menuResourceId, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(CombinedSearchActivity.class);
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

    private void startActivity(Class target) {
        mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), target));
    }

    protected void setActionBarDefaultOptions(ActionBar actionBar) {
        mOwner.restoreActionBar();
    }
}
