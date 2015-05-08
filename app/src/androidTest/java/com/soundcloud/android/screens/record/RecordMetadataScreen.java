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

public class RecordMetadataScreen extends Screen {
    private static final Class ACTIVITY = RecordActivity.class;
    private static final int ACTION_BUTTON = R.id.btn_action;

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

    public RecordMetadataScreen clickUploadButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        waiter.waitForElements(ACTION_BUTTON);
        testDriver
                .findElements(With.id(ACTION_BUTTON))
                .get(0).click();
        return this;
    }
}
