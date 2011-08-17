package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.CloudUtils;

public class CommentPanel extends CommentDisplay {
    public CommentPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void init(){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.comment_panel, this);

        final float density = getResources().getDisplayMetrics().density;
        setBackgroundColor(getResources().getColor(R.color.commentPanelBg));
        setPadding(0, (int) (5 * density), 0, (int) (15 * density));
        super.init();
    }
}