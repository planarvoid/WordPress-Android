package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.utils.ScTextUtils.prettyPrintJson;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.navigation.IntentFactory;
import com.soundcloud.java.strings.Strings;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.widget.TextView;

public class DevEventLoggerMonitorDetailsDialog extends AppCompatDialogFragment {
    private static final String KEY_TRACKING_RECORD_DATA = "KEY_TRACKING_RECORD_DATA";
    private static final String CLIPBOARD_LABEL = "event_data";
    private static final int INDENT_SPACES = 2;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.body) TextView body;

    private Unbinder unbinder;

    public static DevEventLoggerMonitorDetailsDialog create(TrackingRecord trackingRecord) {
        final Bundle args = new Bundle();
        final DevEventLoggerMonitorDetailsDialog dialog = new DevEventLoggerMonitorDetailsDialog();
        args.putString(KEY_TRACKING_RECORD_DATA, trackingRecord.getData());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = View.inflate(getActivity(), R.layout.dialog_dev_event_logger_monitor_details, null);
        final String prettyData = prettyPrintJson(getData(), INDENT_SPACES);
        unbinder = ButterKnife.bind(this, view);
        title.setText(getString(R.string.event_logger_monitor_details_dialog_title));
        body.setText(prettyData);
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.copy, (dialog, which) -> copyDataToClipboard(prettyData))
                .setNegativeButton(R.string.share, (dialog, which) -> startActivity(IntentFactory.createTextShareIntentChooser(getActivity(), prettyData)))
                .create();
    }

    private String getData() {
        final Bundle args = getArguments();
        return args != null ? args.getString(KEY_TRACKING_RECORD_DATA, Strings.EMPTY) : Strings.EMPTY;
    }

    private void copyDataToClipboard(String prettyData) {
        final ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(CLIPBOARD_LABEL, prettyData);
        clipboardManager.setPrimaryClip(clip);
    }

    @Override
    public void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }
}
