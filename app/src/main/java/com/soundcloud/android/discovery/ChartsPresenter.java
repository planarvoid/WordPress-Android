package com.soundcloud.android.discovery;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import rx.Observable;

import android.util.Log;

import javax.inject.Inject;
import java.util.List;

class ChartsPresenter {
    @Inject
    ChartsPresenter() {
    }

    void onNewAndHotClicked() {
        Log.d("charts", "new and hot");
    }

    void onTopFiftyClicked() {
        Log.d("charts", "top 50");
    }
}
