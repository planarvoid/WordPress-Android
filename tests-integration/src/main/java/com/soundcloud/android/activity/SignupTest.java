package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.SignupDetails;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.ActivityInstrumentationTestCase2;

import java.io.File;
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

    public void testSignup() throws Exception {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);

        solo.clearEditText(0);
        String uuid = UUID.randomUUID().toString();
        solo.enterText(0, "someemail-"+uuid+"@example.com");
        solo.enterText(1, "password");
        solo.enterText(2, "password");

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_add_info_msg);

        // username (max 25 characters)
        solo.enterText(0, uuid.substring(0, 24).replace("-", ""));
        solo.clickOnButtonResId(R.string.btn_save);

        solo.assertDialogClosed();

        // Tour
        solo.assertText(R.string.tour_start_welcome);

        solo.clickOnButtonResId(R.string.btn_done);

        // Find Friends
        solo.assertText(R.string.suggested_users_msg);

        solo.clickOnButtonResId(R.string.btn_done);

        solo.assertText(R.string.tab_stream);
    }

    public void testSignupWithPhotoFromCamera() throws Exception {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);

        solo.clearEditText(0);
        String uuid = UUID.randomUUID().toString();
        solo.enterText(0, "someemail-"+uuid+"@example.com");
        solo.enterText(1, "password");
        solo.enterText(2, "password");

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnText(R.string.add_image);
        solo.assertText(R.string.image_where); // How would you like to add an image?

        solo.clickOnText(R.string.take_new_picture);

        // FakeCamera will provide an image
        solo.sleep(1000);

        // make sure add image prompt is gone
        assertFalse(solo.searchText(solo.getString(R.string.add_image), true));

        // clear image
        solo.clickLongOnView(R.id.artwork);
        solo.assertText(R.string.add_image);
    }

    public void testSignupWithExistingPhoto() throws Exception {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);

        solo.clearEditText(0);
        String uuid = UUID.randomUUID().toString();
        solo.enterText(0, "someemail-"+uuid+"@example.com");
        solo.enterText(1, "password");
        solo.enterText(2, "password");

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertDialogClosed();
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnText(R.string.add_image);
        solo.assertText(R.string.image_where); // How would you like to add an image?

        solo.clickOnText(R.string.use_existing_image);

        // FakeGallery will provide an image
        solo.sleep(1000);

        // make sure add image prompt is gone
        assertFalse(solo.searchText(solo.getString(R.string.add_image), true));

        // clear image
        solo.clickLongOnView(R.id.artwork);
        solo.assertText(R.string.add_image);
    }

    public void testSignupWithNonMatchingPasswords() throws Exception {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);

        solo.clearEditText(0);
        solo.enterText(0, "someemail-"+ UUID.randomUUID().toString() +"@example.com");
        solo.enterText(1, "password");
        solo.enterText(2, "anotherpassword");

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_error_password_mismatch);
    }

    public void testSignupWithoutInput() throws Exception {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_error_incomplete_fields);
    }

    public void testSignupWithInvalidEmail() throws Exception {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);

        solo.clearEditText(0);
        solo.enterText(0, "not-an-email");
        solo.enterText(1, "password");
        solo.enterText(2, "password");

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_error_invalid_email);
    }


    public void testSignupWithTooShortPassword() throws Exception {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);

        solo.clearEditText(0);
        solo.enterText(0, "someemail-"+ UUID.randomUUID().toString() +"@example.com");
        solo.enterText(1, "123");
        solo.enterText(2, "123");

        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_error_password_too_short);
    }
}
