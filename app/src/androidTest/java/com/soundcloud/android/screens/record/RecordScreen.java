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
        waiter.waitForElement(actionBar().title(), testDriver.getString(R.string.rec_title_editing));
        return this;
    }

    public RecordScreen clickApplyButton() {
        getApplyButton().click();
        waiter.waitForElement(actionBar().title(), testDriver.getString(R.string.rec_title_idle_play));
        return this;
    }

    public RecordScreen clickPlayButton() {
        getPlayButton().click();
        waiter.waitForElement(actionBar().title(), testDriver.getString(R.string.rec_title_playing));
        return this;
    }

    public RecordScreen deleteRecording() {
        deleteButton().click();
        testDriver.waitForDialogToOpen(2000l);
        acceptDeleteRecording();
        return this;
    }

    public boolean hasNextButton() {
        return nextButton().isVisible();
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
        nextButton().click();
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
        waiter.waitForElement(actionBar().title(), testDriver.getString(R.string.rec_title_recording));
        return this;
    }

    public RecordScreen stopRecording() {
        clickRecordButton();
        waiter.waitForElement(actionBar().title(), testDriver.getString(R.string.rec_title_idle_play));
        return this;
    }

    public RecordScreen waitAndPauseRecording() {
        waiter.waitTwoSeconds();
        stopRecording();
        return this;
    }

    private boolean hasRecording() {
        return deleteButton().isVisible();
    }

    private TextElement chronometer() {
        return new TextElement(testDriver.findElement(With.id(R.id.chronometer)));
    }

    private RecordScreen acceptDeleteRecording() {
        testDriver.findElement(text(testDriver.getString(R.string.btn_yes))).click();
        testDriver.waitForDialogToClose(1000l);
        return this;
    }

    private ViewElement getActionButton() {
        return testDriver.findElement(With.id(R.id.btn_action));
    }

    private ViewElement nextButton() {
        return testDriver.findElement(With.id(R.id.btn_next));
    }

    private ViewElement deleteButton() {
        return testDriver.findElement(With.id(R.id.btn_delete));
    }

    private ViewElement getPlayButton() {
        return testDriver.findElement(With.id(R.id.btn_play));
    }

    public boolean hasRecordedTrack() {
        return deleteButton().isVisible();
    }
}
