package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;

import java.util.UUID;

public class SignupTest extends ActivityInstrumentationTestCase2<Main> {
    private Han solo;

    public SignupTest() {
        super(Main.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        solo = new Han(getInstrumentation(), getActivity());
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        if (solo != null) {
            solo.finishOpenedActivities();
        }
        super.tearDown();
    }

    @FlakyTest
    public void testSignup() throws Exception {
        solo.clickOnButton(solo.getString(R.string.btn_signup));
        solo.assertText(R.string.authentication_sign_up);

        solo.clearEditText(0);
        String uuid = UUID.randomUUID().toString();
        solo.enterText(0, "someemail-"+uuid+"@example.com");
        solo.enterText(1, "password");
        solo.enterText(2, "password");

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertDialogClosed(20 * 1000);
        solo.assertText(R.string.authentication_add_info_msg);

        // username (max 25 characters)
        solo.enterText(0, uuid.substring(0, 24).replace("-", ""));
        solo.clickOnButtonResId(R.string.btn_save);

        solo.assertDialogClosed(20 * 1000);

        // Tour
        solo.assertText(R.string.tour_start_welcome);

        solo.clickOnButtonResId(R.string.btn_done);

        // Find Friends
        solo.assertText(R.string.suggested_users_msg);

        solo.clickOnButtonResId(R.string.btn_done);

        solo.assertText(R.string.tab_stream);
    }
}
