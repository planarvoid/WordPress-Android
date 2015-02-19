package com.soundcloud.android.profile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import com.soundcloud.android.R;

public class MonthPickerDialogFragment extends DialogFragment {

    public static final String MONTH_BUNDLE_KEY = "MONTH_KEY";

    public static DialogFragment build(int monthOfYear) {
        DialogFragment fragment = new MonthPickerDialogFragment();
        Bundle fragmentArguments = new Bundle();
        fragmentArguments.putInt(MONTH_BUNDLE_KEY, monthOfYear);
        fragment.setArguments(fragmentArguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_month)
                .setSingleChoiceItems(R.array.select_month_options, currentMonthIndex(), new OnMonthSelected())
                .create();
    }

    private int currentMonthIndex() {
        return getArguments().getInt(MONTH_BUNDLE_KEY, 0) - 1;
    }

    private Callback getCallback() {
        return ((CallbackProvider) getActivity()).getMonthPickerCallback();
    }

    private class OnMonthSelected implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            String[] months = getActivity().getResources().getStringArray(R.array.select_month_options);
            getCallback().onMonthSelected(months[i], i + 1);
            dialogInterface.dismiss();
        }
    }

    public static interface Callback {
        void onMonthSelected(String monthName, int monthOfYear);
    }

    public static interface CallbackProvider {
        Callback getMonthPickerCallback();
    }
}
