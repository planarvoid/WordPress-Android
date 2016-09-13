package com.soundcloud.android.screens.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.CheckableElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.stations.StationInfoActivity;

public class StationHomeScreen extends Screen {

    private static final Class ACTIVITY = StationInfoActivity.class;

    public StationHomeScreen(Han testDriver) {
        super(testDriver);
    }

    public CollectionScreen goBack() {
        testDriver.goBack();
        return new CollectionScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public VisualPlayerElement clickPlay() {
        playButton().click();
        return new VisualPlayerElement(testDriver);
    }

    public String stationTitle() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.station_title))).getText();
    }

    public boolean isStationLiked() {
        return likeElement().isChecked();
    }

    public StationHomeScreen clickStationLike() {
        likeElement().click();
        return this;
    }

    private ViewElement playButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_play));
    }

    private CheckableElement likeElement() {
        return new CheckableElement(testDriver.findOnScreenElement(With.id(R.id.toggle_like)));
    }
}
