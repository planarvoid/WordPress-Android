package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(SoundCloudTestRunner.class)
public class OnboardingEventTest {

    private OnboardingEvent onboardingEvent;

    @Test
         public void shouldCreateEventFromSignupPrompt() {
        onboardingEvent = OnboardingEvent.signUpPrompt();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.AUTH_PROMPT);
        expect(onboardingEvent.getAttributes().get("type")).toEqual("sign up");
    }

    @Test
    public void shouldCreateEventFromLoginPrompt() {
        onboardingEvent = OnboardingEvent.logInPrompt();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.AUTH_PROMPT);
        expect(onboardingEvent.getAttributes().get("type")).toEqual("sign in");
    }

    @Test
    public void shouldCreateEventFromNativeAuthEvent() {
        onboardingEvent = OnboardingEvent.nativeAuthEvent();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.AUTH_CREDENTIALS);
        expect(onboardingEvent.getAttributes().get("type")).toEqual("native");
    }

    @Test
    public void shouldCreateEventFromGoogleAuthEvent() {
        onboardingEvent = OnboardingEvent.googleAuthEvent();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.AUTH_CREDENTIALS);
        expect(onboardingEvent.getAttributes().get("type")).toEqual("google_plus");
    }

    @Test
    public void shouldCreateEventFromFacebookAuthEvent() {
        onboardingEvent = OnboardingEvent.facebookAuthEvent();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.AUTH_CREDENTIALS);
        expect(onboardingEvent.getAttributes().get("type")).toEqual("facebook");
    }

    @Test
    public void shouldCreateEventFromAcceptTerms() {
        onboardingEvent = OnboardingEvent.termsAccepted();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.CONFIRM_TERMS);
        expect(onboardingEvent.getAttributes().get("action")).toEqual("accept");
    }

    @Test
    public void shouldCreateEventFromRejectTerms() {
        onboardingEvent = OnboardingEvent.termsRejected();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.CONFIRM_TERMS);
        expect(onboardingEvent.getAttributes().get("action")).toEqual("cancel");
    }

    @Test
    public void shouldCreateEventFromAuthComplete() {
        onboardingEvent = OnboardingEvent.authComplete();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.AUTH_COMPLETE);
    }

    @Test
    public void shouldCreateEventFromSaveUserInfo() {
        onboardingEvent = OnboardingEvent.savedUserInfo("", null);
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.USER_INFO);
        expect(onboardingEvent.getAttributes().get("added_username")).toEqual("no");
        expect(onboardingEvent.getAttributes().get("added_picture")).toEqual("no");
    }

    @Test
    public void shouldCreateEventFromSaveUserInfoWithUsernameAndPicture() {
        onboardingEvent = OnboardingEvent.savedUserInfo("Skrillex", new File("/sdcard/avatar.png"));
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.USER_INFO);
        expect(onboardingEvent.getAttributes().get("added_username")).toEqual("yes");
        expect(onboardingEvent.getAttributes().get("added_picture")).toEqual("yes");
    }

    @Test
    public void shouldCreateEventFromSkipUserInfo() {
        onboardingEvent = OnboardingEvent.skippedUserInfo();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.USER_INFO);
        expect(onboardingEvent.getAttributes().get("added_username")).toEqual("no");
        expect(onboardingEvent.getAttributes().get("added_picture")).toEqual("no");
    }

    @Test
    public void shouldCreateEventFromOnboardingComplete() {
        onboardingEvent = OnboardingEvent.onboardingComplete();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.ONBOARDING_COMPLETE);
    }

    @Test
    public void shouldCreateEventFromAcceptEmailOptIn() {
        onboardingEvent = OnboardingEvent.acceptEmailOptIn();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.EMAIL_MARKETING);
        expect(onboardingEvent.getAttributes().get("opt_in")).toEqual("yes");
    }

    @Test
    public void shouldCreateEventFromRejectEmailOptIn() {
        onboardingEvent = OnboardingEvent.rejectEmailOptIn();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.EMAIL_MARKETING);
        expect(onboardingEvent.getAttributes().get("opt_in")).toEqual("no");
    }

    @Test
    public void shouldCreateEventFromDismissEmailOptIn() {
        onboardingEvent = OnboardingEvent.dismissEmailOptIn();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.EMAIL_MARKETING);
        expect(onboardingEvent.getAttributes().get("opt_in")).toEqual("dismiss");
    }

    @Test
    public void shouldCreateSignupServeCaptchaEvent() {
        onboardingEvent = OnboardingEvent.signupServeCaptcha();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.SIGNUP_ERROR);
        expect(onboardingEvent.getAttributes().get("error_type")).toEqual("serve_captcha");
    }

    @Test
    public void shouldCreateSignupDeniedEvent() {
        onboardingEvent = OnboardingEvent.signupDenied();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.SIGNUP_ERROR);
        expect(onboardingEvent.getAttributes().get("error_type")).toEqual("denied_signup");
    }

    @Test
    public void shouldCreateSignupExistingEmailEvent() {
        onboardingEvent = OnboardingEvent.signupExistingEmail();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.SIGNUP_ERROR);
        expect(onboardingEvent.getAttributes().get("error_type")).toEqual("existing_email");
    }

    @Test
    public void shouldCreateSignupInvalidEmailEvent() {
        onboardingEvent = OnboardingEvent.signupInvalidEmail();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.SIGNUP_ERROR);
        expect(onboardingEvent.getAttributes().get("error_type")).toEqual("invalid_email");
    }

    @Test
    public void shouldCreateSignupGeneralErrorEvent() {
        onboardingEvent = OnboardingEvent.signupGeneralError();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.SIGNUP_ERROR);
        expect(onboardingEvent.getAttributes().get("error_type")).toEqual("general_error");
    }

    @Test
    public void shouldCreateDeviceConflictEvent() {
        onboardingEvent = OnboardingEvent.deviceConflictOnLogin();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.DEVICE_CONFLICT);
        expect(onboardingEvent.getAttributes().get("error_type")).toEqual("device_limit");
    }

    @Test
    public void shouldCreateDeviceConflictLoggedOutEvent() {
        onboardingEvent = OnboardingEvent.deviceConflictLoggedOut();
        expect(onboardingEvent.getKind()).toBe(OnboardingEvent.DEVICE_CONFLICT);
        expect(onboardingEvent.getAttributes().get("error_type")).toEqual("logged_out");
    }

}
