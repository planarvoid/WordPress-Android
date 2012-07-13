package com.soundcloud.android.view.tour;

import com.soundcloud.android.R;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

import android.content.Context;

@Tracking(page = Page.Entry_tour__record)
public class Record extends TourLayout {
    public Record(Context context) {
        super(context, R.layout.tour_record);
    }
}
