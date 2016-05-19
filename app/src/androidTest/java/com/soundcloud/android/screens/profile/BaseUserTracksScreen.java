package com.soundcloud.android.screens.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import java.util.List;

public abstract class BaseUserTracksScreen extends BaseUserScreen {
    public BaseUserTracksScreen(Han testDriver) {
        super(testDriver);
    }

    public VisualPlayerElement clickFirstTrack() {
        final List<ViewElement> playableItems = playableRecyclerView().findOnScreenElements(With.id(R.id.track_list_item));
        playableItems.get(0).click();

        final VisualPlayerElement visualPlayer = new VisualPlayerElement(testDriver);
        visualPlayer.waitForExpandedPlayer();

        return visualPlayer;
    }
}
