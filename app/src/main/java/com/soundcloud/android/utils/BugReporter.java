package com.soundcloud.android.utils;

import static com.soundcloud.android.ApplicationModule.BUG_REPORTER;
import static java.lang.String.format;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.java.strings.Charsets;
import okio.Okio;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.ArrayRes;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Locale;

public class BugReporter {

    private final ApplicationProperties applicationProperties;
    private final DeviceHelper deviceHelper;
    private final Resources resources;
    private final Scheduler scheduler;

    private static final String EMAIL_MESSAGE_FORMAT_RFC822 = "message/rfc822";
    private static final String LOGCAT_FILE_NAME = "logcat.txt";

    @Inject
    BugReporter(ApplicationProperties applicationProperties,
                DeviceHelper deviceHelper,
                Resources resources,
                @Named(BUG_REPORTER) Scheduler scheduler) {
        this.applicationProperties = applicationProperties;
        this.deviceHelper = deviceHelper;
        this.resources = resources;
        this.scheduler = scheduler;
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
                                        .setItems(feedbackOptions, (dialog, which) -> {
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
                                        }).show();
    }

    private void sendLogs(Context context, String toEmail, String subject, String body, String chooserText) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(EMAIL_MESSAGE_FORMAT_RFC822);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{toEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        File outputFile = IOUtils.getExternalStorageDir(context, LOGCAT_FILE_NAME);
        if (outputFile == null) {
            Log.e("Failed to get external storage directory for logcat file. Sending bug report without logs.");
            context.startActivity(Intent.createChooser(intent, chooserText));
            return;
        }

        Observable.fromCallable(() -> {
            if (outputFile.exists() && !outputFile.delete()) {
                throw new RuntimeException("Failed to delete file: " + outputFile.getAbsolutePath());
            }
            Process logcatProcess = new ProcessBuilder()
                    .redirectErrorStream(true)
                    .command("logcat", "-v", "time", "-df", outputFile.getAbsolutePath())
                    .start();
            int exitCode = logcatProcess.waitFor();
            if (exitCode != 0) {
                String output = Okio.buffer(Okio.source(logcatProcess.getInputStream())).readString(Charsets.UTF_8);
                throw new RuntimeException(format(Locale.US, "logcat failed with exit code %d. Output: %s", exitCode, output));
            }
            return null;
        }).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(LambdaSubscriber.onNext(t -> {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
            context.startActivity(Intent.createChooser(intent, chooserText));
        }));
    }
}
