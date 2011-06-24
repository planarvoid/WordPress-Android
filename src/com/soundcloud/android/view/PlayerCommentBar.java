package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

public class PlayerCommentBar extends RelativeLayout {

    private static final String TAG = "PlayerCommentBar";

    public PlayerCommentBar(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        
        LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_comment_bar, this);
    }


}
