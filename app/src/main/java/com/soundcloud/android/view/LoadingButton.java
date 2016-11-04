package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LoadingButton extends RelativeLayout {

    private String loadingText;
    private String actionText;
    private String retryActionTextAttr;

    private TextView label;
    private ProgressBar progress;

    public LoadingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public LoadingButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public void setLoading(boolean loading) {
        String loadingLabel = loadingText == null ? actionText : loadingText;
        label.setText(loading ? loadingLabel : actionText);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    public void setRetry() {
        label.setText(retryActionTextAttr);
        progress.setVisibility(View.GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        label.setAlpha(enabled ? 1 : 0.5f);
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
        label.setText(actionText);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        LayoutInflater.from(context).inflate(R.layout.loading_button, this);
        label = (TextView) findViewById(R.id.label);
        progress = (ProgressBar) findViewById(R.id.progress);
        setupAttributes(context, attrs, defStyle);
        setClickable(true);
    }

    private void setupAttributes(Context context, AttributeSet attrs, int defStyle) {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingButton, defStyle, 0);
        loadingText = getLoadingTextAttr(typedArray);
        actionText = getMainActionTextAttr(typedArray);
        retryActionTextAttr = getRetryActionTextAttr(typedArray);
        label.setText(actionText);
        label.setTextColor(getTextColor(typedArray));
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize(typedArray));
        progress.getIndeterminateDrawable().setColorFilter(getLoadingColor(typedArray), PorterDuff.Mode.SRC_IN);
        typedArray.recycle();
    }

    private int getLoadingColor(TypedArray typedArray) {
        return typedArray.getColor(R.styleable.LoadingButton_loadingColor, 0);
    }

    private int getTextColor(TypedArray typedArray) {
        return typedArray.getColor(R.styleable.LoadingButton_textColor, 0);
    }

    private float getTextSize(TypedArray typedArray) {
        return typedArray.getDimensionPixelSize(R.styleable.LoadingButton_textSize, 0);
    }

    private String getLoadingTextAttr(TypedArray typedArray) {
        return typedArray.getString(R.styleable.LoadingButton_loadingText);
    }

    private String getMainActionTextAttr(TypedArray typedArray) {
        return typedArray.getString(R.styleable.LoadingButton_actionText);
    }

    private String getRetryActionTextAttr(TypedArray typedArray) {
        return typedArray.getString(R.styleable.LoadingButton_retryText);
    }

}
