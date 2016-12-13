package com.soundcloud.android.utils;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.ArrayRes;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class BugReporter {

    private final ApplicationProperties applicationProperties;
    private final DeviceHelper deviceHelper;
    private final Resources resources;

    private static final String EMAIL_MESSAGE_FORMAT_RFC822 = "message/rfc822";
    private static final String LOGCAT_FILE_NAME = "logcat.txt";

    @Inject
    public BugReporter(ApplicationProperties applicationProperties,
                       DeviceHelper deviceHelper,
                       Resources resources) {
        this.applicationProperties = applicationProperties;
        this.deviceHelper = deviceHelper;
        this.resources = resources;
    }

    public void showGeneralFeedbackDialog(final Context context) {
        showFeedbackDialog(context, R.array.feedback_general);
    }

    public void showSignInFeedbackDialog(final Context context) {
        showFeedbackDialog(context, R.array.feedback_sign_in);
    }

    private void showFeedbackDialog(final Context context, @ArrayRes int options) {
        final String[] feedbackOptions = resources.getStringArray(options);
        new AlertDialog.Builder(context).setTitle(R.string.select_feedback_category)
                                        .setItems(feedbackOptions, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                final String feedbackOption = feedbackOptions[which];
                                                final String subject = resources.getString(R.string.feedback_email_subject,
                                                                                           feedbackOption);
                                                final String actionChooser = resources.getString(R.string.feedback_action_chooser);
                                                final String feedbackEmail = feedbackOption.equals(resources.getString(R.string.feedback_playback_issue)) ?
                                                                             applicationProperties.getPlaybackFeedbackEmail() :
                                                                             applicationProperties.getFeedbackEmail();

                                                sendLogs(context,
                                                         feedbackEmail,
                                                         subject,
                                                         deviceHelper.getUserAgent(),
                                                         actionChooser);
                                            }
                                        }).show();
    }

    private void sendLogs(Context context, String toEmail, String subject, String body, String chooserText) {
        File outputFile = IOUtils.getExternalStorageDir(context, LOGCAT_FILE_NAME);

        if (outputFile == null) return;

        outputFile.delete();

        try {
            String writeLogcatToFileCommand = String.format("logcat -v time -df %s", outputFile.getAbsolutePath());
            Runtime.getRuntime().exec(writeLogcatToFileCommand);

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(EMAIL_MESSAGE_FORMAT_RFC822);
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{toEmail});
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, body);
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
            context.startActivity(Intent.createChooser(i, chooserText));

        } catch (IOException e) {
            ErrorUtils.handleSilentException(e);
            AndroidUtils.showToast(context, R.string.feedback_unable_to_get_logs);
        }
    }
}