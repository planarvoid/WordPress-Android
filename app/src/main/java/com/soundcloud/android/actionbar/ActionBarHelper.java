package com.soundcloud.android.actionbar;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.inject.Inject;

public class ActionBarHelper extends DefaultActivityLightCycle<AppCompatActivity> {

    private final CastConnectionHelper castConnectionHelper;
    private final EventBus eventBus;
    private final ApplicationProperties applicationProperties;
    private final BugReporter bugReporter;
    private final Navigator navigator;
    private final DeviceHelper deviceHelper;

    @Inject
    public ActionBarHelper(CastConnectionHelper castConnectionHelper,
                           EventBus eventBus,
                           ApplicationProperties applicationProperties,
                           BugReporter bugReporter,
                           Navigator navigator,
                           DeviceHelper deviceHelper) {
        this.castConnectionHelper = castConnectionHelper;
        this.eventBus = eventBus;
        this.applicationProperties = applicationProperties;
        this.bugReporter = bugReporter;
        this.navigator = navigator;
        this.deviceHelper = deviceHelper;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.main, menu);

        if (menu.findItem(R.id.media_route_menu_item) != null) {
            castConnectionHelper.addMediaRouterButton(menu, R.id.media_route_menu_item);
        }

        final MenuItem feedbackItem = menu.findItem(R.id.action_feedback);
        if (feedbackItem != null) {
            feedbackItem.setVisible(applicationProperties.shouldAllowFeedback());
        }

        final MenuItem recordItem = menu.findItem(R.id.action_record);
        if (recordItem != null) {
            recordItem.setVisible(deviceHelper.hasMicrophone());
        }
    }

    @Override
    public boolean onOptionsItemSelected(final AppCompatActivity activity, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                navigator.openDiscovery(activity);
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromSearchAction());
                return true;
            case R.id.action_settings:
                navigator.openSettings(activity);
                return true;
            case R.id.action_record:
                navigator.openRecord(activity);
                return true;
            case R.id.action_activity:
                navigator.openActivities(activity);
                return true;
            case R.id.action_feedback:
                bugReporter.showGeneralFeedbackDialog(activity);
                return true;
            default:
                return false;
        }
    }

}
