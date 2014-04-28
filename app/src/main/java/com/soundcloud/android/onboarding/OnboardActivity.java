package com.soundcloud.android.onboarding;

import static com.soundcloud.android.Consts.RequestCodes;
import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.AnimUtils.hideView;
import static com.soundcloud.android.utils.AnimUtils.showView;
import static com.soundcloud.android.utils.ViewUtils.allChildViewsOf;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.AbstractLoginActivity;
import com.soundcloud.android.onboarding.auth.AcceptTermsLayout;
import com.soundcloud.android.onboarding.auth.AddUserInfoTaskFragment;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.onboarding.auth.FacebookSwitcherActivity;
import com.soundcloud.android.onboarding.auth.GooglePlusSignInTaskFragment;
import com.soundcloud.android.onboarding.auth.LoginLayout;
import com.soundcloud.android.onboarding.auth.LoginTaskFragment;
import com.soundcloud.android.onboarding.auth.RecoverActivity;
import com.soundcloud.android.onboarding.auth.SignUpLayout;
import com.soundcloud.android.onboarding.auth.SignupLog;
import com.soundcloud.android.onboarding.auth.SignupTaskFragment;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.UserDetailsLayout;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import eu.inmite.android.lib.dialogs.ISimpleDialogListener;
import net.hockeyapp.android.UpdateManager;
import org.jetbrains.annotations.Nullable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnboardActivity extends AbstractLoginActivity implements ISimpleDialogListener, LoginLayout.LoginHandler, SignUpLayout.SignUpHandler, UserDetailsLayout.UserDetailsHandler, AcceptTermsLayout.AcceptTermsHandler {

    private static final String FOREGROUND_TAG = "foreground";
    private static final String PARALLAX_TAG = "parallax";
    private static final String SIGNUP_DIALOG_TAG = "signup_dialog";
    public static final int DIALOG_PICK_IMAGE = 1;

    protected enum StartState {
        TOUR, LOGIN, SIGN_UP, SIGN_UP_DETAILS, ACCEPT_TERMS;
    }
    private static final String BUNDLE_STATE           = "BUNDLE_STATE";
    private static final String BUNDLE_USER            = "BUNDLE_USER";
    private static final String BUNDLE_LOGIN           = "BUNDLE_LOGIN";
    private static final String BUNDLE_SIGN_UP         = "BUNDLE_SIGN_UP";
    private static final String BUNDLE_SIGN_UP_DETAILS = "BUNDLE_SIGN_UP_DETAILS";
    private static final String BUNDLE_ACCEPT_TERMS    = "BUNDLE_ACCEPT_TERMS";
    private static final String LAST_GOOGLE_ACCT_USED  = "BUNDLE_LAST_GOOGLE_ACCOUNT_USED";
    private static final Uri TERMS_OF_USE_URL = Uri.parse("http://m.soundcloud.com/terms-of-use");
    private static final Uri PRIVACY_POLICY_URL = Uri.parse("http://m.soundcloud.com/pages/privacy");
    private static final Uri COOKIE_POLICY_URL = Uri.parse("http://m.soundcloud.com/pages/privacy#cookies");

    private StartState lastAuthState;

    private StartState state = StartState.TOUR;

    private String lastGoogleAccountSelected;
    @Nullable private User user;

    private View tourBottomBar, tourLogo, overlayBg, overlayHolder;

    private List<TourLayout> tourPages;

    private ViewPager viewPager;

    @Nullable private LoginLayout login;
    @Nullable private SignUpLayout signUp;
    @Nullable private UserDetailsLayout userDetails;
    @Nullable private AcceptTermsLayout acceptTerms;
    @Nullable private Bundle loginBundle, signUpBundle, userDetailsBundle, acceptTermsBundle;

    private PublicCloudAPI oldCloudAPI;
    private ApplicationProperties applicationProperties;
    private EventBus eventBus;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.start);

        eventBus = SoundCloudApplication.fromContext(this).getEventBus();
        oldCloudAPI = new PublicApi(this);
        overridePendingTransition(0, 0);

        tourBottomBar = findViewById(R.id.tour_bottom_bar);
        tourLogo = findViewById(R.id.tour_logo);
        viewPager = (ViewPager) findViewById(R.id.tour_view);
        overlayBg = findViewById(R.id.overlay_bg);
        overlayHolder = findViewById(R.id.overlay_holder);

        tourPages = new ArrayList<TourLayout>();
        tourPages.add(new TourLayout(this, R.layout.tour_page_1, R.drawable.tour_image_1));
        tourPages.add(new TourLayout(this, R.layout.tour_page_2, R.drawable.tour_image_2));
        tourPages.add(new TourLayout(this, R.layout.tour_page_3, R.drawable.tour_image_3));
        applicationProperties = new ApplicationProperties(getResources());
        // randomize for variety
        Collections.shuffle(tourPages);


        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return tourPages.size();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View v = tourPages.get(position);
                v.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                container.addView(v);
                return v;
            }

            @Override
            public void destroyItem(View collection, int position, Object view) {
                ((ViewPager) collection).removeView((View) view);
            }


            @Override
            public boolean isViewFromObject(View view, Object object) {
                return object == view;
            }
        });

        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
        });

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setState(StartState.LOGIN);
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.AUTH_LOG_IN.get());
                eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.logInPrompt());
            }
        });

        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.AUTH_SIGN_UP.get());
                eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.signUpPrompt());

                if (!applicationProperties.isDevBuildRunningOnDalvik() && SignupLog.shouldThrottleSignup()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.soundcloud.com")));
                    finish();
                } else {
                    setState(StartState.SIGN_UP);
                }
            }
        });

        if (applicationProperties.isBetaBuildRunningOnDalvik()) {
            UpdateManager.register(this, getString(R.string.hockey_app_id));
        }

        setState(StartState.TOUR);
        if (bundle == null) {
            trackTourScreen();
        }

        TourLayout.load(this, tourPages.toArray(new TourLayout[tourPages.size()]));

        final View splash = findViewById(R.id.splash);
        // don't show splash screen on config changes
        splash.setVisibility(bundle == null ? View.VISIBLE : View.GONE);

        tourPages.get(0).setLoadHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case TourLayout.IMAGE_LOADED:
                    case TourLayout.IMAGE_ERROR:
                        hideView(OnboardActivity.this, splash, true);
                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UpdateManager.unregister();

        for (TourLayout layout : tourPages) {
            layout.recycle();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LAST_GOOGLE_ACCT_USED, lastGoogleAccountSelected);
        outState.putSerializable(BUNDLE_STATE, getState());
        outState.putParcelable(BUNDLE_USER, user);

        if (login != null) outState.putBundle(BUNDLE_LOGIN, login.getStateBundle());
        if (signUp != null) outState.putBundle(BUNDLE_SIGN_UP, signUp.getStateBundle());
        if (userDetails != null) outState.putBundle(BUNDLE_SIGN_UP_DETAILS, userDetails.getStateBundle());
        if (acceptTerms != null) outState.putBundle(BUNDLE_ACCEPT_TERMS, acceptTerms.getStateBundle());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        user = savedInstanceState.getParcelable(BUNDLE_USER);
        lastGoogleAccountSelected = savedInstanceState.getString(LAST_GOOGLE_ACCT_USED);

        loginBundle = savedInstanceState.getBundle(BUNDLE_LOGIN);
        signUpBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP);
        userDetailsBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP_DETAILS);
        acceptTermsBundle = savedInstanceState.getBundle(BUNDLE_ACCEPT_TERMS);

        final StartState state = (StartState) savedInstanceState.getSerializable(BUNDLE_STATE);
        setState(state, false);
    }

    private void trackTourScreen() {
        eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.TOUR.get());
    }

    private LoginLayout getLogin() {
        if (login == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.login_stub);

            login = (LoginLayout) stub.inflate();
            login.setLoginHandler(this);
            login.setVisibility(View.GONE);
            login.setState(loginBundle);
        }

        return login;
    }

    private SignUpLayout getSignUp() {
        if (signUp == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.sign_up_stub);

            signUp = (SignUpLayout) stub.inflate();
            signUp.setSignUpHandler(this);
            signUp.setVisibility(View.GONE);
            signUp.setState(signUpBundle);
        }

        return signUp;
    }

    private UserDetailsLayout getUserDetails() {
        if (userDetails == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.user_details_stub);

            userDetails = (UserDetailsLayout) stub.inflate();
            userDetails.setUserDetailsHandler(this);
            userDetails.setVisibility(View.GONE);
            userDetails.setState(userDetailsBundle);
        }

        return userDetails;
    }

    private AcceptTermsLayout getAcceptTerms(){
        if (acceptTerms == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.accept_terms_stub);

            acceptTerms = (AcceptTermsLayout) stub.inflate();
            acceptTerms.setAcceptTermsHandler(this);
            acceptTerms.setState(acceptTermsBundle);
            acceptTerms.setVisibility(View.GONE);
        }
        return acceptTerms;
    }

    @Override
    public void onLogin(String email, String password) {
        LoginTaskFragment.create(email, password).show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.nativeAuthEvent());
    }

    @Override
    public void onCancelLogin() {
        setState(StartState.TOUR);
        trackTourScreen();
    }

    @Override
    public void onSignUp(final String email, final String password) {
        proposeTermsOfUse(SignupVia.API, SignupTaskFragment.getParams(email, password));
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.nativeAuthEvent());
    }

    @Override
    public void onCancelSignUp() {
        setState(StartState.TOUR);
        trackTourScreen();
    }

    @Override
    public void onSubmitDetails(String username, File avatarFile) {
        if (user == null) {
            Log.w(TAG, "no user");
            return;
        }

        AddUserInfoTaskFragment.create(username, avatarFile).show(getSupportFragmentManager(), "add_user_task");
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.savedUserInfo(username, avatarFile));
    }

    @Override
    public void onSkipDetails() {
        new AuthTask(getApp(), new UserStorage()){
            @Override
            protected AuthTaskResult doInBackground(Bundle... params) {
                addAccount(user, oldCloudAPI.getToken(), SignupVia.API);
                return null;
            }

            @Override
            protected void onPostExecute(AuthTaskResult result) {
                onAuthTaskComplete(user, SignupVia.API, false);
            }
        }.execute();
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.skippedUserInfo());
    }

    @Override
    public FragmentActivity getFragmentActivity() {
        return this;
    }

    @Override
    public void onBackPressed() {
        switch (getState()) {
            case LOGIN:
            case SIGN_UP:
            case ACCEPT_TERMS:
                setState(StartState.TOUR);
                trackTourScreen();
                break;

            case SIGN_UP_DETAILS:
                onSkipDetails();
                break;

            case TOUR:
                super.onBackPressed();
                break;
        }
    }

    public StartState getState() {
        return state;
    }

    public void setState(StartState state) {
        setState(state, true);
    }

    public void setState(StartState state, boolean animated) {
        this.state = state;

        // check for nulls when hiding to avoid unnecessary instantiation
        switch (this.state) {
            case TOUR:
                onHideOverlay(animated);
                break;

            case LOGIN:
                lastAuthState = StartState.LOGIN;
                if (signUp != null)        hideView(this, getSignUp(), animated);
                if (userDetails != null)   hideView(this, getUserDetails(), animated);
                if (acceptTerms != null)   hideView(this, getAcceptTerms(), animated);
                showOverlay(getLogin(), animated);
                break;

            case SIGN_UP:
                lastAuthState = StartState.SIGN_UP;
                if (login != null)         hideView(this, getLogin(), animated);
                if (userDetails != null)   hideView(this, getUserDetails(), animated);
                if (acceptTerms != null)   hideView(this, getAcceptTerms(), animated);
                showOverlay(getSignUp(), animated);
                break;

            case SIGN_UP_DETAILS:
                if (login != null)         hideView(this, getLogin(), animated);
                if (signUp != null)        hideView(this, getSignUp(), animated);
                if (acceptTerms != null)   hideView(this, getAcceptTerms(), animated);
                showOverlay(getUserDetails(), animated);
                break;

            case ACCEPT_TERMS:
                if (login != null)         hideView(this, getLogin(), false);
                if (signUp != null)        hideView(this, getSignUp(), false);
                if (userDetails != null)   hideView(this, getUserDetails(), false);
                showOverlay(getAcceptTerms(), animated);
                break;
        }
    }

    private void onHideOverlay(boolean animated){
        showView(this, tourBottomBar, animated);
        showView(this, tourLogo, animated);
        hideView(this, overlayBg, animated);

        // show foreground views
        for (View view : allChildViewsOf(getCurrentTourLayout())) {
            if (isForegroundView(view)) showView(this, view, false);
        }

        if (animated && overlayHolder.getVisibility() == View.VISIBLE){
            hideView(this, overlayHolder, mHideScrollViewListener);
        } else {
            mHideScrollViewListener.onAnimationEnd(null);
        }
    }

    private Animation.AnimationListener mHideScrollViewListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            overlayHolder.setVisibility(View.GONE);
            if (login != null) hideView(OnboardActivity.this, getLogin(), false);
            if (signUp != null) hideView(OnboardActivity.this, getSignUp(), false);
            if (userDetails != null) hideView(OnboardActivity.this, getUserDetails(), false);
            if (acceptTerms != null) hideView(OnboardActivity.this, getAcceptTerms(), false);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void showOverlay(View overlay, boolean animated){
        hideView(this, tourBottomBar, animated);
        hideView(this, tourLogo, animated);

        // hide foreground views
        for (View view : allChildViewsOf(getCurrentTourLayout())) {
            if (isForegroundView(view)) hideView(this, view, animated);
        }
        showView(this, overlayHolder, animated);
        showView(this, overlayBg, animated);
        showView(this, overlay, animated);
    }

    private TourLayout getCurrentTourLayout() {
        return tourPages.get(viewPager.getCurrentItem());
    }

    private static boolean isForegroundView(View view) {
        final Object tag = view.getTag();
        return FOREGROUND_TAG.equals(tag) || PARALLAX_TAG.equals(tag);
    }

    @Override
    public void onGooglePlusAuth() {
        final String[] names = AndroidUtils.getAccountsByType(this, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        if (names.length == 0){
            onError(getString(R.string.authentication_no_google_accounts));
        } else if (names.length == 1){
            onGoogleAccountSelected(names[0]);
        } else {
            ContextThemeWrapper cw = new ContextThemeWrapper( this, R.style.Theme_ScDialog );
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
        proposeTermsOfUse(SignupVia.FACEBOOK_SSO, null);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.facebookAuthEvent());
    }

    @Override
    public void onShowTermsOfUse() {
        startActivity(new Intent(Intent.ACTION_VIEW, TERMS_OF_USE_URL));
    }

    @Override
    public void onShowPrivacyPolicy() {
        startActivity(new Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL));
    }

    @Override
    public void onShowCookiePolicy() {
        startActivity(new Intent(Intent.ACTION_VIEW, COOKIE_POLICY_URL));
    }

    @Override
    public void onRecover(String email) {
        Intent recoveryIntent = new Intent(this, RecoverActivity.class);
        if (email != null && email.length() > 0) {
            recoveryIntent.putExtra("email", email);
        }
        startActivityForResult(recoveryIntent, RequestCodes.RECOVER_CODE);
    }

    @Override
    public void onAcceptTerms(SignupVia signupVia, Bundle signupParams) {
        setState(StartState.TOUR);
        switch (signupVia){
            case GOOGLE_PLUS:
                GooglePlusSignInTaskFragment.create(signupParams).show(getSupportFragmentManager(), SIGNUP_DIALOG_TAG);
                break;
            case FACEBOOK_SSO:
                startActivityForResult(new Intent(this, FacebookSwitcherActivity.class)
                        .putExtra(FacebookSSOActivity.VIA_SIGNUP_SCREEN, lastAuthState == StartState.SIGN_UP),
                        RequestCodes.SIGNUP_VIA_FACEBOOK);
                break;
            case API:
                SignupTaskFragment.create(signupParams).show(getSupportFragmentManager(), SIGNUP_DIALOG_TAG);
                break;
        }

        hideView(this, getAcceptTerms(), true);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.termsAccepted());

    }

    @Override
    public void onRejectTerms() {
        setState(StartState.TOUR);
        trackTourScreen();
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.termsRejected());
    }

    @Override
    public void onAuthTaskComplete(User user, SignupVia via, boolean wasApiSignupTask) {
        if (wasApiSignupTask){
            SignupLog.writeNewSignupAsync();
            this.user = user;
            setState(StartState.SIGN_UP_DETAILS);
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.AUTH_USER_DETAILS.get());
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());
        } else {
            super.onAuthTaskComplete(user, via, wasApiSignupTask);
        }
    }

    @Override
    protected boolean wasAuthorizedViaSignupScreen() {
        return lastAuthState == StartState.SIGN_UP;
    }

    private void onGoogleAccountSelected(String name) {
        // store the last account name in case we have to retry after startActivityForResult with G+ app
        lastGoogleAccountSelected = name;
        proposeTermsOfUse(SignupVia.GOOGLE_PLUS, GooglePlusSignInTaskFragment.getParams(name, RequestCodes.SIGNUP_VIA_GOOGLE));
    }

    /**
     * Set signup params on accept terms view and change state
     * @param signupVia
     * @param params
     */
    private void proposeTermsOfUse(SignupVia signupVia, Bundle params){
        getAcceptTerms().setSignupParams(signupVia, params);
        setState(StartState.ACCEPT_TERMS);
        eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.AUTH_TERMS.get());
    }

    private SoundCloudApplication getApp() {
        return ((SoundCloudApplication) getApplication());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case RequestCodes.GALLERY_IMAGE_PICK: {
                if (getUserDetails() != null) {
                    getUserDetails().onImagePick(resultCode, intent);
                }
                break;
            }

            case RequestCodes.GALLERY_IMAGE_TAKE: {
                if (getUserDetails() != null) {
                    getUserDetails().onImageTake(resultCode, intent);
                }
                break;
            }

            case Crop.REQUEST_CROP: {
                if (getUserDetails() != null) {
                    getUserDetails().onImageCrop(resultCode, intent);
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
            case RequestCodes.RECOVER_CODE: {
                if (resultCode != RESULT_OK || intent == null) break;
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

    @Override
    public void onPositiveButtonClicked(int requestCode) {
        switch (requestCode){
            case DIALOG_PICK_IMAGE :
                ImageUtils.startTakeNewPictureIntent(this, getUserDetails().generateTempAvatarFile(),
                        Consts.RequestCodes.GALLERY_IMAGE_TAKE);
                break;
        }
    }

    @Override
    public void onNegativeButtonClicked(int requestCode) {
        switch (requestCode){
            case DIALOG_PICK_IMAGE :
                ImageUtils.startPickImageIntent(this, Consts.RequestCodes.GALLERY_IMAGE_PICK);
        }
    }

    private void onGoogleActivityResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            // just kick off another task with the last account selected
            final Bundle params = GooglePlusSignInTaskFragment.getParams(lastGoogleAccountSelected, RequestCodes.SIGNUP_VIA_GOOGLE);
            GooglePlusSignInTaskFragment.create(params).show(getSupportFragmentManager(), SIGNUP_DIALOG_TAG);
        }
    }
}
