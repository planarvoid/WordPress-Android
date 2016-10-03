package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;

public class ConversionRestrictionsDialog extends AppCompatDialogFragment {

    private static final String PROMO_DURATION = "promo_duration";
    private static final String PROMO_PRICE = "promo_price";
    private static final String PROMO_REGULAR_PRICE = "promo_regular_price";

    private static final String TRIAL_DAYS = "trial_days";

    public static ConversionRestrictionsDialog createForNoTrial() {
        return new ConversionRestrictionsDialog();
    }

    public static ConversionRestrictionsDialog createForTrial(int trialDays) {
        final ConversionRestrictionsDialog dialog = new ConversionRestrictionsDialog();
        Bundle args = new Bundle();
        args.putInt(TRIAL_DAYS, trialDays);
        dialog.setArguments(args);
        return dialog;
    }

    public static ConversionRestrictionsDialog createForPromo(String duration, String promoPrice, String regularPrice) {
        final ConversionRestrictionsDialog dialog = new ConversionRestrictionsDialog();
        Bundle args = new Bundle();
        args.putString(PROMO_DURATION, duration);
        args.putString(PROMO_PRICE, promoPrice);
        args.putString(PROMO_REGULAR_PRICE, regularPrice);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.conversion_restrictions_dialog_title)
                .setMessage(formatBody())
                .get();
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private String formatBody() {
        if (getArguments() == null) {
            return getString(R.string.conversion_restrictions_message_no_trial);
        } else if (getArguments().containsKey(PROMO_DURATION)) {
            String duration = getArguments().getString(PROMO_DURATION);
            return getString(R.string.conversion_restrictions_message_promo, duration, getArguments().getString(PROMO_PRICE), getArguments().getString(PROMO_REGULAR_PRICE));
        } else if (getArguments().containsKey(TRIAL_DAYS)) {
            int trialDays = getArguments().getInt(TRIAL_DAYS);
            return getString(R.string.conversion_restrictions_message_trial, trialDays, trialDays, trialDays);
        } else {
            return getString(R.string.conversion_restrictions_message_no_trial);
        }
    }

}
