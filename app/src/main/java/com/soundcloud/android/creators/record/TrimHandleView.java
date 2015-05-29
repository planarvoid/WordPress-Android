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

    public TrimHandleView(Context context, HandleType type) {
        super(context);
        setLayoutParams(type.layoutParams);
        setBackgroundResource(type.backgroundResId);
        setClickable(false);
        this.type = type;
    }

    @Override
    public RelativeLayout.LayoutParams getLayoutParams() {
        return (RelativeLayout.LayoutParams) super.getLayoutParams();
    }

    public void update(int position) {
        if (type == HandleType.LEFT) {
            getLayoutParams().leftMargin = position;
        } else {
            getLayoutParams().rightMargin = position;
        }

        requestLayout();
    }

    public static RelativeLayout.LayoutParams getRightLayoutParams() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(ALIGN_PARENT_BOTTOM, 1);
        lp.addRule(ALIGN_PARENT_RIGHT, 1);
        return lp;
    }

    private static RelativeLayout.LayoutParams getLeftLayoutParams() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(ALIGN_PARENT_BOTTOM, 1);
        lp.addRule(ALIGN_PARENT_LEFT, 1);
        return lp;
    }

    public enum HandleType {
        LEFT(getLeftLayoutParams(), R.drawable.ic_record_handle_l),
        RIGHT(getRightLayoutParams(), R.drawable.ic_record_handle_r);

        private final RelativeLayout.LayoutParams layoutParams;
        private final int backgroundResId;

        HandleType(RelativeLayout.LayoutParams layoutParams, int backgroundResId) {
            this.layoutParams = layoutParams;
            this.backgroundResId = backgroundResId;
        }
    }
}
