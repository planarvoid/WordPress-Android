package com.soundcloud.android.onboarding;

import static android.util.Log.INFO;
import static com.soundcloud.android.Consts.RequestCodes;
import static com.soundcloud.android.onboarding.FacebookSessionCallback.DEFAULT_FACEBOOK_READ_PERMISSIONS;
import static com.soundcloud.android.utils.AnimUtils.hideView;
import static com.soundcloud.android.utils.AnimUtils.showView;
import static com.soundcloud.android.utils.ErrorUtils.log;
import static com.soundcloud.android.utils.Log.ONBOARDING_TAG;

import com.facebook.NonCachingTokenCachingStrategy;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.dialog.ImageAlertDialog;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.auth.AcceptTermsLayout;
import com.soundcloud.android.onboarding.auth.AddUserInfoTaskFragment;
import com.soundcloud.android.onboarding.auth.AuthTaskFragment;
import com.soundcloud.android.onboarding.auth.GenderPickerDialogFragment;
import com.soundcloud.android.onboarding.auth.GooglePlusSignInTaskFragment;
import com.soundcloud.android.onboarding.auth.LoginLayout;
import com.soundcloud.android.onboarding.auth.LoginTaskFragment;
import com.soundcloud.android.onboarding.auth.RecoverActivity;
import com.soundcloud.android.onboarding.auth.SignupBasicsLayout;
import com.soundcloud.android.onboarding.auth.SignupDetailsLayout;
import com.soundcloud.android.onboarding.auth.SignupLog;
import com.soundcloud.android.onboarding.auth.SignupMethodLayout;
import com.soundcloud.android.onboarding.auth.SignupTaskFragment;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoriesFragment;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;

import org.jetbrains.annotations.Nullable;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import javax.inject.Inject;
import java.io.File;

public class OnboardActivity extends FragmentActivity
        implements AuthTaskFragment.OnAuthResultListener, LoginLayout.LoginHandler,
        SignupMethodLayout.SignUpMethodHandler, SignupDetailsLayout.UserDetailsHandler,
        AcceptTermsLayout.AcceptTermsHandler, SignupBasicsLayout.SignUpBasicsHandler,
        GenderPickerDialogFragment.CallbackProvider {

    protected enum OnboardingState {
        PHOTOS, LOGIN, SIGN_UP_METHOD, SIGN_UP_BASICS, SIGN_UP_DETAILS, ACCEPT_TERMS
    }

    private static final String SIGNUP_DIALOG_TAG = "signup_dialog";
    private static final String BUNDLE_STATE = "BUNDLE_STATE";
    private static final String BUNDLE_USER = "BUNDLE_USER";
    private static final String BUNDLE_LOGIN = "BUNDLE_LOGIN";
    private static final String BUNDLE_SIGN_UP_BASICS = "BUNDLE_SIGN_UP_BASICS";
    private static final String BUNDLE_SIGN_UP_DETAILS = "BUNDLE_SIGN_UP_DETAILS";
    private static final String BUNDLE_ACCEPT_TERMS = "BUNDLE_ACCEPT_TERMS";
    private static final String LAST_GOOGLE_ACCT_USED = "BUNDLE_LAST_GOOGLE_ACCOUNT_USED";
    private static final String LOGIN_DIALOG_TAG = "login_dialog";
    private final ViewPager.OnPageChangeListener onTourPageChange = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int selected) {
            RadioGroup group = (RadioGroup) findViewById(R.id.rdo_tour_step);

            for (int i = 0; i < group.getChildCount(); i++) {
                RadioButton button = (RadioButton) group.getChildAt(i);
                button.setChecked(i == selected);
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };


    private OnboardingState lastAuthState;
    private OnboardingState state = OnboardingState.PHOTOS;
    private String lastGoogleAccountSelected;
    private ActivityResult activityResult = ActivityResult.empty();
    @Nullable private PublicApiUser user;

    private View photoBottomBar, photoLogo;
    private ViewPager photoPager;

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
    private Bundle resultBundle;

    private OAuth oauth;

    // a bullshit fix for https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/533f4054fabb27481b26624a
    // We need to redo onboarding, so this is just a quick fix to prevent the crashes during the sign in flow
    private boolean isBeingDestroyed = false;

    private final Animation.AnimationListener hideScrollViewListener = new AnimUtils.SimpleAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            overlayHolder.setVisibility(View.GONE);
            final Context context = OnboardActivity.this;
            if (loginLayout != null) {
                hideView(context, loginLayout, false);
            }
            if (signUpMethodLayout != null) {
                hideView(context, signUpMethodLayout, false);
            }
            if (signUpBasicsLayout != null) {
                hideView(context, signUpBasicsLayout, false);
            }
            if (signUpDetailsLayout != null) {
                hideView(context, signUpDetailsLayout, false);
            }
            if (acceptTermsLayout != null) {
                hideView(context, acceptTermsLayout, false);
            }
        }
    };
    @Nullable private Bundle loginBundle, signUpBasicsBundle, signUpDetailsBundle, acceptTermsBundle;

    private final Session.StatusCallback sessionStatusCallback = new FacebookSessionCallback(this);
    private PublicCloudAPI oldCloudAPI;
    private Session currentFacebookSession;
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

            if (!applicationProperties.isDevBuildRunningOnDevice() && SignupLog.shouldThrottleSignup()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_site))));
                finish();
            } else {
                setState(OnboardingState.SIGN_UP_METHOD);
            }
        }
    };
    private TourPhotoPagerAdapter photosAdapter;

    @Inject ConfigurationOperations configurationOperations;
    @Inject ApplicationProperties applicationProperties;
    @Inject BugReporter bugReporter;
    @Inject EventBus eventBus;

    public OnboardActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    OnboardActivity(ConfigurationOperations configurationOperations,
                    BugReporter bugReporter,
                    EventBus eventBus) {
        this.configurationOperations = configurationOperations;
        this.bugReporter = bugReporter;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);

        oauth = new OAuth(SoundCloudApplication.instance.getAccountOperations());
        oldCloudAPI = new PublicApi(this);

        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(this.getClass()));

        accountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onRequestContinued();
        }

        showPhotos(savedInstanceState == null);
        setButtonListeners();

        if (configurationOperations.shouldDisplayDeviceConflict()) {
            showDeviceConflictLogoutDialog();
            configurationOperations.clearDeviceConflict();
        }
    }

    private void showPhotos(boolean isNotConfigChange) {
        overridePendingTransition(0, 0);

        photoBottomBar = findViewById(R.id.tour_bottom_bar);
        photoLogo = findViewById(R.id.tour_logo);
        photoPager = (ViewPager) findViewById(R.id.tour_view);
        overlayBg = findViewById(R.id.overlay_bg);
        overlayHolder = findViewById(R.id.overlay_holder);

        photosAdapter = new TourPhotoPagerAdapter(this);

        buildPhotoPager(photosAdapter);

        setState(OnboardingState.PHOTOS);

        if (isNotConfigChange) {
            trackTourScreen();
        }

        final View splash = findViewById(R.id.splash);
        splash.setVisibility(isNotConfigChange ? View.VISIBLE : View.GONE);

        final PhotoLoadHandler photoLoadHandler = new PhotoLoadHandler(this, splash);
        photosAdapter.load(this, photoLoadHandler);
    }

    private void buildPhotoPager(PagerAdapter photosAdapter) {
        photoPager.setAdapter(photosAdapter);
        photoPager.setCurrentItem(0);
        photoPager.setOnPageChangeListener(onTourPageChange);
    }

    private void setButtonListeners() {
        findViewById(R.id.login_btn).setOnClickListener(onLoginButtonClick);
        findViewById(R.id.signup_btn).setOnClickListener(onSignupButtonClick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isBeingDestroyed = false;
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(this.getClass()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(this.getClass()));
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
    public void onSubmitUserDetails(String username, File avatarFile) {
        if (user == null) {
            return;
        }

        AddUserInfoTaskFragment.create(username, avatarFile).show(getSupportFragmentManager(), "add_user_task");
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.savedUserInfo(username, avatarFile));
    }

    @Override
    public void onSkipUserDetails() {
        new AuthTask(getApp(), new LegacyUserStorage()) {
            @Override
            protected AuthTaskResult doInBackground(Bundle... params) {
                addAccount(user, oldCloudAPI.getToken(), SignupVia.API);
                return null;
            }

            @Override
            protected void onPostExecute(AuthTaskResult result) {
                onAuthTaskComplete(user, SignupVia.API, false, false);
            }
        }.execute();
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.skippedUserInfo());
    }

    private SoundCloudApplication getApp() {
        return ((SoundCloudApplication) getApplication());
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
                onSkipUserDetails();
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
                lastAuthState = OnboardingState.LOGIN;
                hideViews(state, animated);
                showOverlay(getLoginLayout(), animated);
                break;

            case SIGN_UP_METHOD:
                lastAuthState = OnboardingState.SIGN_UP_METHOD;
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
            hideView(this, loginLayout, animated);
        }

        if (state != OnboardingState.SIGN_UP_METHOD && signUpMethodLayout != null) {
            hideView(this, signUpMethodLayout, animated);
        }

        if (state != OnboardingState.SIGN_UP_BASICS && signUpBasicsLayout != null) {
            hideView(this, signUpBasicsLayout, animated);
        }

        if (state != OnboardingState.SIGN_UP_DETAILS && signUpDetailsLayout != null) {
            hideView(this, signUpDetailsLayout, animated);
        }

        if (state != OnboardingState.ACCEPT_TERMS && acceptTermsLayout != null) {
            hideView(this, acceptTermsLayout, animated);
        }
    }

    @Override
    public void onGooglePlusAuth() {
        log(INFO, ONBOARDING_TAG, "on Google+ auth");

        final String[] names = AndroidUtils.getAccountsByType(this, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        if (names.length == 0) {
            onError(getString(R.string.authentication_no_google_accounts));
        } else if (names.length == 1) {
            onGoogleAccountSelected(names[0]);
        } else {
            ContextThemeWrapper cw = new ContextThemeWrapper(this, R.style.Theme_ScDialog);
            final AlertDialog.Builder builder = new AlertDialog.Builder(cw).setTitle(R.string.dialog_select_google_account);
            builder.setItems(names, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onGoogleAccountSelected(names[which]);
                }
            });
            builder.show();
        }
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

        hideView(this, getAcceptTermsLayout(), true);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.termsAccepted());
    }

    private void createNewUserFromEmail(Bundle signupParams) {
        SignupTaskFragment.create(signupParams).show(getSupportFragmentManager(), SIGNUP_DIALOG_TAG);
    }

    private void createNewUserFromGooglePlus(Bundle signupParams) {
        GooglePlusSignInTaskFragment.create(signupParams).show(getSupportFragmentManager(), SIGNUP_DIALOG_TAG);
    }

    private void createNewUserFromFacebook() {
        currentFacebookSession = new Session.Builder(getApplicationContext())
                .setTokenCachingStrategy(new NonCachingTokenCachingStrategy())
                .setApplicationId(getString(R.string.production_facebook_app_id))
                .build();
        currentFacebookSession.addCallback(sessionStatusCallback);

        Session.OpenRequest openRequest = new Session.OpenRequest(this);
        openRequest.setRequestCode(Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
        openRequest.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
        openRequest.setPermissions(DEFAULT_FACEBOOK_READ_PERMISSIONS);
        currentFacebookSession.openForRead(openRequest);
    }

    @Override
    public void onRejectTerms() {
        setState(OnboardingState.PHOTOS);
        trackTourScreen();
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.termsRejected());
    }

    @Override
    public void onAuthTaskComplete(PublicApiUser user, SignupVia via, boolean wasApiSignupTask, boolean showFacebookSuggestions) {
        log(INFO, ONBOARDING_TAG, "auth task complete, via: " + via + ", was api signup task: " + wasApiSignupTask);

        if (wasApiSignupTask) {
            SignupLog.writeNewSignupAsync();
            this.user = user;
            setState(OnboardingState.SIGN_UP_DETAILS);
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.AUTH_USER_DETAILS));
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());
        } else {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
            boolean wasSignup = via != SignupVia.NONE;
            resultBundle = result;

            sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                    .putExtra(PublicApiUser.EXTRA_ID, user.getId())
                    .putExtra(SignupVia.EXTRA, via.name));

            if (wasSignup || wasAuthorizedViaSignupScreen()) {
                startActivity(new Intent(this, SuggestedUsersActivity.class)
                        .putExtra(SuggestedUsersCategoriesFragment.SHOW_FACEBOOK, showFacebookSuggestions)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } else {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }

            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        photosAdapter.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        isBeingDestroyed = true;
        super.onSaveInstanceState(outState);

        outState.putString(LAST_GOOGLE_ACCT_USED, lastGoogleAccountSelected);
        outState.putSerializable(BUNDLE_STATE, state);
        outState.putParcelable(BUNDLE_USER, user);

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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        user = savedInstanceState.getParcelable(BUNDLE_USER);
        lastGoogleAccountSelected = savedInstanceState.getString(LAST_GOOGLE_ACCT_USED);

        loginBundle = savedInstanceState.getBundle(BUNDLE_LOGIN);
        signUpBasicsBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP_BASICS);
        signUpDetailsBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP_DETAILS);
        acceptTermsBundle = savedInstanceState.getBundle(BUNDLE_ACCEPT_TERMS);

        final OnboardingState state = (OnboardingState) savedInstanceState.getSerializable(BUNDLE_STATE);
        setState(state, false);
    }

    protected boolean wasAuthorizedViaSignupScreen() {
        return lastAuthState == OnboardingState.SIGN_UP_METHOD;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (currentFacebookSession != null) {
            currentFacebookSession.onActivityResult(this, requestCode, resultCode, intent);
        }
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

            case RequestCodes.SIGNUP_VIA_FACEBOOK: {
                if (intent != null && intent.hasExtra("error")) {
                    final String error = intent.getStringExtra("error");
                    AndroidUtils.showToast(this, error);
                } else {
                    finish();
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

            case RequestCodes.SIGNUP_VIA_GOOGLE: {
                onGoogleActivityResult(resultCode);
                break;
            }

            case RequestCodes.RECOVER_FROM_PLAY_SERVICES_ERROR: {
                onGoogleActivityResult(resultCode);
                break;
            }
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
        showView(this, photoBottomBar, animated);
        showView(this, photoLogo, animated);

        hideView(this, overlayBg, animated);

        photosAdapter.hideViewsOfLayout(this, photoPager.getCurrentItem());

        if (animated && overlayHolder.getVisibility() == View.VISIBLE) {
            hideView(this, overlayHolder, hideScrollViewListener);
        } else {
            hideScrollViewListener.onAnimationEnd(null);
        }
    }

    private void showOverlay(View overlay, boolean animated) {
        hideView(this, photoBottomBar, animated);
        hideView(this, photoLogo, animated);

        // hide foreground views
        photosAdapter.showViewsOfLayout(this, photoPager.getCurrentItem(), animated);

        showView(this, overlayHolder, animated);
        showView(this, overlayBg, animated);
        showView(this, overlay, animated);
    }

    @Override
    public GenderPickerDialogFragment.Callback getGenderPickerCallback() {
        return getSignUpBasicsLayout();
    }

    private void onGoogleAccountSelected(String name) {
        // store the last account name in case we have to retry after startActivityForResult with G+ app
        lastGoogleAccountSelected = name;
        proposeTermsOfUse(SignupVia.GOOGLE_PLUS, GooglePlusSignInTaskFragment.getParams(name, RequestCodes.SIGNUP_VIA_GOOGLE));
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
            final Bundle params = GooglePlusSignInTaskFragment.getParams(lastGoogleAccountSelected, RequestCodes.SIGNUP_VIA_GOOGLE);
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

    /**
     * Used for creating SoundCloud account from Facebook SDK
     *
     * @param data contains grant data and FB token
     */
    protected void login(Bundle data) {
        if (!isBeingDestroyed) {
            LoginTaskFragment.create(data).show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
        }
    }

    @Override
    public void onError(String message) {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(TextUtils.isEmpty(message) ? getString(R.string.authentication_signup_error_message) : message)
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupGeneralError());
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
                .setNeutralButton(getString(R.string.cancel), spamDialogOnClickListener);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupServeCaptcha());
    }

    @Override
    public void onBlocked() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_blocked_title)
                .setMessage(R.string.authentication_blocked_message)
                .setPositiveButton(R.string.contact_support, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse(getString(R.string.url_contact_support))));
                        dialogInterface.dismiss();
                    }
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
        final AlertDialog.Builder builder = new ImageAlertDialog(this)
                .setContent(R.drawable.dialog_device_management,
                        R.string.device_management_limit,
                        R.string.device_management_login_message)
                .setPositiveButton(R.string.device_management_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        login(loginBundle);
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        showDialogAndTrackEvent(builder, OnboardingEvent.deviceConflictOnLogin());
    }

    private void showDeviceConflictLogoutDialog() {
        final AlertDialog.Builder builder = new ImageAlertDialog(this)
                .setContent(R.drawable.dialog_device_management,
                        R.string.device_management_limit,
                        R.string.device_management_conflict_message)
                .setPositiveButton(R.string.device_management_continue, null);
        showDialogAndTrackEvent(builder, OnboardingEvent.deviceConflictLoggedOut());
    }

    private void showDialogAndTrackEvent(AlertDialog.Builder dialogBuilder, OnboardingEvent event) {
        if (!isFinishing()) {
            addFeedbackButton(dialogBuilder);
            dialogBuilder
                    .create()
                    .show();
            eventBus.publish(EventQueue.ONBOARDING, event);
        }
    }

    private void addFeedbackButton(AlertDialog.Builder dialogBuilder) {
        if (applicationProperties.shouldAllowFeedback()) {
            dialogBuilder
                    .setNeutralButton(R.string.title_feedback, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(ONBOARDING_TAG, "on send bug report");
                            bugReporter.showFeedbackDialog(getFragmentActivity());
                        }
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

    protected void setBundle(Bundle bundle) {
        this.resultBundle = bundle;
    }

}
