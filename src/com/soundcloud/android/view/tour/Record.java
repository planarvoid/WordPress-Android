package com.soundcloud.android.view.tour;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;

public class Record extends TourLayout {

    public Record(Context context) {
        super(context);

        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tour_record, this);
        init(context.getString(R.string.tour_record_title));
    }
}
