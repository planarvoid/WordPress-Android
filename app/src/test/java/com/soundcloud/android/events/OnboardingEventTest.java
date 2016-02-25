package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

@RunWith(MockitoJUnitRunner.class)
public class OnboardingEventTest {

    private OnboardingEvent onboardingEvent;

    @Test
    public void shouldCreateEventFromSignupPrompt() {
        onboardingEvent = OnboardingEvent.signUpPrompt();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.AUTH_PROMPT);
        assertThat(onboardingEvent.getAttributes().get("type")).isEqualTo("sign up");
    }

    @Test
    public void shouldCreateEventFromLoginPrompt() {
        onboardingEvent = OnboardingEvent.logInPrompt();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.AUTH_PROMPT);
        assertThat(onboardingEvent.getAttributes().get("type")).isEqualTo("sign in");
    }

    @Test
    public void shouldCreateEventFromNativeAuthEvent() {
        onboardingEvent = OnboardingEvent.nativeAuthEvent();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.AUTH_CREDENTIALS);
        assertThat(onboardingEvent.getAttributes().get("type")).isEqualTo("native");
    }

    @Test
    public void shouldCreateEventFromGoogleAuthEvent() {
        onboardingEvent = OnboardingEvent.googleAuthEvent();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.AUTH_CREDENTIALS);
        assertThat(onboardingEvent.getAttributes().get("type")).isEqualTo("google_plus");
    }

    @Test
    public void shouldCreateEventFromFacebookAuthEvent() {
        onboardingEvent = OnboardingEvent.facebookAuthEvent();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.AUTH_CREDENTIALS);
        assertThat(onboardingEvent.getAttributes().get("type")).isEqualTo("facebook");
    }

    @Test
    public void shouldCreateEventFromAcceptTerms() {
        onboardingEvent = OnboardingEvent.termsAccepted();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.CONFIRM_TERMS);
        assertThat(onboardingEvent.getAttributes().get("action")).isEqualTo("accept");
    }

    @Test
    public void shouldCreateEventFromRejectTerms() {
        onboardingEvent = OnboardingEvent.termsRejected();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.CONFIRM_TERMS);
        assertThat(onboardingEvent.getAttributes().get("action")).isEqualTo("cancel");
    }

    @Test
    public void shouldCreateEventFromAuthComplete() {
        onboardingEvent = OnboardingEvent.authComplete();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.AUTH_COMPLETE);
    }

    @Test
    public void shouldCreateEventFromSaveUserInfo() {
        onboardingEvent = OnboardingEvent.savedUserInfo("", null);
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.USER_INFO);
        assertThat(onboardingEvent.getAttributes().get("added_username")).isEqualTo("no");
        assertThat(onboardingEvent.getAttributes().get("added_picture")).isEqualTo("no");
    }

    @Test
    public void shouldCreateEventFromSaveUserInfoWithUsernameAndPicture() {
        onboardingEvent = OnboardingEvent.savedUserInfo("Skrillex", new File("/sdcard/avatar.png"));
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.USER_INFO);
        assertThat(onboardingEvent.getAttributes().get("added_username")).isEqualTo("yes");
        assertThat(onboardingEvent.getAttributes().get("added_picture")).isEqualTo("yes");
    }

    @Test
    public void shouldCreateEventFromSkipUserInfo() {
        onboardingEvent = OnboardingEvent.skippedUserInfo();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.USER_INFO);
        assertThat(onboardingEvent.getAttributes().get("added_username")).isEqualTo("no");
        assertThat(onboardingEvent.getAttributes().get("added_picture")).isEqualTo("no");
    }

    @Test
    public void shouldCreateEventFromOnboardingComplete() {
        onboardingEvent = OnboardingEvent.onboardingComplete();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.ONBOARDING_COMPLETE);
    }

    @Test
    public void shouldCreateEventFromAcceptEmailOptIn() {
        onboardingEvent = OnboardingEvent.acceptEmailOptIn();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.EMAIL_MARKETING);
        assertThat(onboardingEvent.getAttributes().get("opt_in")).isEqualTo("yes");
    }

    @Test
    public void shouldCreateEventFromRejectEmailOptIn() {
        onboardingEvent = OnboardingEvent.rejectEmailOptIn();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.EMAIL_MARKETING);
        assertThat(onboardingEvent.getAttributes().get("opt_in")).isEqualTo("no");
    }

    @Test
    public void shouldCreateEventFromDismissEmailOptIn() {
        onboardingEvent = OnboardingEvent.dismissEmailOptIn();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.EMAIL_MARKETING);
        assertThat(onboardingEvent.getAttributes().get("opt_in")).isEqualTo("dismiss");
    }

    @Test
    public void shouldCreateSignupServeCaptchaEvent() {
        onboardingEvent = OnboardingEvent.signupServeCaptcha();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.SIGNUP_ERROR);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("serve_captcha");
    }

    @Test
    public void shouldCreateSignupDeniedEvent() {
        onboardingEvent = OnboardingEvent.signupDenied();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.SIGNUP_ERROR);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("denied_signup");
    }

    @Test
    public void shouldCreateSignupExistingEmailEvent() {
        onboardingEvent = OnboardingEvent.signupExistingEmail();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.SIGNUP_ERROR);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("existing_email");
    }

    @Test
    public void shouldCreateSignupInvalidEmailEvent() {
        onboardingEvent = OnboardingEvent.signupInvalidEmail();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.SIGNUP_ERROR);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("invalid_email");
    }

    @Test
    public void shouldCreateSignupGeneralErrorEvent() {
        onboardingEvent = OnboardingEvent.signupGeneralError();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.SIGNUP_ERROR);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("general_error");
    }

    @Test
    public void shouldCreateDeviceConflictEvent() {
        onboardingEvent = OnboardingEvent.deviceConflictOnLogin();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.DEVICE_MANAGEMENT);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("device_conflict");
    }

    @Test
    public void shouldCreateDeviceBlockEvent() {
        onboardingEvent = OnboardingEvent.deviceBlockOnLogin();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.DEVICE_MANAGEMENT);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("device_block");
    }

    @Test
    public void shouldCreateDeviceConflictLoggedOutEvent() {
        onboardingEvent = OnboardingEvent.deviceConflictLoggedOut();
        assertThat(onboardingEvent.getKind()).isEqualTo(OnboardingEvent.DEVICE_MANAGEMENT);
        assertThat(onboardingEvent.getAttributes().get("error_type")).isEqualTo("logged_out");
    }

}
