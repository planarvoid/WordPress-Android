package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.utils.ViewUtils;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class TopPaddingDecorator extends RecyclerView.ItemDecoration {

    private final int topPadding;

    public TopPaddingDecorator(Context context) {
        this.topPadding = ViewUtils.dpToPx(context, 72);
    }

    public TopPaddingDecorator(Context context, int topPaddingDp) {
        this.topPadding = ViewUtils.dpToPx(context, topPaddingDp);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == 0) {
            outRect.top = topPadding;
            outRect.left = 0;
            outRect.right = 0;
            outRect.bottom = 0;
        }
    }
}
