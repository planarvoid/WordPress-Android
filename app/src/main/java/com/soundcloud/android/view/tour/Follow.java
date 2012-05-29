package com.soundcloud.android.view.tour;

import android.content.Context;
import android.view.LayoutInflater;

import com.soundcloud.android.R;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

@Tracking(page = Page.Entry_tour__people)
public class Follow extends TourLayout {

    public Follow(Context context) {
        super(context);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tour_follow, this);
    }
}
