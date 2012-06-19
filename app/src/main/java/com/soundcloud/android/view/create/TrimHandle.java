package com.soundcloud.android.view.create;

import static android.widget.RelativeLayout.*;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class TrimHandle extends ImageButton {

    public enum HandleType {
        LEFT(getLeftLayoutParams(), R.drawable.left_handle_states, R.dimen.trim_handle_left_margin_offset),
        RIGHT(getRightLayoutParams(), R.drawable.right_handle_states, R.dimen.trim_handle_right_margin_offset);

        private final RelativeLayout.LayoutParams layoutParams;
        private final int backgroundResId;
        private final int marginOffsetDimenId;

        HandleType(RelativeLayout.LayoutParams layoutParams, int backgroundResId, int marginOffsetDimenId) {
            this.layoutParams = layoutParams;
            this.backgroundResId = backgroundResId;
            this.marginOffsetDimenId = marginOffsetDimenId;
        }
    }

    private HandleType mType;
    private int mMarginOffset;

    public TrimHandle(Context context, HandleType type) {
        super(context);
        setLayoutParams(type.layoutParams);
        setBackgroundResource(type.backgroundResId);
        setClickable(false);
        mType = type;
        mMarginOffset = (int) context.getResources().getDimension(type.marginOffsetDimenId);
    }


    @Override
    public RelativeLayout.LayoutParams getLayoutParams() {
        return (RelativeLayout.LayoutParams) super.getLayoutParams();
    }

    public void update(int position){
        if (mType == HandleType.LEFT){
            getLayoutParams().leftMargin = position + mMarginOffset;
        } else {
            getLayoutParams().rightMargin = position + mMarginOffset;
        }
        requestLayout();
    }

    private static RelativeLayout.LayoutParams getLeftLayoutParams() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(ALIGN_PARENT_BOTTOM, 1);
        lp.addRule(ALIGN_PARENT_LEFT, 1);
        return lp;
    }

    public static RelativeLayout.LayoutParams getRightLayoutParams() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(ALIGN_PARENT_BOTTOM, 1);
        lp.addRule(ALIGN_PARENT_RIGHT, 1);
        return lp;
    }
}
