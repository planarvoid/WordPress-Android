package com.soundcloud.android.creators.record;

import static android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM;
import static android.widget.RelativeLayout.ALIGN_PARENT_LEFT;
import static android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class TrimHandleView extends ImageButton {

    private final HandleType type;
    private final int marginOffset;

    public TrimHandleView(Context context, HandleType type) {
        super(context);
        setLayoutParams(type.layoutParams);
        setBackgroundResource(type.backgroundResId);
        setClickable(false);
        this.type = type;
        marginOffset = (int) context.getResources().getDimension(type.marginOffsetDimenId);
    }

    @Override
    public RelativeLayout.LayoutParams getLayoutParams() {
        return (RelativeLayout.LayoutParams) super.getLayoutParams();
    }

    public void update(int position) {
        if (type == HandleType.LEFT) {
            getLayoutParams().leftMargin = position + marginOffset;
        } else {
            getLayoutParams().rightMargin = position + marginOffset;
        }
        requestLayout();
    }

    public static RelativeLayout.LayoutParams getRightLayoutParams() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(ALIGN_PARENT_BOTTOM, 1);
        lp.addRule(ALIGN_PARENT_RIGHT, 1);
        return lp;
    }

    public int getRightWithMargin() {
        return getRight() + marginOffset;
    }

    public int getLeftWithMargin() {
        return getLeft() - marginOffset;
    }

    private static RelativeLayout.LayoutParams getLeftLayoutParams() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(ALIGN_PARENT_BOTTOM, 1);
        lp.addRule(ALIGN_PARENT_LEFT, 1);
        return lp;
    }

    public enum HandleType {
        LEFT(getLeftLayoutParams(), R.drawable.ic_record_handle_l, R.dimen.trim_handle_left_margin_offset),
        RIGHT(getRightLayoutParams(), R.drawable.ic_record_handle_r, R.dimen.trim_handle_right_margin_offset);

        private final RelativeLayout.LayoutParams layoutParams;
        private final int backgroundResId;
        private final int marginOffsetDimenId;

        HandleType(RelativeLayout.LayoutParams layoutParams, int backgroundResId, int marginOffsetDimenId) {
            this.layoutParams = layoutParams;
            this.backgroundResId = backgroundResId;
            this.marginOffsetDimenId = marginOffsetDimenId;
        }
    }
}
