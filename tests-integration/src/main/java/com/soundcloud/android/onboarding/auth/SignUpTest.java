package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.crop.CropImageActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.test.suitebuilder.annotation.Suppress;
import android.widget.EditText;

import java.util.UUID;

public class SignUpTest extends ActivityTestCase<OnboardActivity> {
    public SignUpTest() {
        super(OnboardActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (applicationProperties.isRunningOnEmulator()) {
            final String[] notInstalled = {
                    "com.android.camera",  // Camera.apk
                    "com.android.gallery", // Gallery.apk
                    "com.cooliris.media"   // Gallery3D.apk
            };

            for (String pkg : notInstalled) {
                assertPackageNotInstalled(pkg);
            }
        }
    }

    @Suppress
    public void ignore_testSignup() throws Exception {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_add_info_msg);

        // username (max 25 characters)
        solo.enterText(0, generateUsername());
        solo.clickOnButtonResId(R.string.btn_save);

        solo.assertText(R.string.side_menu_stream);
    }

    @Suppress
    public void ignore_testSignupSkip() throws Exception {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnButtonResId(R.string.btn_skip);

        // Find Friends
        solo.assertText(R.string.side_menu_who_to_follow);

        solo.clickOnButtonResId(R.string.done);

        solo.assertText(R.string.side_menu_stream);
    }

    @Suppress
    public void ignore_testSignupWithPhotoFromCamera() throws Exception {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnText(R.string.add_image);
        solo.assertText(R.string.image_where); // How would you like to add an image?

        if (applicationProperties.isRunningOnEmulator()) {
            solo.clickOnText(R.string.take_new_picture);
            // FakeCamera will provide an image
            solo.sleep(1000);

            solo.waitForActivity(CropImageActivity.class);
            solo.clickOnText("Save");

            // make sure add image prompt is gone
            solo.assertTextFound(solo.getString(R.string.add_image), true);

            // clear image
            solo.findElement(With.id(R.id.artwork)).longClick();
            solo.assertText(R.string.add_image);
        }
    }

    @Suppress
    public void ignore_testSignupWithExistingPhoto() throws Exception {
        performSignup(generateEmail(), "password");

        solo.clickOnText(R.string.add_image);
        solo.assertText(R.string.image_where); // How would you like to add an image?

        if (applicationProperties.isRunningOnEmulator()) {
            solo.clickOnText(R.string.use_existing_image);

            // FakeGallery will provide an image
            solo.sleep(1000);

            solo.waitForActivity(CropImageActivity.class);
            solo.clickOnText("Save");

            // make sure add image prompt is gone
            solo.assertTextFound(solo.getString(R.string.add_image), true);

            // clear image
            solo.findElement(With.id(R.id.artwork)).longClick();
            solo.assertText(R.string.add_image);
        }
    }

    public void ignore_testSignupWithNonMatchingPasswords() throws Exception {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_login_error_password_message);
    }

    public void ignore_testSignupWithoutInput() throws Exception {
        solo.clickOnButtonResId(R.string.authentication_sign_up);
        solo.assertText(R.string.authentication_sign_up);
        solo.clickOnButtonResId(R.string.done);
        solo.assertText(R.string.authentication_error_incomplete_fields);
    }

    public void ignore_testSignupWithInvalidEmail() throws Exception {
        performSignup("not-an-email", "password");
        solo.assertText(R.string.authentication_error_invalid_email);
    }

    @Suppress
    public void ignore_testSignupEmailAlreadyTaken() throws Exception {
        String email = generateEmail();
        performSignup(email, "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnButtonResId(R.string.btn_skip);

        // Find Friends
        solo.assertText(R.string.side_menu_who_to_follow);

        solo.clickOnButtonResId(R.string.done);

        solo.assertText(R.string.side_menu_stream);

        menuScreen.logout();

        performSignup(email, "password");

        solo.assertText(R.string.authentication_signup_error_message);
        solo.clickOnText(android.R.string.ok);
    }

    public void ignore_testSignupWithTooShortPassword() throws Exception {
        performSignup(generateEmail(), "123");
        solo.assertText(R.string.authentication_error_password_too_short);
    }

    @Suppress
    public void ignore_testShouldShowEmailConfirmationDialogAfterSignupNoThanks() throws Exception {
        // perform a full signup
        ignore_testSignup();

        // exit app
        solo.goBack();
        solo.finishOpenedActivities();

        // relaunch activity
        setActivity(null);
        getActivity();

        solo.assertText(R.string.email_confirmation_you_need_to_confirm);
        solo.clickOnText(R.string.email_confirmation_confirm_later);
        solo.assertText(R.string.side_menu_stream);

        // relaunch activity, make sure email screen doesn't show up again
        solo.goBack();
        solo.finishOpenedActivities();

        setActivity(null);
        getActivity();
        solo.assertText(R.string.side_menu_stream);
    }


    @Suppress
    public void ignore_testShouldShowEmailConfirmationDialogAfterSignupResendEmail() throws Exception {
        // perform a full signup
        ignore_testSignup();

        // exit app
        solo.goBack();
        solo.finishOpenedActivities();

        // relaunch activity
        setActivity(null);
        getActivity();

        solo.sleep(500); // should not be needed

        solo.assertText(R.string.email_confirmation_you_need_to_confirm);
        solo.clickOnText(R.string.email_confirmation_resend);
        solo.assertText(R.string.side_menu_stream);

        // relaunch activity, make sure email screen doesn't show up again
        solo.goBack();
        solo.finishOpenedActivities();

        setActivity(null);
        getActivity();
        solo.assertText(R.string.side_menu_stream);
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

    private void performSignup(String email, String password) {
        solo.clickOnButtonResId(R.string.authentication_sign_up);
        solo.assertText(R.string.authentication_sign_up);
        ViewElement emailField = solo.findElement(With.id(R.id.txt_email_address));
        emailField.typeText(email);
        solo.assertText(email);

        solo.findElement(With.id(R.id.txt_choose_a_password)).typeText(password);
        solo.clickOnButtonResId(R.string.done);
    }
}
