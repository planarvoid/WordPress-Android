package com.soundcloud.android.activity.tour;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.os.Bundle;

public class Record extends TourActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_record);

        init(getString(R.string.tour_record_title),Consts.TourActivityIndexes.RECORD);
    }
}
