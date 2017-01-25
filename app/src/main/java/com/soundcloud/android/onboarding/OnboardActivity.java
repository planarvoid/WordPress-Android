package com.soundcloud.android.onboarding;

import static android.util.Log.INFO;
import static com.soundcloud.android.Consts.RequestCodes;
import static com.soundcloud.android.onboarding.FacebookSessionCallback.DEFAULT_FACEBOOK_READ_PERMISSIONS;
import static com.soundcloud.android.onboarding.FacebookSessionCallback.EMAIL_ONLY_PERMISSION;
import static com.soundcloud.android.util.AnimUtils.hideView;
import static com.soundcloud.android.util.AnimUtils.showView;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.auth.AcceptTermsLayout;
import com.soundcloud.android.onboarding.auth.AddUserInfoTaskFragment;
import com.soundcloud.android.onboarding.auth.AuthTaskFragment;
import com.soundcloud.android.onboarding.auth.GenderPickerDialogFragment;
import com.soundcloud.android.onboarding.auth.GooglePlusSignInTaskFragment;
import com.soundcloud.android.onboarding.auth.LoginLayout;
import com.soundcloud.android.onboarding.auth.LoginTaskFragment;
import com.soundcloud.android.onboarding.auth.RecoverActivity;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.onboarding.auth.SignupBasicsLayout;
import com.soundcloud.android.onboarding.auth.SignupDetailsLayout;
import com.soundcloud.android.onboarding.auth.SignupLog;
import com.soundcloud.android.onboarding.auth.SignupMethodLayout;
import com.soundcloud.android.onboarding.auth.SignupTaskFragment;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.widget.ImageView;

import javax.inject.Inject;
import java.io.File;
import java.util.Random;

public class OnboardActivity extends FragmentActivity
        implements AuthTaskFragment.OnAuthResultListener, LoginLayout.LoginHandler,
        SignupMethodLayout.SignUpMethodHandler, SignupDetailsLayout.UserDetailsHandler,
        AcceptTermsLayout.AcceptTermsHandler, SignupBasicsLayout.SignUpBasicsHandler,
        GenderPickerDialogFragment.CallbackProvider, FacebookSessionCallback.FacebookLoginCallbacks {

    public static final String ONBOARDING_TAG = "ScOnboarding";

    private static final int[] BACKGROUND_IMAGES = new int[]{R.drawable.onboard_background_ago, R.drawable.onboard_background_simz};

    protected enum OnboardingState {
        PHOTOS, LOGIN, SIGN_UP_METHOD, SIGN_UP_BASICS, SIGN_UP_DETAILS, ACCEPT_TERMS
    }

    public static final String EXTRA_DEEPLINK_URN = "EXTRA_URN";

    private static final String BACKGROUND_IMAGE_IDX = "BACKGROUND_IMAGE_IDX";
    private static final String SIGNUP_DIALOG_TAG = "signup_dialog";
    private static final String BUNDLE_STATE = "BUNDLE_STATE";
    private static final String BUNDLE_USER = "BUNDLE_USER";
    private static final String BUNDLE_LOGIN = "BUNDLE_LOGIN";
    private static final String BUNDLE_SIGN_UP_BASICS = "BUNDLE_SIGN_UP_BASICS";
    private static final String BUNDLE_SIGN_UP_DETAILS = "BUNDLE_SIGN_UP_DETAILS";
    private static final String BUNDLE_ACCEPT_TERMS = "BUNDLE_ACCEPT_TERMS";
    private static final String LAST_GOOGLE_ACCT_USED = "BUNDLE_LAST_GOOGLE_ACCOUNT_USED";
    private static final String LOGIN_DIALOG_TAG = "login_dialog";

    private int backgroundImageIdx;
    private OnboardingState state = OnboardingState.PHOTOS;
    private String lastGoogleAccountSelected;
    private ActivityResult activityResult = ActivityResult.empty();
    @Nullable private Urn userUrn = Urn.NOT_SET;

    private View photoBottomBar, photoLogo;

    private View overlayBg, overlayHolder;
    @Nullable private LoginLayout loginLayout;
    @Nullable private SignupMethodLayout signUpMethodLayout;
    @Nullable private SignupBasicsLayout signUpBasicsLayout;
    @Nullable private SignupDetailsLayout signUpDetailsLayout;
    @Nullable private AcceptTermsLayout acceptTermsLayout;

    /**
     * Extracted account authenticator functions. Extracted because of Fragment usage, we have to extend FragmentActivity.
     * See {@link android.accounts.AccountAuthenticatorActivity} for documentation
     */
    private AccountAuthenticatorResponse accountAuthenticatorResponse;
    @VisibleForTesting protected Bundle resultBundle;

    private final Animation.AnimationListener hideScrollViewListener = new AnimUtils.SimpleAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            overlayHolder.setVisibility(View.GONE);

            if (loginLayout != null) {
                hideView(loginLayout, false);
            }
            if (signUpMethodLayout != null) {
                hideView(signUpMethodLayout, false);
            }
            if (signUpBasicsLayout != null) {
                hideView(signUpBasicsLayout, false);
            }
            if (signUpDetailsLayout != null) {
                hideView(signUpDetailsLayout, false);
            }
            if (acceptTermsLayout != null) {
                hideView(acceptTermsLayout, false);
            }
        }
    };
    @Nullable private Bundle loginBundle, signUpBasicsBundle, signUpDetailsBundle, acceptTermsBundle;
    private Urn resourceUrn = Urn.NOT_SET;

    private final View.OnClickListener onLoginButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setState(OnboardingState.LOGIN);
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.AUTH_LOG_IN));
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.logInPrompt());
        }
    };
    private final View.OnClickListener onSignupButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.AUTH_SIGN_UP));
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.signUpPrompt());

            if (!applicationProperties.isDevBuildRunningOnDevice() && SignupLog.shouldThrottleSignup(v.getContext())) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_site))));
                finish();
            } else {
                setState(OnboardingState.SIGN_UP_METHOD);
            }
        }
    };

    @Inject FacebookSdk facebookSdk;
    @Inject CallbackManager facebookCallbackManager;
    @Inject LoginManager facebookLoginManager;
    @Inject ConfigurationManager configurationManager;
    @Inject ApplicationProperties applicationProperties;
    @Inject BugReporter bugReporter;
    @Inject EventBus eventBus;
    @Inject FeatureFlags featureFlags;
    @Inject TokenInformationGenerator tokenUtils;
    @Inject Navigator navigator;
    @Inject OAuth oauth;

    public OnboardActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    OnboardActivity(ConfigurationManager configurationManager,
                    BugReporter bugReporter,
                    EventBus eventBus,
                    TokenInformationGenerator tokenUtils,
                    Navigator navigator,
                    FacebookSdk facebookSdk,
                    LoginManager facebookLoginManager,
                    CallbackManager facebookCallbackManager) {
        this.configurationManager = configurationManager;
        this.bugReporter = bugReporter;
        this.eventBus = eventBus;
        this.tokenUtils = tokenUtils;
        this.navigator = navigator;
        this.facebookSdk = facebookSdk;
        this.facebookLoginManager = facebookLoginManager;
        this.facebookCallbackManager = facebookCallbackManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landing);

        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(this));

        unpackAccountAuthenticatorResponse(getIntent());
        unpackDeeplink(getIntent());
        setupViews(savedInstanceState);
        setButtonListeners();
        checkForDeviceConflict();
        setupFacebookCallback();
    }

    private void setupFacebookCallback() {
        facebookLoginManager.registerCallback(facebookCallbackManager, new FacebookSessionCallback(this));
    }

    private void unpackAccountAuthenticatorResponse(Intent intent) {
        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onRequestContinued();
        }
    }

    private void unpackDeeplink(Intent intent) {
        if (intent.hasExtra(EXTRA_DEEPLINK_URN)) {
            resourceUrn = intent.getParcelableExtra(EXTRA_DEEPLINK_URN);
        }
    }

    private void checkForDeviceConflict() {
        if (configurationManager.shouldDisplayDeviceConflict()) {
            showDeviceConflictLogoutDialog();
            configurationManager.clearDeviceConflict();
        }
    }

    private void setupViews(@Nullable Bundle savedInstanceState) {
        final boolean isConfigChange = savedInstanceState != null;

        overridePendingTransition(0, 0);
        showBackground(savedInstanceState);

        photoBottomBar = findViewById(R.id.tour_bottom_bar);
        photoLogo = findViewById(R.id.tour_logo);
        overlayBg = findViewById(R.id.overlay_bg);
        overlayHolder = findViewById(R.id.overlay_holder);

        setState(OnboardingState.PHOTOS);

        if (!isConfigChange) {
            trackTourScreen();
        }

        if (isConfigChange) {
            overlayBg.setVisibility(View.GONE);
            overlayHolder.setVisibility(View.GONE);
        }
    }

    private void showBackground(@Nullable Bundle savedInstanceState) {
        backgroundImageIdx = savedInstanceState == null
                             ? new Random().nextInt(BACKGROUND_IMAGES.length)
                             : savedInstanceState.getInt(BACKGROUND_IMAGE_IDX);
        ImageView bgImageView = (ImageView) findViewById(R.id.landing_background_image);
        final Drawable drawable = ResourcesCompat.getDrawable(getResources(),
                                                              BACKGROUND_IMAGES[backgroundImageIdx],
                                                              null);
        bgImageView.setImageDrawable(drawable);
        showView(bgImageView, true);
    }

    private void setButtonListeners() {
        findViewById(R.id.btn_login).setOnClickListener(onLoginButtonClick);
        findViewById(R.id.btn_create_account).setOnClickListener(onSignupButtonClick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(this));
    }

    @Override
    public void onLogin(String email, String password) {
        LoginTaskFragment.create(email, password).show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.nativeAuthEvent());
    }

    @Override
    public void onCancelLogin() {
        setState(OnboardingState.PHOTOS);
        trackTourScreen();
    }

    @Override
    public void onEmailAuth() {
        setState(OnboardingState.SIGN_UP_BASICS);
    }

    @Override
    public void onSignUp(String email, String password, BirthdayInfo birthday, String gender) {
        proposeTermsOfUse(SignupVia.API, SignupTaskFragment.getParams(email, password, birthday, gender));
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.nativeAuthEvent());
    }

    @Override
    public void onCancelSignUp() {
        setState(OnboardingState.PHOTOS);
        trackTourScreen();
    }

    @Override
    public void onSubmitUserDetails(String username, @Nullable File avatarFile) {
        if (userUrn == Urn.NOT_SET) {
            return;
        }

        AddUserInfoTaskFragment.create(username, avatarFile).show(getSupportFragmentManager(), "add_user_task");
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.savedUserInfo(username, avatarFile));
    }

    @Override
    public FragmentActivity getFragmentActivity() {
        return this;
    }

    @Override
    public void onBackPressed() {
        switch (state) {
            case LOGIN:
            case SIGN_UP_METHOD:
            case ACCEPT_TERMS:
                setState(OnboardingState.PHOTOS);
                trackTourScreen();
                break;

            case SIGN_UP_BASICS:
                setState(OnboardingState.SIGN_UP_METHOD);
                break;

            case SIGN_UP_DETAILS:
                getSignUpDetailsLayout().onSave();
                break;

            case PHOTOS:
                super.onBackPressed();
                break;
        }
    }

    private void setState(OnboardingState state) {
        setState(state, true);
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private void setState(OnboardingState state, boolean animated) {
        this.state = state;
        log(INFO, ONBOARDING_TAG, "will set OnboardActivity state to: " + state);

        switch (this.state) {
            case PHOTOS:
                onHideOverlay(animated);
                break;
            case LOGIN:
                hideViews(state, animated);
                showOverlay(getLoginLayout(), animated);
                break;

            case SIGN_UP_METHOD:
                hideViews(state, animated);
                showOverlay(getSignUpMethodLayout(), animated);
                break;

            case SIGN_UP_BASICS:
                hideViews(state, animated);
                showOverlay(getSignUpBasicsLayout(), animated);
                break;

            case SIGN_UP_DETAILS:
                hideViews(state, animated);
                showOverlay(getSignUpDetailsLayout(), animated);
                break;

            case ACCEPT_TERMS:
                hideViews(state, false);
                showOverlay(getAcceptTermsLayout(), animated);
                break;
        }
    }

    private void hideViews(OnboardingState state, boolean animated) {
        if (state != OnboardingState.LOGIN && loginLayout != null) {
            hideView(loginLayout, animated);
        }

        if (state != OnboardingState.SIGN_UP_METHOD && signUpMethodLayout != null) {
            hideView(signUpMethodLayout, animated);
        }

        if (state != OnboardingState.SIGN_UP_BASICS && signUpBasicsLayout != null) {
            hideView(signUpBasicsLayout, animated);
        }

        if (state != OnboardingState.SIGN_UP_DETAILS && signUpDetailsLayout != null) {
            hideView(signUpDetailsLayout, animated);
        }

        if (state != OnboardingState.ACCEPT_TERMS && acceptTermsLayout != null) {
            hideView(acceptTermsLayout, animated);
        }
    }

    @Override
    public void onGooglePlusAuth() {
        log(INFO, ONBOARDING_TAG, "on Google+ auth");

        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            showGoogleAccountPicker();
        } else {
            final boolean isNotResolvable = !googleAPI.isUserResolvableError(result);
            if (isNotResolvable || !tryToShowResolveDialog(googleAPI, result)) {
                showGooglePlayServicesInstallError();
            }
        }
    }

    private boolean tryToShowResolveDialog(GoogleApiAvailability googleAPI, int result) {
        return googleAPI.showErrorDialogFragment(this, result, RequestCodes.PLAY_SERVICES_INSTALLED);
    }

    private void showGooglePlayServicesInstallError() {
        ErrorUtils.handleSilentException(new IllegalStateException("Unable to install Google Play Services during login"));
        new AlertDialog.Builder(this)
                .setTitle(R.string.authentication_error_title)
                .setMessage(R.string.authentication_error_unable_to_use_google_play_services)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showGoogleAccountPicker() {
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
                                                             false, null, null, null, null);
        startActivityForResult(intent, RequestCodes.PICK_GOOGLE_ACCOUNT);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.googleAuthEvent());
    }

    @Override
    public void onFacebookAuth() {
        log(INFO, ONBOARDING_TAG, "on Facebook auth");

        proposeTermsOfUse(SignupVia.FACEBOOK_SSO, null);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.facebookAuthEvent());
    }

    @Override
    public void onShowTermsOfUse() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_terms))));
    }

    @Override
    public void onShowPrivacyPolicy() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_privacy))));
    }

    @Override
    public void onRecoverPassword(String email) {
        Intent recoveryIntent = new Intent(this, RecoverActivity.class);
        if (email != null && email.length() > 0) {
            recoveryIntent.putExtra("email", email);
        }
        startActivityForResult(recoveryIntent, RequestCodes.RECOVER_PASSWORD_CODE);
    }

    @Override
    public void onAcceptTerms(SignupVia signupVia, Bundle signupParams) {
        setState(OnboardingState.PHOTOS);
        switch (signupVia) {
            case GOOGLE_PLUS:
                createNewUserFromGooglePlus(signupParams);
                break;
            case FACEBOOK_SSO:
                createNewUserFromFacebook();
                break;
            case API:
                createNewUserFromEmail(signupParams);
                break;
            default:
                throw new IllegalArgumentException("Unknown signupVia: " + signupVia.name());
        }

        hideView(getAcceptTermsLayout(), true);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.termsAccepted());
    }

    private void createNewUserFromEmail(Bundle signupParams) {
        SignupTaskFragment.create(signupParams).show(getSupportFragmentManager(), SIGNUP_DIALOG_TAG);
    }

    private void createNewUserFromGooglePlus(Bundle signupParams) {
        GooglePlusSignInTaskFragment.create(signupParams).show(getSupportFragmentManager(), SIGNUP_DIALOG_TAG);
    }

    private void createNewUserFromFacebook() {
        facebookLoginManager.logInWithReadPermissions(this, DEFAULT_FACEBOOK_READ_PERMISSIONS);
    }

    @Override
    public void onRejectTerms() {
        setState(OnboardingState.PHOTOS);
        trackTourScreen();
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.termsRejected());
    }

    @Override
    public void onAuthTaskComplete(ApiUser user, SignupVia via, boolean wasApiSignupTask) {
        log(INFO, ONBOARDING_TAG, "auth task complete, via: " + via + ", was api signup task: " + wasApiSignupTask);

        if (wasApiSignupTask) {
            SignupLog.writeNewSignupAsync(this);
            this.userUrn = user.getUrn();
            setState(OnboardingState.SIGN_UP_DETAILS);
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.AUTH_USER_DETAILS));
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());
        } else {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, user.getUsername());
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
            resultBundle = result;

            sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                                  .putExtra(PublicApiUser.EXTRA_ID, user.getId())
                                  .putExtra(SignupVia.EXTRA, via.name));

            if (Urn.NOT_SET.equals(resourceUrn)) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } else {
                navigator.openResolveForUrn(this, resourceUrn);
            }

            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LAST_GOOGLE_ACCT_USED, lastGoogleAccountSelected);
        outState.putSerializable(BUNDLE_STATE, state);
        outState.putParcelable(BUNDLE_USER, userUrn);
        outState.putInt(BACKGROUND_IMAGE_IDX, backgroundImageIdx);

        if (!Urn.NOT_SET.equals(resourceUrn)) {
            outState.putParcelable(EXTRA_DEEPLINK_URN, resourceUrn);
        }

        if (loginLayout != null) {
            outState.putBundle(BUNDLE_LOGIN, loginLayout.getStateBundle());
        }
        if (signUpBasicsLayout != null) {
            outState.putBundle(BUNDLE_SIGN_UP_BASICS, signUpBasicsLayout.getStateBundle());
        }
        if (signUpDetailsLayout != null) {
            outState.putBundle(BUNDLE_SIGN_UP_DETAILS, signUpDetailsLayout.getStateBundle());
        }
        if (acceptTermsLayout != null) {
            outState.putBundle(BUNDLE_ACCEPT_TERMS, acceptTermsLayout.getStateBundle());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        userUrn = savedInstanceState.getParcelable(BUNDLE_USER);
        lastGoogleAccountSelected = savedInstanceState.getString(LAST_GOOGLE_ACCT_USED);
        loginBundle = savedInstanceState.getBundle(BUNDLE_LOGIN);
        signUpBasicsBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP_BASICS);
        signUpDetailsBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP_DETAILS);
        acceptTermsBundle = savedInstanceState.getBundle(BUNDLE_ACCEPT_TERMS);

        unpackDeeplink(savedInstanceState);

        final OnboardingState state = (OnboardingState) savedInstanceState.getSerializable(BUNDLE_STATE);
        setState(state, false);
    }

    private void unpackDeeplink(@NotNull Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(EXTRA_DEEPLINK_URN)) {
            resourceUrn = savedInstanceState.getParcelable(EXTRA_DEEPLINK_URN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        activityResult = new ActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        doSafeActivityResultActions(activityResult);
        activityResult = ActivityResult.empty();
    }

    private void doSafeActivityResultActions(ActivityResult activityResult) {
        final int requestCode = activityResult.requestCode;
        final int resultCode = activityResult.resultCode;
        final Intent intent = activityResult.intent;

        if (FacebookSdk.isFacebookRequestCode(requestCode)) {
            facebookCallbackManager.onActivityResult(requestCode, resultCode, intent);
        }

        switch (requestCode) {
            case RequestCodes.GALLERY_IMAGE_PICK: {
                if (getSignUpDetailsLayout() != null) {
                    getSignUpDetailsLayout().onImagePick(resultCode, intent);
                }
                break;
            }

            case RequestCodes.GALLERY_IMAGE_TAKE: {
                if (getSignUpDetailsLayout() != null) {
                    getSignUpDetailsLayout().onImageTake(resultCode);
                }
                break;
            }

            case Crop.REQUEST_CROP: {
                if (getSignUpDetailsLayout() != null) {
                    getSignUpDetailsLayout().onImageCrop(resultCode, intent);
                }
                break;
            }

            case RequestCodes.RECOVER_PASSWORD_CODE: {
                if (resultCode != RESULT_OK || intent == null) {
                    break;
                }
                final boolean success = intent.getBooleanExtra("success", false);

                if (success) {
                    AndroidUtils.showToast(this, R.string.authentication_recover_password_success);
                } else {
                    final String error = intent.getStringExtra("error");
                    AndroidUtils.showToast(this,
                                           error == null ?
                                           getString(R.string.authentication_recover_password_failure) :
                                           getString(R.string.authentication_recover_password_failure_reason, error));
                }
                break;
            }

            case RequestCodes.PLAY_SERVICES_INSTALLED: {
                if (resultCode != RESULT_OK) {
                    showGooglePlayServicesInstallError();
                } else {
                    showGoogleAccountPicker();
                }
                break;
            }

            case RequestCodes.SIGNUP_VIA_GOOGLE: {
                onGoogleActivityResult(resultCode);
                break;
            }

            case RequestCodes.RECOVER_FROM_PLAY_SERVICES_ERROR: {
                onGoogleActivityResult(resultCode);
                break;
            }

            case RequestCodes.PICK_GOOGLE_ACCOUNT: {
                if (resultCode == RESULT_OK) {
                    onGoogleAccountSelected(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                }
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (IOUtils.isExternalStoragePermissionGranted(requestCode, grantResults) && signUpDetailsLayout != null) {
            signUpDetailsLayout.onStoragePermissionGranted(this);
        }
    }

    private void trackTourScreen() {
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.TOUR));
    }

    private LoginLayout getLoginLayout() {
        if (loginLayout == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.login_stub);

            loginLayout = (LoginLayout) stub.inflate();
            loginLayout.setLoginHandler(this);
            loginLayout.setVisibility(View.GONE);
            loginLayout.setStateFromBundle(loginBundle);
            loginLayout.setGooglePlusVisibility(applicationProperties.isGooglePlusEnabled());
        }

        return loginLayout;
    }

    private SignupMethodLayout getSignUpMethodLayout() {
        if (signUpMethodLayout == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.sign_up_stub);

            signUpMethodLayout = (SignupMethodLayout) stub.inflate();
            signUpMethodLayout.setSignUpMethodHandler(this);
            signUpMethodLayout.setVisibility(View.GONE);
            signUpMethodLayout.setGooglePlusVisibility(applicationProperties.isGooglePlusEnabled());
        }

        return signUpMethodLayout;
    }

    private SignupBasicsLayout getSignUpBasicsLayout() {
        if (signUpBasicsLayout == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.sign_up_basic_stub);

            signUpBasicsLayout = (SignupBasicsLayout) stub.inflate();
            signUpBasicsLayout.setSignUpHandler(this);
            signUpBasicsLayout.setVisibility(View.GONE);
            signUpBasicsLayout.setStateFromBundle(signUpBasicsBundle);
        }

        return signUpBasicsLayout;
    }

    private SignupDetailsLayout getSignUpDetailsLayout() {
        if (signUpDetailsLayout == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.user_details_stub);

            signUpDetailsLayout = (SignupDetailsLayout) stub.inflate();
            signUpDetailsLayout.setUserDetailsHandler(this);
            signUpDetailsLayout.setVisibility(View.GONE);
            signUpDetailsLayout.setState(signUpDetailsBundle);
        }

        return signUpDetailsLayout;
    }

    private AcceptTermsLayout getAcceptTermsLayout() {
        if (acceptTermsLayout == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.accept_terms_stub);

            acceptTermsLayout = (AcceptTermsLayout) stub.inflate();
            acceptTermsLayout.setAcceptTermsHandler(this);
            acceptTermsLayout.setState(acceptTermsBundle);
            acceptTermsLayout.setVisibility(View.GONE);
        }
        return acceptTermsLayout;
    }

    private void onHideOverlay(boolean animated) {
        showView(photoBottomBar, animated);
        showView(photoLogo, animated);
        hideView(overlayBg, animated);

        showView(findViewById(R.id.onboarding_text), false);

        if (animated && overlayHolder.getVisibility() == View.VISIBLE) {
            hideView(overlayHolder, hideScrollViewListener);
        } else {
            hideScrollViewListener.onAnimationEnd(null);
        }
    }

    private void showOverlay(View overlay, boolean animated) {
        hideView(photoBottomBar, animated);
        hideView(photoLogo, animated);

        // hide foreground views
        hideView(findViewById(R.id.onboarding_text), animated);

        showView(overlayHolder, animated);
        showView(overlayBg, animated);
        showView(overlay, animated);
    }

    @Override
    public GenderPickerDialogFragment.Callback getGenderPickerCallback() {
        return getSignUpBasicsLayout();
    }

    private void onGoogleAccountSelected(String name) {
        // store the last account name in case we have to retry after startActivityForResult with G+ app
        lastGoogleAccountSelected = name;
        proposeTermsOfUse(SignupVia.GOOGLE_PLUS,
                          GooglePlusSignInTaskFragment.getParams(name, RequestCodes.SIGNUP_VIA_GOOGLE));
    }

    /**
     * Set signup params on accept terms view and change state
     */
    private void proposeTermsOfUse(SignupVia signupVia, Bundle params) {
        getAcceptTermsLayout().setSignupParams(signupVia, params);
        setState(OnboardingState.ACCEPT_TERMS);
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.AUTH_TERMS));
    }

    private void onGoogleActivityResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            // just kick off another task with the last account selected
            final Bundle params = GooglePlusSignInTaskFragment.getParams(lastGoogleAccountSelected,
                                                                         RequestCodes.SIGNUP_VIA_GOOGLE);
            createNewUserFromGooglePlus(params);
        }
    }

    public void finish() {
        if (accountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (resultBundle != null) {
                accountAuthenticatorResponse.onResult(resultBundle);
            } else {
                accountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
            }
            accountAuthenticatorResponse = null;
        }
        super.finish();
    }

    @Override
    public void loginWithFacebook(String facebookToken) {
        if (featureFlags.isEnabled(Flag.AUTH_API_MOBILE)) {
            login(SignInOperations.getFacebookTokenBundle(facebookToken));
        } else {
            login(tokenUtils.getGrantBundle(OAuth.GRANT_TYPE_FACEBOOK, facebookToken));
        }
    }

    public void requestFacebookEmail() {
        log(INFO, ONBOARDING_TAG, "re-requesting facebook email permission");
        facebookLoginManager.logInWithReadPermissions(this, EMAIL_ONLY_PERMISSION);
    }

    @Override
    public void confirmRequestForFacebookEmail() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(R.string.authentication_signup_facebook_email_required)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> requestFacebookEmail());
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupFacebookEmailDenied());
    }

    @Override
    public void onFacebookAuthenticationFailedMessage() {
        final boolean allowUserFeedback = true;
        onError(getString(R.string.facebook_authentication_failed_message), allowUserFeedback);
    }

    @Override
    public void onError(String message, boolean allowUserFeedback) {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(TextUtils.isEmpty(message) ?
                            getString(R.string.authentication_signup_error_message) :
                            message)
                .setPositiveButton(android.R.string.ok, null);

        if (allowUserFeedback) {
            showDialogWithFeedbackAndTrackEvent(dialogBuilder, OnboardingEvent.signupGeneralError());
        } else {
            showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupExistingEmail());
        }
    }

    @Override
    public void onEmailTaken() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(R.string.authentication_email_taken_message)
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupExistingEmail());
    }

    @Override
    public void onSpam() {
        final SpamDialogOnClickListener spamDialogOnClickListener = new SpamDialogOnClickListener(this, oauth);
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(R.string.authentication_captcha_message)
                .setPositiveButton(getString(R.string.try_again), spamDialogOnClickListener)
                .setNeutralButton(getString(R.string.btn_cancel), spamDialogOnClickListener);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupServeCaptcha());
    }

    @Override
    public void onBlocked() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_blocked_title)
                .setMessage(R.string.authentication_blocked_message)
                .setPositiveButton(R.string.contact_support, (dialogInterface, i) -> {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                                          .setData(Uri.parse(getString(R.string.url_contact_support))));
                    dialogInterface.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null);
        showDialogWithHyperlinksAndTrackEvent(dialogBuilder, OnboardingEvent.signupDenied());
    }

    @Override
    public void onEmailInvalid() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(R.string.authentication_email_invalid_message)
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupInvalidEmail());
    }

    @Override
    public void onUsernameInvalid(String message) {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null);
        //TODO: tracking spec is not ready yet
        if (!isFinishing()) {
            dialogBuilder.create().show();
        }
    }

    @Override
    public void onDeviceConflict(final Bundle loginBundle) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(new CustomFontViewBuilder(this)
                                 .setIcon(R.drawable.dialog_device_management)
                                 .setTitle(R.string.device_management_limit_title)
                                 .setMessage(R.string.device_management_limit_active).get())
                .setPositiveButton(R.string.device_management_register, (dialog, which) -> login(loginBundle))
                .setNegativeButton(R.string.btn_cancel, null);
        showDialogAndTrackEvent(builder, OnboardingEvent.deviceConflictOnLogin());
    }

    @Override
    public void onDeviceBlock() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(new CustomFontViewBuilder(this)
                                 .setIcon(R.drawable.dialog_device_management)
                                 .setTitle(R.string.device_management_limit_title)
                                 .setMessage(R.string.device_management_limit_registered).get())
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(builder, OnboardingEvent.deviceBlockOnLogin());
    }

    private void showDeviceConflictLogoutDialog() {
        final View view = new CustomFontViewBuilder(this)
                .setIcon(R.drawable.dialog_device_management)
                .setTitle(R.string.device_management_limit_title)
                .setMessage(R.string.device_management_conflict_message).get();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(builder, OnboardingEvent.deviceConflictLoggedOut());
    }

    private void showDialogWithFeedbackAndTrackEvent(AlertDialog.Builder dialogBuilder, OnboardingEvent event) {
        if (!isFinishing()) {
            addFeedbackButton(dialogBuilder);
            showDialogAndTrackEvent(dialogBuilder, event);
        }
    }

    private void showDialogAndTrackEvent(AlertDialog.Builder dialogBuilder, OnboardingEvent event) {
        if (!isFinishing()) {
            dialogBuilder
                    .create()
                    .show();
            eventBus.publish(EventQueue.ONBOARDING, event);
        }
    }

    private void addFeedbackButton(AlertDialog.Builder dialogBuilder) {
        if (applicationProperties.shouldAllowFeedback()) {
            dialogBuilder
                    .setNeutralButton(R.string.title_feedback, (dialog, which) -> {
                        Log.i(ONBOARDING_TAG, "on send bug report");
                        bugReporter.showSignInFeedbackDialog(getFragmentActivity());
                    });
        }
    }

    private void showDialogWithHyperlinksAndTrackEvent(AlertDialog.Builder dialogBuilder, OnboardingEvent event) {
        if (!isFinishing()) {
            final Dialog alertDialog = dialogBuilder.create();
            alertDialog.show();

            eventBus.publish(EventQueue.ONBOARDING, event);
        }
    }

    private AlertDialog.Builder createDefaultAuthErrorDialogBuilder(int title) {
        return new AlertDialog.Builder(OnboardActivity.this)
                .setTitle(getString(title))
                .setIconAttribute(android.R.attr.alertDialogIcon);
    }

    private void login(Bundle data) {
        LoginTaskFragment.create(data).show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
    }

}
