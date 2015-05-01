package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public class GenderPickerDialogFragment extends DialogFragment {

    public static final String GENDER_BUNDLE_KEY = "GENDER_KEY";

    static DialogFragment build(GenderOption startingOption) {
        DialogFragment fragment = new GenderPickerDialogFragment();
        Bundle fragmentArguments = new Bundle();
        fragmentArguments.putSerializable(GENDER_BUNDLE_KEY, startingOption);
        fragment.setArguments(fragmentArguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.onboarding_indicate_gender)
                .setSingleChoiceItems(genderOptions(), currentGenderIndex(), new OnGenderSelected())
                .create();
    }

    private String[] genderOptions() {
        GenderOption[] values = GenderOption.values();
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            options[i] = getString(values[i].getResId());
        }
        return options;
    }

    private int currentGenderIndex() {
        GenderOption currentGenderOption = (GenderOption) getArguments().getSerializable(GENDER_BUNDLE_KEY);
        return (currentGenderOption != null) ? currentGenderOption.ordinal() : -1;
    }

    private Callback getCallback() {
        return ((CallbackProvider) getActivity()).getGenderPickerCallback();
    }

    private class OnGenderSelected implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            getCallback().onGenderSelected(GenderOption.values()[i]);
            dialogInterface.dismiss();
        }
    }

    public static interface Callback {
        void onGenderSelected(GenderOption gender);
    }

    public static interface CallbackProvider {
        Callback getGenderPickerCallback();
    }
}
