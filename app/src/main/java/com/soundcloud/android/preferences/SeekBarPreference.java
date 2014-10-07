package com.soundcloud.android.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/* Based on the SeekBarPreference by Matthew Wiggins
 * Released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private SeekBar seekBar;
    private TextView valueText;
    private final Context context;

    private final String dialogMessage, suffix;
    private final int defaultValue;
    private int maxValue, value = 0;

    public SeekBarPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        this.context = context;
        dialogMessage = attributeSet.getAttributeValue(androidns, "dialogMessage");
        suffix = attributeSet.getAttributeValue(androidns, "text");
        defaultValue = attributeSet.getAttributeIntValue(androidns, "defaultValue", 50);
        maxValue = attributeSet.getAttributeIntValue(androidns, "max", 100);
    }

    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        TextView splashText = new TextView(context);
        if (dialogMessage != null) {
            splashText.setText(dialogMessage);
        }
        layout.addView(splashText);

        valueText = new TextView(context);
        valueText.setGravity(Gravity.CENTER_HORIZONTAL);
        valueText.setTextSize(32);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(valueText, params);

        seekBar = new SeekBar(context);
        seekBar.setOnSeekBarChangeListener(this);
        layout.addView(seekBar, params);

        if (shouldPersist()) {
            value = getPersistedInt(defaultValue);
        }

        seekBar.setMax(maxValue);
        seekBar.setProgress(value);

        return layout;
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            value = shouldPersist() ? getPersistedInt(this.defaultValue) : 0;
        } else {
            value = (Integer) defaultValue;
        }
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        String t = String.valueOf(value);
        valueText.setText(suffix == null ? t : t.concat(suffix));

        if (shouldPersist()) {
            persistInt(value);
        }

        callChangeListener(value);
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }

    public void setMax(int max) {
        maxValue = max;
    }

    public int getMax() {
        return maxValue;
    }

    public void setProgress(int progress) {
        value = progress;
        if (seekBar != null) {
            seekBar.setProgress(progress);
        }
    }

    public int getProgress() {
        return value;
    }
}
