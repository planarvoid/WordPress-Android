package com.soundcloud.android.view.tour;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

public class Comment extends TourLayout {

    public Comment(Context context) {
        super(context);

        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tour_comment, this);
        init(context.getString(R.string.tour_comment_title));
    }
}
