package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.viewelements.EditTextElement;
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
    public void ignore_testSignup() {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_add_info_msg);

        // username (max 25 characters)
        new EditTextElement(solo.findElements(With.className(EditText.class)).get(0)).typeText(generateUsername());
        solo.clickOnButtonWithText(R.string.btn_save);

        solo.assertText(R.string.side_menu_stream);
    }

    @Suppress
    public void ignore_testSignupSkip() {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnButtonWithText(R.string.btn_skip);

        // Find Friends
        solo.assertText(R.string.side_menu_who_to_follow);

        solo.clickOnButtonWithText(R.string.done);

        solo.assertText(R.string.side_menu_stream);
    }

    @Suppress
    public void ignore_testSignupWithPhotoFromCamera() {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnText(R.string.add_image);
        solo.assertText(R.string.image_where); // How would you like to add an image?

        if (applicationProperties.isRunningOnEmulator()) {
            solo.clickOnText(R.string.take_new_picture);
            // FakeCamera will provide an image
            solo.sleep(1000);

            solo.clickOnText("Save");

            // make sure add image prompt is gone

            // clear image
            solo.findElement(With.id(R.id.artwork)).longClick();
            solo.assertText(R.string.add_image);
        }
    }

    @Suppress
    public void ignore_testSignupWithExistingPhoto() {
        performSignup(generateEmail(), "password");

        solo.clickOnText(R.string.add_image);
        solo.assertText(R.string.image_where); // How would you like to add an image?

        if (applicationProperties.isRunningOnEmulator()) {
            solo.clickOnText(R.string.use_existing_image);

            // FakeGallery will provide an image
            solo.sleep(1000);

            solo.clickOnText("Save");

            // make sure add image prompt is gone

            // clear image
            solo.findElement(With.id(R.id.artwork)).longClick();
            solo.assertText(R.string.add_image);
        }
    }

    public void ignore_testSignupWithNonMatchingPasswords() {
        performSignup(generateEmail(), "password");
        solo.assertText(R.string.authentication_login_error_password_message);
    }

    public void ignore_testSignupWithoutInput() throws Exception {
        solo.clickOnButtonWithText(R.string.authentication_sign_up);
        solo.assertText(R.string.authentication_sign_up);
        solo.clickOnButtonWithText(R.string.done);
        solo.assertText(R.string.authentication_error_incomplete_fields);
    }

    public void ignore_testSignupWithInvalidEmail() {
        performSignup("not-an-email", "password");
        solo.assertText(R.string.authentication_error_invalid_email);
    }

    @Suppress
    public void ignore_testSignupEmailAlreadyTaken() {
        String email = generateEmail();
        performSignup(email, "password");
        solo.assertText(R.string.authentication_add_info_msg);

        solo.clickOnButtonWithText(R.string.btn_skip);

        // Find Friends
        solo.assertText(R.string.side_menu_who_to_follow);

        solo.clickOnButtonWithText(R.string.done);

        solo.assertText(R.string.side_menu_stream);

        menuScreen.logout();

        performSignup(email, "password");

        solo.assertText(R.string.authentication_signup_error_message);
        solo.clickOnText(android.R.string.ok);
    }

    public void ignore_testSignupWithTooShortPassword() {
        performSignup(generateEmail(), "123");
        solo.assertText(R.string.authentication_error_password_too_short);
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
        solo.clickOnButtonWithText(R.string.authentication_sign_up);
        solo.assertText(R.string.authentication_sign_up);
        EditTextElement emailField = new EditTextElement(solo.findElement(With.id(R.id.txt_email_address)));
        emailField.typeText(email);
        solo.assertText(email);

        new EditTextElement(solo.findElement(With.id(R.id.txt_choose_a_password))).typeText(password);
        solo.clickOnButtonWithText(R.string.done);
    }
}
