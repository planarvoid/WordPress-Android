package com.soundcloud.android.onboarding;

import com.soundcloud.android.api.HttpProperties;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import java.util.Locale;

class SpamDialogOnClickListener implements DialogInterface.OnClickListener {
    private static final String SIGNUP_WITH_CAPTCHA_URI =
            "https://soundcloud.com/connect?c=true&highlight=signup&client_id=%s&redirect_uri=soundcloud://auth&response_type=code&scope=non-expiring";
    private final HttpProperties httpProperties = new HttpProperties();
    private final Context context;

    SpamDialogOnClickListener(Context context) {
        this.context = context;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onCaptchaRequested();
                dialog.dismiss();
                break;
            default:
                dialog.dismiss();
        }
    }

    private void onCaptchaRequested() {
        String uriString = String.format(Locale.US, SIGNUP_WITH_CAPTCHA_URI, httpProperties.getClientId());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        context.startActivity(intent);
    }
}
