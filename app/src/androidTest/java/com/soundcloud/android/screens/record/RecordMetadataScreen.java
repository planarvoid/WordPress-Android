package com.soundcloud.android.screens.record;

import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.CheckableElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public class RecordMetadataScreen extends Screen {
    private static final Class ACTIVITY = RecordActivity.class;

    public RecordMetadataScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag("metadata_fragment");
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
        testDriver.findOnScreenElement(With.id(R.id.btn_action)).click();
        return new RecordScreen(testDriver);
    }

    public RecordMetadataScreen setPrivate() {
        getPrivateRadioButton().click();
        return this;
    }

    private EditTextElement getTitleEditText() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(R.id.title)));
    }

    private CheckableElement getPrivateRadioButton() {
        return new CheckableElement(testDriver.findOnScreenElement(With.id(R.id.rdo_private)));
    }
}
