package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class ShareHeaderView extends TextView {

    public ShareHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setText(Html.fromHtml(getResources().getString(R.string.share_to_soundcloud)));
    }

    public ShareHeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setText(Html.fromHtml(getResources().getString(R.string.share_to_soundcloud)));
    }


}
