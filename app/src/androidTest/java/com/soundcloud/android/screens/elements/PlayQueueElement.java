package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class PlayQueueElement extends Element {

    public PlayQueueElement(Han testDriver) {
        super(testDriver, With.id(R.id.play_queue_body));
    }

    public PlayQueueElement pressShuffleButton() {
        shuffleButton().click();
        return this;
    }

    public VisualPlayerElement pressCloseButton() {
        closeButton().click();
        return new VisualPlayerElement(testDriver);
    }

    public PlayQueueElement pressRepeatButton() {
        repeatButton().click();
        return this;
    }

    private ViewElement shuffleButton() {
        return testDriver.findOnScreenElement(With.id(R.id.shuffle_button));
    }

    private ViewElement repeatButton() {
        return testDriver.findOnScreenElement(With.id(R.id.repeat_button));
    }

    private ViewElement closeButton() {
        return testDriver.findOnScreenElement(With.id(R.id.close_play_queue));
    }

}
