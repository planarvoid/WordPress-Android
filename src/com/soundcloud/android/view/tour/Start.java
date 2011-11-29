package com.soundcloud.android.view.tour;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class Start extends TourLayout {

    public Start(Context context) {
        super(context);

        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tour_start, this);
        init(context.getString(R.string.tour_comment_title));
    }
}
