package com.soundcloud.android.activity.tour;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

public class Record extends TourActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_record);

        init(getString(R.string.tour_record_title),Consts.TourActivityIndexes.RECORD);
    }
}
