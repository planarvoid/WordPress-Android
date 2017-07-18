package com.soundcloud.android.screens.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.settings.LegalActivity;

public class LegalScreen extends Screen {

    private static final Class ACTIVITY = LegalActivity.class;

    public LegalScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public CopyrightScreen clickCopyrightLink() {
        copyrightLink().click();
        return new CopyrightScreen(testDriver);
    }

    private ViewElement copyrightLink() {
        return testDriver.findOnScreenElement(With.text(R.string.copyright_information));
    }
}
