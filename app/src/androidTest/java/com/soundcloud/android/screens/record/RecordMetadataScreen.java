package com.soundcloud.android.screens.record;

import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.RadioButtonElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public class RecordMetadataScreen extends Screen {
    private static final Class ACTIVITY = RecordActivity.class;

    public RecordMetadataScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("metadata_fragment");
    }

    public String getTitle() {
        return actionBar().getTitle();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public RecordMetadataScreen setTitle(String title) {
        getTitleEditText().typeText(title);
        return this;
    }

    public RecordScreen clickUploadButton() {
        testDriver.findElement(With.id(R.id.btn_action)).click();
        return new RecordScreen(testDriver);
    }

    public RecordMetadataScreen setPrivate() {
        getPrivateRadioButton().click();
        return this;
    }

    private EditTextElement getTitleEditText() {
        return new EditTextElement(testDriver.findElement(With.id(R.id.title)));
    }

    private RadioButtonElement getPrivateRadioButton() {
        return new RadioButtonElement(testDriver.findElement(With.id(R.id.rdo_private)));
    }
}
