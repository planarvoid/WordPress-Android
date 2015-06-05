package com.soundcloud.android.utils;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class BugReporter {

    private final ApplicationProperties applicationProperties;
    private final DeviceHelper deviceHelper;
    private final Resources resources;

    private static final String EMAIL_MESSAGE_FORMAT_RFC822 = "message/rfc822";

    @Inject
    public BugReporter(ApplicationProperties applicationProperties,
                       DeviceHelper deviceHelper,
                       Resources resources) {
        this.applicationProperties = applicationProperties;
        this.deviceHelper = deviceHelper;
        this.resources = resources;
    }

    public void showFeedbackDialog(final FragmentActivity activity) {
        final String[] feedbackOptions = resources.getStringArray(R.array.feedback_options);
        new AlertDialog.Builder(activity).setTitle(R.string.select_feedback_category)
                .setItems(feedbackOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String feedbackOption = feedbackOptions[which];
                        final String subject = resources.getString(R.string.feedback_email_subject, feedbackOption);
                        final String actionChooser = resources.getString(R.string.feedback_action_chooser);
                        final String feedbackEmail = feedbackOption.equals(resources.getString(R.string.feedback_playback_issue)) ?
                                applicationProperties.getPlaybackFeedbackEmail() : applicationProperties.getFeedbackEmail() ;

                        sendLogs(activity, feedbackEmail, subject, deviceHelper.getUserAgent(), actionChooser);
                    }
                }).show();
    }

    private void sendLogs(Context context, String toEmail, String subject, String body, String chooserText) {
        // save logcat in file
        File outputFile = new File(Environment.getExternalStorageDirectory(), "logcat.txt");
        try {
            Runtime.getRuntime().exec("logcat -v time -df " + outputFile.getAbsolutePath());

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(EMAIL_MESSAGE_FORMAT_RFC822);
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{toEmail});
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT   , body);
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
            context.startActivity(Intent.createChooser(i, chooserText));

        } catch (IOException e) {
            ErrorUtils.handleSilentException(e);
            AndroidUtils.showToast(context, R.string.feedback_unable_to_get_logs);
        }
    }

}
