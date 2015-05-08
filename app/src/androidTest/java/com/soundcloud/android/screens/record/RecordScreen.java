package com.soundcloud.android.screens.record;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public class RecordScreen extends Screen {
    private static final Class ACTIVITY = RecordActivity.class;
    private static final int ACTION_BUTTON = R.id.btn_action;
    private static final int CHRONOMETER = R.id.chronometer;
    private static final int BOTTOM_BAR = R.id.bottom_bar;

    public RecordScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("recording_fragment");
    }

    public String getTitle() {
        return actionBar().getTitle();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public RecordScreen clickRecordButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        waiter.waitForElements(ACTION_BUTTON);
        testDriver
                .findElements(With.id(ACTION_BUTTON))
                .get(0).click();
        return this;
    }

    public boolean hasRecording() {
        return getDeleteRecordingButton().isVisible();
    }

    public RecordScreen acceptDeleteRecording() {
        testDriver.findElement(text(testDriver.getString(R.string.yes))).click();
        testDriver.waitForDialogToClose(1000l);

        return this;
    }

    public ViewElement getDeleteRecordingButton() {
        return getBottomBarButton("Delete");
    }

    public ViewElement getNextButton() {
        return getBottomBarButton("Next");
    }

    public RecordMetadataScreen clickNext() {
        getNextButton().click();
        return new RecordMetadataScreen(testDriver);
    }

    public RecordScreen deleteRecordingIfPresent() {
        if(hasRecording()) {
            getDeleteRecordingButton().click();
            testDriver.waitForDialogToOpen(2000l);
            acceptDeleteRecording();
            testDriver.waitForDialogToClose(2000l);
        }
        return this;
    }

    public TextElement getChronometer() {
        return new TextElement(testDriver.findElement(With.id(CHRONOMETER)));
    }

    private ViewElement getBottomBarButton(String withText) {
        ViewElement bottomBar = testDriver.findElement(With.id(BOTTOM_BAR));

        if(bottomBar.isVisible()){
            return bottomBar.findElement(With.text(withText));
        } else{
            return new EmptyViewElement(null);
        }
    }


}
