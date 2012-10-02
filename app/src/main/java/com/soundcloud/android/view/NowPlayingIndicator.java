package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

public class NowPlayingIndicator extends ProgressBar {

    public NowPlayingIndicator(Context context) {
        super(context);
        init();
    }

    public NowPlayingIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NowPlayingIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setProgress((getProgress() + 10) % 100);
            }
        });
    }


}
