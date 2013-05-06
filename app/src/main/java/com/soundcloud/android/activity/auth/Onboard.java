package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Consts.RequestCodes;
import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.AnimUtils.hideView;
import static com.soundcloud.android.utils.AnimUtils.showView;
import static com.soundcloud.android.utils.ViewUtils.allChildViewsOf;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.dialog.auth.AddUserInfoTaskFragment;
import com.soundcloud.android.dialog.auth.GooglePlusSignInTaskFragment;
import com.soundcloud.android.dialog.auth.LoginTaskFragment;
import com.soundcloud.android.dialog.auth.SignupTaskFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.auth.AuthTask;
import com.soundcloud.android.task.auth.AuthTaskResult;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.tour.TourLayout;
import net.hockeyapp.android.UpdateManager;
import org.jetbrains.annotations.Nullable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
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


public class Onboard extends AbstractLoginActivity implements Login.LoginHandler, SignUp.SignUpHandler, UserDetails.UserDetailsHandler, AcceptTerms.AcceptTermsHandler {

    private static final String FOREGROUND_TAG = "foreground";
    private static final String PARALLAX_TAG = "parallax";

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

    private StartState mState = StartState.TOUR;

    private String mLastGoogleAccountSelected;
    @Nullable private User mUser;

    private View mTourBottomBar, mTourLogo, mOverlayBg, mScrollView;

    private List<TourLayout> mTourPages;

    private ViewPager mViewPager;

    @Nullable private Login  mLogin;

    @Nullable private SignUp mSignUp;
    @Nullable private UserDetails mUserDetails;
    @Nullable private AcceptTerms mAcceptTerms;
    @Nullable private Bundle mLoginBundle, mSignUpBundle, mUserDetailsBundle, mAcceptTermsBundle;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.start);
        overridePendingTransition(0, 0);
        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        mTourBottomBar = findViewById(R.id.tour_bottom_bar);
        mTourLogo      = findViewById(R.id.tour_logo);
        mViewPager     = (ViewPager) findViewById(R.id.tour_view);
        mOverlayBg     = findViewById(R.id.overlay_bg);
        mScrollView    = findViewById(R.id.scrollview);

        mTourPages = new ArrayList<TourLayout>();
        mTourPages.add(new TourLayout(this, R.layout.tour_page_1, R.drawable.tour_image_1));
        mTourPages.add(new TourLayout(this, R.layout.tour_page_2, R.drawable.tour_image_2));
        mTourPages.add(new TourLayout(this, R.layout.tour_page_3, R.drawable.tour_image_3));

        // randomize for variety
        Collections.shuffle(mTourPages);


        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mTourPages.size();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View v = mTourPages.get(position);
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

        mViewPager.setCurrentItem(0);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
                app.track(Click.Login);

                setState(StartState.LOGIN);
            }
        });
        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup);

                if (!SoundCloudApplication.DEV_MODE && SignupLog.shouldThrottleSignup()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.soundcloud.com")));
                    finish();
                } else {
                    setState(StartState.SIGN_UP);
                }
            }
        });

        if (SoundCloudApplication.BETA_MODE) {
            UpdateManager.register(this, getString(R.string.hockey_app_id));
        }

        setState(StartState.TOUR);

        TourLayout.load(this, mTourPages.toArray(new TourLayout[mTourPages.size()]));

        final View splash = findViewById(R.id.splash);
        // don't show splash screen on config changes
        splash.setVisibility(bundle == null ? View.VISIBLE : View.GONE);

        mTourPages.get(0).setLoadHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case TourLayout.IMAGE_LOADED:
                    case TourLayout.IMAGE_ERROR:
                        hideView(Onboard.this, splash, true);
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getApp().track(Page.Entry_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UpdateManager.unregister();

        for (TourLayout layout : mTourPages) {
            layout.recycle();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LAST_GOOGLE_ACCT_USED, mLastGoogleAccountSelected);
        outState.putSerializable(BUNDLE_STATE, getState());
        outState.putParcelable(BUNDLE_USER,    mUser);

        if (mLogin       != null) outState.putBundle(BUNDLE_LOGIN, mLogin.getStateBundle());
        if (mSignUp      != null) outState.putBundle(BUNDLE_SIGN_UP, mSignUp.getStateBundle());
        if (mUserDetails != null) outState.putBundle(BUNDLE_SIGN_UP_DETAILS, mUserDetails.getStateBundle());
        if (mAcceptTerms != null) outState.putBundle(BUNDLE_ACCEPT_TERMS, mAcceptTerms.getStateBundle());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mUser = savedInstanceState.getParcelable(BUNDLE_USER);
        mLastGoogleAccountSelected = savedInstanceState.getString(LAST_GOOGLE_ACCT_USED);

        mLoginBundle       = savedInstanceState.getBundle(BUNDLE_LOGIN);
        mSignUpBundle      = savedInstanceState.getBundle(BUNDLE_SIGN_UP);
        mUserDetailsBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP_DETAILS);
        mAcceptTermsBundle = savedInstanceState.getBundle(BUNDLE_ACCEPT_TERMS);

        final StartState state = (StartState) savedInstanceState.getSerializable(BUNDLE_STATE);
        setState(state, false);
    }


    public Login getLogin() {
        if (mLogin == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.login_stub);

            mLogin = (Login) stub.inflate();
            mLogin.setLoginHandler(this);
            mLogin.setVisibility(View.GONE);
            mLogin.setState(mLoginBundle);
        }

        return mLogin;
    }

    public SignUp getSignUp() {
        if (mSignUp == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.sign_up_stub);

            mSignUp = (SignUp) stub.inflate();
            mSignUp.setSignUpHandler(this);
            mSignUp.setVisibility(View.GONE);
            mSignUp.setState(mSignUpBundle);
        }

        return mSignUp;
    }

    public UserDetails getUserDetails() {
        if (mUserDetails == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.user_details_stub);

            mUserDetails = (UserDetails) stub.inflate();
            mUserDetails.setUserDetailsHandler(this);
            mUserDetails.setVisibility(View.GONE);
            mUserDetails.setState(mUserDetailsBundle);
        }

        return mUserDetails;
    }

    public AcceptTerms getAcceptTerms(){
        if (mAcceptTerms == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.accept_terms_stub);

            mAcceptTerms = (AcceptTerms) stub.inflate();
            mAcceptTerms.setAcceptTermsHandler(this);
            mAcceptTerms.setState(mAcceptTermsBundle);
            mAcceptTerms.setVisibility(View.GONE);
        }
        return mAcceptTerms;
    }

    @Override
    public void onLogin(String email, String password) {
        LoginTaskFragment.create(email, password).show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
    }

    @Override
    public void onCancelLogin() {
        setState(StartState.TOUR);
    }

    @Override
    public void onSignUp(final String email, final String password) {
        proposeTermsOfUse(SignupVia.API, SignupTaskFragment.getParams(email, password));
    }

    @Override
    public void onCancelSignUp() {
        setState(StartState.TOUR);
    }

    @Override
    public void onSubmitDetails(String username, File avatarFile) {
        if (mUser == null) {
            Log.w(TAG, "no user");
            return;
        }

        if (!TextUtils.isEmpty(username)) {
            mUser.username  = username;
            mUser.permalink = username;
        }

        AddUserInfoTaskFragment.create(mUser,avatarFile).show(getSupportFragmentManager(),"add_user_task");
    }

    @Override
    public void onSkipDetails() {
        new AuthTask(getApp(), new UserStorage()){
            @Override
            protected AuthTaskResult doInBackground(Bundle... params) {
                addAccount(mUser, getSoundCloudApplication().getToken(), SignupVia.API);
                return null;
            }

            @Override
            protected void onPostExecute(AuthTaskResult result) {
                onAuthTaskComplete(mUser, SignupVia.API, false);
            }
        }.execute();
    }

    @Override
    public void onBackPressed() {
        switch (getState()) {
            case LOGIN:
            case SIGN_UP:
            case ACCEPT_TERMS:
                setState(StartState.TOUR);
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
        return mState;
    }

    public void setState(StartState state) {
        setState(state, true);
    }

    public void setState(StartState state, boolean animated) {
        mState = state;

        // check for nulls when hiding to avoid unnecessary instantiation
        switch (mState) {
            case TOUR:
                onHideOverlay(animated);
                break;

            case LOGIN:
                if (mSignUp != null)        hideView(this, getSignUp(), animated);
                if (mUserDetails != null)   hideView(this, getUserDetails(), animated);
                if (mAcceptTerms != null)   hideView(this, getAcceptTerms(), animated);
                showOverlay(getLogin(), animated);
                break;

            case SIGN_UP:
                if (mLogin != null)         hideView(this, getLogin(), animated);
                if (mUserDetails != null)   hideView(this, getUserDetails(), animated);
                if (mAcceptTerms != null)   hideView(this, getAcceptTerms(), animated);
                showOverlay(getSignUp(), animated);
                break;

            case SIGN_UP_DETAILS:
                if (mLogin != null)         hideView(this, getLogin(), animated);
                if (mSignUp != null)        hideView(this, getSignUp(), animated);
                if (mAcceptTerms != null)   hideView(this, getAcceptTerms(), animated);
                showOverlay(getUserDetails(), animated);
                break;

            case ACCEPT_TERMS:
                if (mLogin != null)         hideView(this, getLogin(), animated);
                if (mSignUp != null)        hideView(this, getSignUp(), animated);
                if (mUserDetails != null)   hideView(this, getUserDetails(), animated);
                showOverlay(getAcceptTerms(), animated);
                break;
        }
    }

    private void onHideOverlay(boolean animated){
        showView(this, mTourBottomBar, animated);
        showView(this, mTourLogo, animated);
        hideView(this, mOverlayBg, animated);

        // show foreground views
        for (View view : allChildViewsOf(getCurrentTourLayout())) {
            if (isForegroundView(view)) showView(this, view, false);
        }

        if (animated && mScrollView.getVisibility() == View.VISIBLE){
            hideView(this, mScrollView, mHideScrollViewListener);
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
            mScrollView.setVisibility(View.GONE);
            if (mLogin != null) hideView(Onboard.this, getLogin(), false);
            if (mSignUp != null) hideView(Onboard.this, getSignUp(), false);
            if (mUserDetails != null) hideView(Onboard.this, getUserDetails(), false);
            if (mAcceptTerms != null) hideView(Onboard.this, getAcceptTerms(), false);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void showOverlay(View overlay, boolean animated){
        hideView(this, mTourBottomBar, animated);
        hideView(this, mTourLogo, animated);

        // hide foreground views
        for (View view : allChildViewsOf(getCurrentTourLayout())) {
            if (isForegroundView(view)) hideView(this, view, animated);
        }
        showView(this, mScrollView, animated);
        showView(this, mOverlayBg, animated);
        showView(this, overlay, animated);
    }

    private TourLayout getCurrentTourLayout() {
        return mTourPages.get(mViewPager.getCurrentItem());
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
            new AlertDialog.Builder(this).setTitle(R.string.dialog_select_google_account)
                    .setItems(names, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onGoogleAccountSelected(names[which]);
                        }
                    }).show();
        }
    }

    @Override
    public void onFacebookAuth() {
        getApp().track(Click.Login_with_facebook);
        proposeTermsOfUse(SignupVia.FACEBOOK_SSO, null);
    }

    @Override
    public void onShowTermsOfUse() {
        getApp().track(Click.Signup_Signup_terms);
        startActivity(new Intent(Intent.ACTION_VIEW, TERMS_OF_USE_URL));
    }

    @Override
    public void onShowPrivacyPolicy() {
        getApp().track(Click.Signup_Signup_privacy);
        startActivity(new Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL));

    }

    @Override
    public void onShowCookiePolicy() {
        getApp().track(Click.Signup_Signup_cookies);
        startActivity(new Intent(Intent.ACTION_VIEW, COOKIE_POLICY_URL));
    }


    @Override
    public void onRecover(String email) {
        Intent recoveryIntent = new Intent(this, Recover.class);
        if (email != null && email.length() > 0) {
            recoveryIntent.putExtra("email", email);
        }
        startActivityForResult(recoveryIntent, RequestCodes.RECOVER_CODE);
    }

    @Override
    public void onAcceptTerms(SignupVia signupVia, Bundle signupParams) {
        switch (signupVia){
            case GOOGLE_PLUS:
                GooglePlusSignInTaskFragment.create(signupParams).show(getSupportFragmentManager(), "signup_dialog");
                break;
            case FACEBOOK_SSO:
                startActivityForResult(new Intent(this, Facebook.class), RequestCodes.SIGNUP_VIA_FACEBOOK);
                break;
            case API:
                SignupTaskFragment.create(signupParams).show(getSupportFragmentManager(), "signup_dialog");
                break;
        }
    }

    @Override
    public void onCancel() {
        setState(StartState.TOUR);
    }

    @Override
    public void onAuthTaskComplete(User user, SignupVia via, boolean wasApiSignupTask) {
        if (wasApiSignupTask){
            SignupLog.writeNewSignupAsync();
            mUser = user;
            setState(StartState.SIGN_UP_DETAILS);
        } else {
            super.onAuthTaskComplete(user, via, wasApiSignupTask);
        }
    }

    private void onGoogleAccountSelected(String name) {
        // store the last account name in case we have to retry after startActivityForResult with G+ app
        mLastGoogleAccountSelected = name;
        getApp().track(Click.Login_with_googleplus);
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

            case RequestCodes.IMAGE_CROP: {
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
                if (resultCode == RESULT_OK) onGoogleAccountSelected(mLastGoogleAccountSelected);
                break;
            }

            case RequestCodes.RECOVER_FROM_PLAY_SERVICES_ERROR: {
                if (resultCode == RESULT_OK) onGoogleAccountSelected(mLastGoogleAccountSelected);
                break;
            }

        }
    }
}
