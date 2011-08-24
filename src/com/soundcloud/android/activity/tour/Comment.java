package com.soundcloud.android.activity.tour;

import android.os.Bundle;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

public class Comment extends TourActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_comment);
        init(getString(R.string.tour_comment_title),Consts.TourActivityIndexes.COMMENT);
    }
}
