package com.soundcloud.android.screens.record;

import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.RadioButtonElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public class RecordMetadataScreen extends Screen {
    private static final Class ACTIVITY = RecordActivity.class;
    private static final int ACTION_BUTTON = R.id.btn_action;
    private static final int RADIO_PRIVATE = R.id.rdo_private;
    private static final int TITLE_TEXT_EDIT = R.id.title;

    public RecordMetadataScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("metadata_fragment");
    }

    public String getTitle() {
        return actionBar().getTitle();
    }

    public RecordMetadataScreen setTitle(String title) {
        getTitleEditText().typeText(title);
        return this;
    }

    private EditTextElement getTitleEditText() {
        return new EditTextElement(testDriver.findElement(With.id(TITLE_TEXT_EDIT)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public RecordScreen clickUploadButton() {
        testDriver.findElement(With.id(ACTION_BUTTON)).click();
        return new RecordScreen(testDriver);
    }

    public RecordMetadataScreen setPrivate() {
        getPrivateRadioButton().click();
        return this;
    }

    private RadioButtonElement getPrivateRadioButton() {
        return getRadioButton(RADIO_PRIVATE);
    }

    private RadioButtonElement getRadioButton(int id) {
        ViewElement view = testDriver.findElement(With.id(id));
        return new RadioButtonElement(view);
    }
}
