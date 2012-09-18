package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import com.soundcloud.android.R;

public class ClearText extends EditText{

    private Drawable mOriginalRightDrawable;
    private OnClickListener mDefaultDrawableListener;

    public ClearText(Context context) {
        super(context);
        init();
    }

    public ClearText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClearText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setDefaultDrawableClickListener(OnClickListener listener){
        mDefaultDrawableListener = listener;
    }

    private void init(){
        String value = "";//any text you are pre-filling in the EditText
        setText(value);

        mOriginalRightDrawable = getCompoundDrawables()[2];

        final Drawable x = getResources().getDrawable(R.drawable.ic_txt_delete_states);//your x image, this one from standard android images looks pretty good actually
        x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());
        setCompoundDrawables(null, null, "".equals(value) ? mOriginalRightDrawable : x, null);
        setCompoundDrawablePadding((int) (getResources().getDisplayMetrics().density * 5));
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getCompoundDrawables()[2] == null) {
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (event.getX() > getWidth() - getPaddingRight() - x.getIntrinsicWidth()) {
                    if (TextUtils.isEmpty(getText())){
                        if (mDefaultDrawableListener != null){
                            mDefaultDrawableListener.onClick(ClearText.this);
                        }
                    } else {
                        setText("");
                        setCompoundDrawables(null, null, mOriginalRightDrawable, null);
                    }
                }
                return false;
            }
        });
        addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setCompoundDrawables(null, null, getText().toString().equals("") ? mOriginalRightDrawable : x, null);
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
