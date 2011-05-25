package com.soundcloud.android.utils;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

public class ClickSpan extends ClickableSpan {
    private OnClickListener mListener;
    private boolean mUnderline;

        public ClickSpan(OnClickListener listener) {
            this(listener, false);
        }

        public ClickSpan(OnClickListener listener, boolean underline) {
            mListener = listener;
            mUnderline = underline;
        }

        @Override
        public void onClick(View widget) {
           if (mListener != null) mListener.onClick();
        }

        @Override public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(mUnderline);
        }

        public interface OnClickListener {
            void onClick();
        }
    }