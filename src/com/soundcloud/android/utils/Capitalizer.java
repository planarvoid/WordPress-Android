package com.soundcloud.android.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

public class Capitalizer implements TextWatcher {
    private TextView text;
    public Capitalizer(TextView text) {
        this.text = text;
    }

    public void afterTextChanged(Editable s) {
        if (s.length() == 1
        && !s.toString().toUpperCase().contentEquals(s.toString())) {
            text.setTextKeepState(s.toString().toUpperCase());
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
