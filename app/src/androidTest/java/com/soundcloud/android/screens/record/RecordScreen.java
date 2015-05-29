package com.soundcloud.android.screens.record;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public class RecordScreen extends Screen {
    private static final Class ACTIVITY = RecordActivity.class;

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

    public RecordScreen clickPlayButton() {
        getPlayButton().click();
        return this;
    }

    public RecordScreen waitForRecord() {
        waiter.waitTwoSeconds();
        return this;
    }

    public RecordScreen deleteRecording() {
        getDeleteButton().click();
        testDriver.waitForDialogToOpen(2000l);
        acceptDeleteRecording();
        return this;
    }

    public ViewElement getDeleteButton() {
        return testDriver.findElement(With.id(R.id.btn_delete));
    }

    public ViewElement getNextButton() {
        return testDriver.findElement(With.id(R.id.btn_next));
    }

    public ViewElement getApplyButton() {
        return testDriver.findElement(With.id(R.id.btn_apply));
    }

    public ViewElement getRevertButton() {
        return testDriver.findElement(With.id(R.id.btn_revert));
    }

    public ViewElement getEditButton() {
        return testDriver.findElement(With.id(R.id.btn_edit));
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

    public String getChronometer() {
        return chronometer().getText();
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

    private boolean hasRecording() {
        return getDeleteButton().isVisible();
    }

    private TextElement chronometer() {
        return new TextElement(testDriver.findElement(With.id(R.id.chronometer)));
    }

    private RecordScreen acceptDeleteRecording() {
        testDriver.findElement(text(testDriver.getString(R.string.yes))).click();
        testDriver.waitForDialogToClose(1000l);
        return this;
    }

    private ViewElement getActionButton() {
        return testDriver.findElement(With.id(R.id.btn_action));
    }

    private ViewElement getPlayButton() {
        return testDriver.findElement(With.id(R.id.btn_play));
    }
}
