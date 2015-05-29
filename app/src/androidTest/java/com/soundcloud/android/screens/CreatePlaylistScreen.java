package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class CreatePlaylistScreen extends Screen {

    public CreatePlaylistScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("create_new_set_dialog");
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag("create_new_set_dialog");
    }

    public ViewElement offlineCheck() {
        return testDriver.findElement(With.id(R.id.chk_offline));
    }

    public CreatePlaylistScreen enterTitle(String title) {
        titleInputField().clearText();
        titleInputField().typeText(title);
        return this;
    }

    public StreamScreen clickDoneAndReturnToStream() {
        getDoneButton().click();
        return new StreamScreen(testDriver);
    }

    public VisualPlayerElement clickDoneAndReturnToPlayer() {
        getDoneButton().click();
        return new VisualPlayerElement(testDriver);
    }

    private EditTextElement titleInputField() {
        return new EditTextElement(testDriver.findElement(With.id(android.R.id.edit)));
    }

    private ViewElement getDoneButton() {
        return testDriver.findElement(With.id(android.R.id.button1));
    }
}
