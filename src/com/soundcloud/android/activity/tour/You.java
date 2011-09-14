package com.soundcloud.android.activity.tour;

import android.os.Bundle;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

public class You extends TourActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_you);
        init(getString(R.string.tour_you_title),Consts.TourActivityIndexes.YOU);
    }
}
