package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.test.FlakyTest;
import android.text.Html;

import java.util.UUID;

public class SignupTest extends ActivityTestCase<Main> {
    public SignupTest() {
        super(Main.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.setUp();
    }

    public void testSignup() throws Exception {
        performSignup(generateEmail(), "password", "password");
        solo.assertText(R.string.authentication_add_info_msg);

        // username (max 25 characters)
        solo.enterText(0, generateUsername());
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

    public void testSignupSkip() throws Exception {
        performSignup(generateEmail(), "password", "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnButtonResId(R.string.btn_skip);
        solo.assertDialogClosed();

        // Tour
        solo.assertText(R.string.tour_start_welcome);

        solo.clickOnButtonResId(R.string.btn_done);

        // Find Friends
        solo.assertText(R.string.suggested_users_msg);

        solo.clickOnButtonResId(R.string.btn_done);

        solo.assertText(R.string.tab_stream);
    }

    @FlakyTest
    public void testSignupSwipeThroughTour() throws Exception {
        performSignup(generateEmail(), "password", "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnButtonResId(R.string.btn_skip);
        solo.assertDialogClosed();

        Tour tour = solo.assertActivity(Tour.class);

        assertEquals(solo.getString(R.string.tour_start_message), tour.getMessage());
        solo.swipeLeft();
        assertEquals(solo.getString(R.string.tour_record_message), tour.getMessage());
        solo.swipeLeft();
        assertEquals(solo.getString(R.string.tour_share_message), tour.getMessage());
        solo.swipeLeft();
        assertEquals(solo.getString(R.string.tour_follow_message), tour.getMessage());
        solo.swipeLeft();
        assertEquals(solo.getString(R.string.tour_comment_message), tour.getMessage());
        solo.swipeLeft();
        assertEquals(
            Html.fromHtml(solo.getString(R.string.tour_finish_message)).toString(), tour.getMessage());

        solo.clickOnButtonResId(R.string.btn_done);
        // Find Friends
        solo.assertText(R.string.suggested_users_msg);

        solo.clickOnButtonResId(R.string.btn_done);

        solo.assertText(R.string.tab_stream);
    }

    public void testSignupWithPhotoFromCamera() throws Exception {
        performSignup(generateEmail(), "password", "password");
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
        performSignup(generateEmail(), "password", "password");

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
        performSignup(generateEmail(), "password", "different-password");
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
        performSignup("not-an-email", "password", "password");
        solo.assertText(R.string.authentication_error_invalid_email);
    }

    public void testSignupEmailAlreadyTaken() throws Exception {
        String email = generateEmail();
        performSignup(email, "password", "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnButtonResId(R.string.btn_skip);
        solo.assertDialogClosed();

        // Tour
        solo.assertText(R.string.tour_start_welcome);

        solo.clickOnButtonResId(R.string.btn_done);

        // Find Friends
        solo.assertText(R.string.suggested_users_msg);

        solo.clickOnButtonResId(R.string.btn_done);

        solo.assertText(R.string.tab_stream);

        solo.logoutViaSettings();

        performSignup(email, "password", "password");

        solo.assertText(R.string.authentication_signup_failure_title);
        solo.clickOnOK();
    }

    public void testSignupWithTooShortPassword() throws Exception {
        performSignup(generateEmail(), "123", "123");
        solo.assertText(R.string.authentication_error_password_too_short);
    }

    public void testShouldShowEmailConfirmationDialogAfterSignupNoThanks() throws Exception {
        // perform a full signup
        testSignup();

        // exit app
        solo.goBack();
        solo.finishOpenedActivities();

        // relaunch activity
        setActivity(null);
        getActivity();

        solo.assertText(R.string.email_confirmation_you_need_to_confirm);
        solo.clickOnText(R.string.email_confirmation_confirm_later);
        solo.assertText(R.string.tab_stream);

        // relaunch activity, make sure email screen doesn't show up again
        solo.goBack();
        solo.finishOpenedActivities();

        setActivity(null);
        getActivity();
        solo.assertText(R.string.tab_stream);
    }


    public void testShouldShowEmailConfirmationDialogAfterSignupResendEmail() throws Exception {
        // perform a full signup
        testSignup();

        // exit app
        solo.goBack();
        solo.finishOpenedActivities();

        // relaunch activity
        setActivity(null);
        getActivity();

        solo.sleep(500); // should not be needed

        solo.assertText(R.string.email_confirmation_you_need_to_confirm);
        solo.clickOnText(R.string.email_confirmation_resend);
        solo.assertText(R.string.tab_stream);

        // relaunch activity, make sure email screen doesn't show up again
        solo.goBack();
        solo.finishOpenedActivities();

        setActivity(null);
        getActivity();
        solo.assertText(R.string.tab_stream);
    }


    // helper methods
    private String generateEmail() {
        String uuid = UUID.randomUUID().toString();
        return "someemail-"+uuid+"@example.com";
    }

    private String generateUsername() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(0, 24).replace("-", "");
    }

    private void performSignup(String email, String password, String passwordConfirm) {
        solo.clickOnButtonResId(R.string.btn_signup);
        solo.assertText(R.string.authentication_sign_up);
        solo.clearEditText(0);
        solo.enterText(0, email);
        solo.enterText(1, password);
        solo.enterText(2, passwordConfirm);

        solo.clickOnButtonResId(R.string.btn_signup);
    }
}
