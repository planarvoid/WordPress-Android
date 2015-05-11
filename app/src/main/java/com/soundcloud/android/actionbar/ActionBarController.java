package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import javax.inject.Inject;

public class ActionBarController extends DefaultLightCycleActivity<ActionBarActivity> {
    protected EventBus eventBus;
    private final ApplicationProperties applicationProperties;
    private final DeviceHelper deviceHelper;

    @Inject
    protected ActionBarController(EventBus eventBus, ApplicationProperties applicationProperties,
                                  DeviceHelper deviceHelper) {
        this.eventBus = eventBus;
        this.applicationProperties = applicationProperties;
        this.deviceHelper = deviceHelper;
    }

    @Override
    public boolean onOptionsItemSelected(final ActionBarActivity activity, MenuItem item) {
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
            case R.id.action_feedback:
                showFeedbackDialog(activity);
                return true;
            default:
                return false;
        }
    }

    private void showFeedbackDialog(final ActionBarActivity activity) {
        final String[] feedbackOptions = activity.getResources().getStringArray(R.array.feedback_options);
        new AlertDialog.Builder(activity).setTitle(R.string.select_feedback_category)
                .setItems(feedbackOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String feedbackOption = feedbackOptions[which];
                        final String subject = activity.getString(R.string.feedback_email_subject, feedbackOption);
                        final String actionChooser = activity.getString(R.string.feedback_action_chooser);
                        final String feedbackEmail = feedbackOption.equals(activity.getString(R.string.feedback_playback_issue)) ?
                                applicationProperties.getPlaybackFeedbackEmail() : applicationProperties.getFeedbackEmail() ;

                        DebugUtils.sendLogs(activity, feedbackEmail, subject, deviceHelper.getUserAgent(), actionChooser);
                    }
                }).show();
    }

    private void startActivity(FragmentActivity activity, Class target) {
        activity.startActivity(new Intent(activity, target));
    }
}
