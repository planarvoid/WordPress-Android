package com.soundcloud.android.activity.tour;

import android.os.Bundle;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

public class Share extends TourActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_share);
        init(getString(R.string.tour_share_title),Consts.TourActivityIndexes.SHARE);
    }
}
