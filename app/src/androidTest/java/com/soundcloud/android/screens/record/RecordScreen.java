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
    private static final int EDIT_BUTTON = R.id.btn_edit;
    private static final int PLAY_BUTTON = R.id.btn_play;
    private static final int CHRONOMETER = R.id.chronometer;
    private static final int BOTTOM_BAR = R.id.bottom_bar;

    public RecordScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("recording_fragment");
    }

    public String getTitle() {
        return actionBar().getTitle();
    }

    public RecordScreen clickPlayButton() {
        getPlayButton().click();
        return this;
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public RecordScreen clickRecordButton() {
        getActionButton().click();
        return this;
    }

    public RecordScreen clickEditButton() {
        getEditButton().click();
        return this;
    }

    public RecordScreen clickApplyButton() {
        getApplyButton().click();
        return this;
    }

    public RecordScreen waitForRecord() {
        waiter.waitTwoSeconds();
        return this;
    }

    public boolean hasRecording() {
        return getDeleteRecordingButton().isVisible();
    }

    public RecordScreen deleteRecording() {
        getDeleteRecordingButton().click();
        testDriver.waitForDialogToOpen(2000l);
        acceptDeleteRecording();
        return this;
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

    public ViewElement getApplyButton() {
        return getBottomBarButton("Apply");
    }

    public ViewElement getRevertButton() {
        return getBottomBarButton("Revert to original");
    }

    public ViewElement getEditButton() {
        return testDriver.findElement(With.id(EDIT_BUTTON));
    }

    private ViewElement getActionButton() {
        return testDriver.findElement(With.id(ACTION_BUTTON));
    }

    private ViewElement getPlayButton() {
        return testDriver.findElement(With.id(PLAY_BUTTON));
    }


    public RecordMetadataScreen clickNext() {
        getNextButton().click();
        return new RecordMetadataScreen(testDriver);
    }

    public RecordScreen deleteRecordingIfPresent() {
        if(hasRecording()) {
            deleteRecording();
        }
        return this;
    }

    public TextElement getChronometer() {
        return new TextElement(testDriver.findElement(With.id(CHRONOMETER)));
    }

    public RecordScreen startRecording() {
        clickRecordButton();
        return this;
    }

    public RecordScreen waitAndPauseRecording() {
        waitForRecord();
        clickRecordButton();
        return this;
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
