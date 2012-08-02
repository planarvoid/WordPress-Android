package com.soundcloud.android.activity.preference;

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
    private static final String androidns="http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mSplashText, mValueText;
    private Context mContext;

    private String mDialogMessage, mSuffix;
    private int mDefault, mMaxValue, mValue = 0;

    public SeekBarPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mContext       = context;
        mDialogMessage = attributeSet.getAttributeValue(androidns, "dialogMessage");
        mSuffix        = attributeSet.getAttributeValue(androidns, "text");
        mDefault       = attributeSet.getAttributeIntValue(androidns, "defaultValue", 50);
        mMaxValue      = attributeSet.getAttributeIntValue(androidns, "max", 100);
    }

    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        mSplashText = new TextView(mContext);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(32);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, params);

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMaxValue);
        mSeekBar.setProgress(mValue);

        return layout;
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        else
            mValue = (Integer)defaultValue;
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
    {
        String t = String.valueOf(value);
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));

        if (shouldPersist())
            persistInt(value);

        callChangeListener(value);
    }
    public void onStartTrackingTouch(SeekBar seek) {}
    public void onStopTrackingTouch(SeekBar seek) {}

    public void setMax(int max) { mMaxValue = max; }
    public int getMax() { return mMaxValue; }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }

    public int getProgress() { return mValue; }
}
