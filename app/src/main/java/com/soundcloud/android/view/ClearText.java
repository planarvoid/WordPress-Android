package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ClearText extends AppCompatEditText {

    private Drawable originalRightDrawable;
    private OnClickListener defaultDrawableListener;

    public ClearText(Context context) {
        super(context);
        init();
    }

    public ClearText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

        applyCustomFont(context, this, attrs);
    }

    public ClearText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();

        applyCustomFont(context, this, attrs);
    }

    public void setDefaultDrawableClickListener(OnClickListener listener) {
        defaultDrawableListener = listener;
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private void init() {
        String value = "";//any text you are pre-filling in the EditText
        setText(value);

        originalRightDrawable = getCompoundDrawables()[2];

        final Drawable x = getResources().getDrawable(R.drawable.header_search_cancel);//your x image, this one from standard android images looks pretty good actually
        x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());
        setCompoundDrawables(null, null, "".equals(value) ? originalRightDrawable : x, null);
        setCompoundDrawablePadding((int) (getResources().getDisplayMetrics().density * 5));
        setOnTouchListener((v, event) -> {
            if (getCompoundDrawables()[2] == null) {
                return false;
            }
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }
            if (event.getX() > getWidth() - getPaddingRight() - x.getIntrinsicWidth()) {
                if (TextUtils.isEmpty(getText())) {
                    if (defaultDrawableListener != null) {
                        defaultDrawableListener.onClick(ClearText.this);
                    }
                } else {
                    setText("");
                    setCompoundDrawables(null, null, originalRightDrawable, null);
                }
            }
            return false;
        });
        addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setCompoundDrawables(null, null, getText().toString().equals("") ? originalRightDrawable : x, null);
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }

}
