package com.soundcloud.android.screens.record;


import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.NonNull;

public class RecordScreen extends Screen {
    private static final Class ACTIVITY = RecordActivity.class;

    public RecordScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag("recording_fragment");
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
        testDriver.waitForDialogToOpen(2000L);
        acceptDeleteRecording();
        return this;
    }

    public boolean hasNextButton() {
        return nextButton().isOnScreen();
    }

    public ViewElement getNextButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_next));
    }

    public ViewElement getApplyButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_apply));
    }

    public ViewElement getRevertButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_revert));
    }

    public ViewElement getEditButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_edit));
    }

    public RecordMetadataScreen clickNext() {
        nextButton().click();
        return new RecordMetadataScreen(testDriver);
    }

    public RecordScreen deleteRecordingIfPresent() {
        if (hasRecording()) {
            deleteRecording();
        }
        return this;
    }

    public String getChronometer() {
        return chronometer().getText();
    }

    public RecordScreen startRecording() {
        // Wait until the chronometer jumps from it's initial value (e.g. "Record") to the current recording length
        String initialText = chronometer().getText();
        clickRecordButton();
        waiter.waitForElement(chronometerValueHasChanged(initialText));

        // Assert that the chronometer advances
        String preRecordingText = chronometer().getText();
        waiter.waitForElement(chronometerValueHasChanged(preRecordingText));
        // wait 2 more seconds to make sure we at least record a couple of seconds
        waiter.waitTwoSeconds();
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

    @NonNull
    private With chronometerValueHasChanged(final String preRecordingText) {
        return new With() {
            @Override
            public String getSelector() {
                return "chronometer has a different value";
            }

            @Override
            public boolean apply(@Nullable ViewElement input) {
                return !chronometer().getText().equals(preRecordingText);
            }
        };
    }

    private boolean hasRecording() {
        return deleteButton().isOnScreen();
    }

    private TextElement chronometer() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.chronometer)));
    }

    private RecordScreen acceptDeleteRecording() {
        testDriver.findOnScreenElement(text(testDriver.getString(R.string.btn_yes))).click();
        testDriver.waitForDialogToClose(1000L);
        return this;
    }

    private ViewElement getActionButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_action));
    }

    private ViewElement nextButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_next));
    }

    private ViewElement deleteButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_delete));
    }

    private ViewElement getPlayButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_play));
    }

    public boolean hasRecordedTrack() {
        return deleteButton().isOnScreen();
    }
}
