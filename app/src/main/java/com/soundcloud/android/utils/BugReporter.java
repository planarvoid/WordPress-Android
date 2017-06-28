package com.soundcloud.android.utils;

import static com.soundcloud.android.ApplicationModule.BUG_REPORTER;
import static java.lang.String.format;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.java.strings.Charsets;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import okio.Okio;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.ArrayRes;
import android.support.v4.content.FileProvider;

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
        getFeedbackDialog(context, R.array.feedback_general).show();
    }

    public void showSignInFeedbackDialog(final Context context) {
        getFeedbackDialog(context, R.array.feedback_sign_in).show();
    }

    public AlertDialog getFeedbackDialog(final Context context, @ArrayRes int options) {
        final String[] feedbackOptions = resources.getStringArray(options);
        return new AlertDialog.Builder(context).setTitle(R.string.select_feedback_category)
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
                                               }).create();
    }

    private void sendLogs(Context context, String toEmail, String subject, String body, String chooserText) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(EMAIL_MESSAGE_FORMAT_RFC822);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{toEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        File outputFile = IOUtils.createExternalStorageDir(context, LOGCAT_FILE_NAME);
        if (outputFile == null) {
            Log.e("Failed to get external storage directory for logcat file. Sending bug report without logs.");
            context.startActivity(Intent.createChooser(intent, chooserText));
            return;
        }

        collectLogCat(context, outputFile).subscribeOn(scheduler)
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribeWith(LambdaSingleObserver.onNext((uri) -> {
                                              if (!Uri.EMPTY.equals(uri)) {
                                                  intent.putExtra(Intent.EXTRA_STREAM, uri);
                                              }
                                              context.startActivity(Intent.createChooser(intent, chooserText));
                                          }));
    }

    private Single<Uri> collectLogCat(Context context, File outputFile) {
        return Single.fromCallable(() -> {
            if (outputFile.exists() && !outputFile.delete()) {
                Log.e("Failed to delete file: " + outputFile.getAbsolutePath());
                return Uri.EMPTY;
            }
            Process logcatProcess = new ProcessBuilder()
                    .redirectErrorStream(true)
                    .command("logcat", "-v", "time", "-df", outputFile.getAbsolutePath())
                    .start();
            int exitCode = logcatProcess.waitFor();
            if (exitCode != 0) {
                String output = Okio.buffer(Okio.source(logcatProcess.getInputStream())).readString(Charsets.UTF_8);
                Log.e(format(Locale.US, "logcat failed with exit code %d. Output: %s", exitCode, output));
                return Uri.EMPTY;
            }
            return FileProvider.getUriForFile(context, BuildConfig.FILE_PROVIDER_AUTHORITY, outputFile);
        }).onErrorReturnItem(Uri.EMPTY);
    }
}
