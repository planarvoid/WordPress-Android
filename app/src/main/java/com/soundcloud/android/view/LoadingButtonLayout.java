package com.soundcloud.android.view;


import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingButtonLayout extends FrameLayout {

    private String mainActionTextAttr;
    private String retryActionTextAttr;

    public LoadingButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public LoadingButtonLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public void setWaiting() {
        findLabel().setText(mainActionTextAttr);
        findProgressBar().setVisibility(View.VISIBLE);
    }

    public void setRetry() {
        findLabel().setText(retryActionTextAttr);
        findProgressBar().setVisibility(View.GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        findLabel().setAlpha(enabled ? 1 : 0.5f);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        LayoutInflater.from(context).inflate(R.layout.loading_button, this);
        final TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingButtonLayout, defStyle, 0);

        final TextView label = findLabel();
        mainActionTextAttr = getMainActionTextAttr(typeArray);
        retryActionTextAttr = getRetryActionTextAttr(typeArray);
        label.setText(mainActionTextAttr);
        label.setTextColor(getTextColor(typeArray));
        findProgressBar().getIndeterminateDrawable().setColorFilter(getLoadingColor(typeArray), PorterDuff.Mode.SRC_IN);
        typeArray.recycle();
        setClickable(true);
    }

    private int getLoadingColor(TypedArray typeArray) {
        return typeArray.getColor(R.styleable.LoadingButtonLayout_loading_color, 0);
    }

    private ProgressBar findProgressBar() {
        return (ProgressBar) findViewById(R.id.progress);
    }

    private int getTextColor(TypedArray typeArray) {
        return typeArray.getColor(R.styleable.LoadingButtonLayout_label_textColor, 0);
    }

    private String getMainActionTextAttr(TypedArray typeArray) {
        return typeArray.getString(R.styleable.LoadingButtonLayout_label_action_text);
    }

    private String getRetryActionTextAttr(TypedArray typeArray) {
        return typeArray.getString(R.styleable.LoadingButtonLayout_label_retry_text);
    }

    private TextView findLabel() {
        return (TextView) findViewById(R.id.label);
    }
}
