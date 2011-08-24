package com.soundcloud.android.activity.tour;

import android.os.Bundle;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

public class Follow extends TourActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_follow);
        init(getString(R.string.tour_follow_title),Consts.TourActivityIndexes.FOLLOW);
    }
}
