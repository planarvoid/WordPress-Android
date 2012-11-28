package com.soundcloud.android.activity.auth;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.soundcloud.android.*;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.AddUserInfoTask;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.SignupTask;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.tour.TourLayout;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import net.hockeyapp.android.UpdateManager;

import android.accounts.AccountAuthenticatorActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import org.jetbrains.annotations.Nullable;

import java.io.*;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.ViewUtils.allChildViewsOf;

public class Start extends AccountAuthenticatorActivity implements Login.LoginHandler, SignUp.SignUpHandler, UserDetails.UserDetailsHandler {
    protected enum StartState {
        TOUR, LOGIN, SIGN_UP, SIGN_UP_DETAILS;
    }

    private static final String BUNDLE_STATE           = "BUNDLE_STATE";
    private static final String BUNDLE_USER            = "BUNDLE_USER";
    private static final String BUNDLE_LOGIN           = "BUNDLE_LOGIN";
    private static final String BUNDLE_SIGN_UP         = "BUNDLE_SIGN_UP";
    private static final String BUNDLE_SIGN_UP_DETAILS = "BUNDLE_SIGN_UP_DETAILS";

    private static final File SIGNUP_LOG = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".dr");

    public static final String FB_CONNECTED_EXTRA    = "facebook_connected";
    public static final String TOUR_BACKGROUND_EXTRA = "tour_background";

    private static final Uri TERMS_OF_USE_URL = Uri.parse("http://m.soundcloud.com/terms-of-use");
    public static final int THROTTLE_WINDOW = 60 * 60 * 1000;

    public static final int THROTTLE_AFTER_ATTEMPT = 3;

    private StartState mState = StartState.TOUR;

    @Nullable private User mUser;

    private View mTourBottomBar;

    private TourLayout[] mTourPages;

    private ViewPager mViewPager;

    @Nullable private Login  mLogin;
    @Nullable private SignUp mSignUp;
    @Nullable private UserDetails mUserDetails;

    @Nullable private Bundle mLoginBundle, mSignUpBundle, mUserDetailsBundle;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.start);

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        mTourBottomBar = findViewById(R.id.tour_bottom_bar);

        mViewPager = (ViewPager) findViewById(R.id.tour_view);
        mTourPages = new TourLayout[]{
            new TourLayout(this, R.layout.tour_page_1, R.drawable.tour_image_1),
            new TourLayout(this, R.layout.tour_page_2, R.drawable.tour_image_2),
            new TourLayout(this, R.layout.tour_page_3, R.drawable.tour_image_3)
        };

        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mTourPages.length;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View v = mTourPages[position];
                v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
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

        findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               onFacebookLogin();
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

                if (shouldThrottleSignup(Start.this)) {
                    // TODO: bring up mobile website
                    setState(StartState.TOUR);
                } else {
                    setState(StartState.SIGN_UP);
                }
            }
        });

        if (SoundCloudApplication.BETA_MODE) {
            UpdateManager.register(this, getString(R.string.hockey_app_id));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication)getApplication()).track(Page.Entry_main);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(BUNDLE_STATE, getState());
        outState.putParcelable(BUNDLE_USER,    mUser);

        if (mLogin         != null) outState.putBundle(BUNDLE_LOGIN, mLogin.getStateBundle());
        if (mSignUp        != null) outState.putBundle(BUNDLE_SIGN_UP, mSignUp.getStateBundle());
        if (mUserDetails != null) outState.putBundle(BUNDLE_SIGN_UP_DETAILS, mUserDetails.getStateBundle());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mUser = savedInstanceState.getParcelable(BUNDLE_USER);

        mLoginBundle         = savedInstanceState.getBundle(BUNDLE_LOGIN);
        mSignUpBundle        = savedInstanceState.getBundle(BUNDLE_SIGN_UP);
        mUserDetailsBundle = savedInstanceState.getBundle(BUNDLE_SIGN_UP_DETAILS);

        setState((StartState) savedInstanceState.getSerializable(BUNDLE_STATE), false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (TourLayout layout : mTourPages) {
            layout.recycle();
        }
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

    static boolean shouldThrottleSignup(Context context) {
        AndroidCloudAPI api = (AndroidCloudAPI) context.getApplicationContext();
        // don't throttle sandbox requests - we need it for integration testing
        if (api.getEnv() ==  Env.SANDBOX) return false;

        final long[] signupLog = readLog();
        if (signupLog == null) {
            return false;
        } else {
            int i = signupLog.length - 1;
            while (i >= 0 &&
                    System.currentTimeMillis() - signupLog[i] < THROTTLE_WINDOW &&
                    signupLog.length - i <= THROTTLE_AFTER_ATTEMPT) {
                i--;
            }
            return signupLog.length - i > THROTTLE_AFTER_ATTEMPT;
        }
    }

    static boolean writeNewSignupToLog() {
        return writeNewSignupToLog(System.currentTimeMillis());
    }

    static boolean writeNewSignupToLog(long timestamp) {
        long[] toWrite, current = readLog();
        if (current == null) {
            toWrite = new long[1];
        } else {
            toWrite = new long[current.length + 1];
            System.arraycopy(current, 0, toWrite, 0, current.length);
        }
        toWrite[toWrite.length - 1] = timestamp;
        return writeLog(toWrite);
    }

    static boolean writeLog(long[] toWrite) {
        try {
            IOUtils.mkdirs(SIGNUP_LOG.getParentFile());

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SIGNUP_LOG));
            out.writeObject(toWrite);
            out.close();
            return true;
        } catch (IOException e) {
            Log.w(SoundCloudApplication.TAG, "Error writing to sign up log ", e);
            return false;
        }
    }

    @Nullable static long[] readLog() {

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(SIGNUP_LOG));
            return (long[]) in.readObject();
        } catch (IOException e) {
            Log.e(SoundCloudApplication.TAG, "Error reading sign up log ", e);
        } catch (ClassNotFoundException e) {
            Log.e(SoundCloudApplication.TAG, "Error reading sign up log ", e);
        }
        return null;
    }

    @Override
    public void onLogin(String email, String password) {
        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        final Bundle param = new Bundle();
        param.putString("username", email);
        param.putString("password", password);

        new GetTokensTask(app) {
            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                if (!isFinishing()) {
                    progress = AndroidUtils.showProgress(Start.this,
                                                         R.string.authentication_login_progress_message);
                }
            }

            @Override
            protected void onPostExecute(final Token token) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "GetTokensTask#onPostExecute("+token+")");

                if (!isFinishing()) {
                    try {
                        progress.dismiss();
                    } catch (IllegalArgumentException ignored) {}
                }

                if (token != null) {
                    new FetchUserTask(app) {
                        @Override
                        protected void onPostExecute(User user) {
                            // need to create user account as soon as possible, so the executeRefreshTask logic in
                            // SoundCloudApplication works properly
                            final boolean signedUp = app.addUserAccount(user, app.getToken(), SignupVia.API);

                            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "GetTokensTask#onPostExecute("+user+")");

                            if (user != null) {
                                startActivityForResult(new Intent(Start.this, Home.class), 0);
                                Start.this.finish();
                            } else { // user request failed
                                presentError(null);
                            }
                        }
                    }.execute(Request.to(Endpoints.MY_DETAILS));
                } else { // no tokens obtained
                    presentError(mException.getLocalizedMessage());
                }
            }
        }.execute(param);
    }

    @Override
    public void onCancelLogin() {
        setState(StartState.TOUR);
    }

    @Override
    public void onSignUp(final String email, final String password) {
        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        final Bundle param = new Bundle();
        param.putString("username", email);
        param.putString("password", password);


        new SignupTask(app) {
            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                progress = AndroidUtils.showProgress(Start.this,
                                                     R.string.authentication_signup_progress_message);
            }

            @Override
            protected void onPostExecute(final User user) {
                if (!isFinishing()) {
                    try {
                        progress.dismiss();
                    } catch (IllegalArgumentException ignored) {}
                }

                if (user != null) {
                    writeNewSignupToLog();

                    // need to create user account as soon as possible, so the executeRefreshTask logic in
                    // SoundCloudApplication works properly
                    final boolean signedUp = app.addUserAccount(user, app.getToken(), SignupVia.API);

                    new GetTokensTask(mApi) {
                        @Override protected void onPostExecute(Token token) {
                            if (token != null) {
                                mUser = user;

                                setState(StartState.SIGN_UP_DETAILS);
                            } else {
                                presentError(null);
                            }
                        }
                    }.execute(param);
                } else {
                    presentError(getFirstError());
                }
            }
        }.execute(email, password);
    }

    @Override
    public void onCancelSignUp() {
        setState(StartState.TOUR);
    }

    @Override
    public void onSubmitDetails(String username, File avatarFile) {
        if (!TextUtils.isEmpty(username)) {
            mUser.username  = username;
            mUser.permalink = username;
        }
        new AddUserInfoTask((AndroidCloudAPI) getApplication()) {
            ProgressDialog dialog;
            @Override protected void onPreExecute() {
                dialog = AndroidUtils.showProgress(Start.this, R.string.authentication_add_info_progress_message);
            }

            @Override protected void onPostExecute(User user) {
                if (!isFinishing()) {
                    try {
                        if (dialog != null) dialog.dismiss();
                    } catch (IllegalArgumentException ignored) {
                    }

                    if (user != null) {
                        startActivityForResult(new Intent(Start.this, Home.class), 0);
                        finish();
                    } else {
                        presentError(getFirstError());
                    }
                }
            }
        }.execute(Pair.create(mUser, avatarFile));

    }

    @Override
    public void onSkipDetails() {
        startActivityForResult(new Intent(Start.this, Home.class), 0);
        finish();
    }

    @Override
    public void onBackPressed() {
        switch (getState()) {
            case LOGIN:
            case SIGN_UP:
                setState(StartState.TOUR);
                return;

            case SIGN_UP_DETAILS:
                finish();
                return;

            case TOUR:
                super.onBackPressed();
                return;
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

        switch (mState) {
            case TOUR:
                showForegroundViews(animated);

                hideView(getLogin(),         animated);
                hideView(getSignUp(),        animated);
                hideView(getUserDetails(), animated);
                return;

            case LOGIN:
                hideForegroundViews(animated);

                showView(getLogin(),         animated);
                hideView(getSignUp(),        animated);
                hideView(getUserDetails(), animated);
                return;

            case SIGN_UP:
                hideForegroundViews(animated);

                hideView(getLogin(),         animated);
                showView(getSignUp(),        animated);
                hideView(getUserDetails(), animated);
                return;

            case SIGN_UP_DETAILS:
                hideForegroundViews(animated);

                hideView(getLogin(),         animated);
                hideView(getSignUp(), animated);
                showView(getUserDetails(), animated);
                return;
        }
    }

    private void showForegroundViews(boolean animated) {
        showView(mTourBottomBar, animated);

        for (View view : allChildViewsOf(getCurrentTourLayout())) {
            if (isForegroundView(view)) showView(view, animated);
        }
    }

    private void hideForegroundViews(boolean animated) {
        hideView(mTourBottomBar, animated);

        for (View view : allChildViewsOf(getCurrentTourLayout())) {
            if (isForegroundView(view)) hideView(view, animated);
        }
    }

    private void showView(final View view, boolean animated) {
        view.clearAnimation();

        if (view.getVisibility() == View.VISIBLE) return;

        if (!animated) {
            view.setVisibility(View.VISIBLE);
        } else {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

            view.setVisibility(View.VISIBLE);
            view.startAnimation(animation);
        }
    }

    private void hideView(final View view, boolean animated) {
        view.clearAnimation();

        if (view.getVisibility() == View.GONE) return;

        if (!animated) {
            view.setVisibility(View.GONE);
        } else {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (animation == view.getAnimation()) {
                        view.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            view.startAnimation(animation);
        }
    }

    private TourLayout getCurrentTourLayout() {
        return mTourPages[mViewPager.getCurrentItem()];
    }

    private static boolean isForegroundView(View view) {
        Object tag = view.getTag();

        return "foreground".equals(tag) || "parallax".equals(tag);
    }

    protected void presentError(@Nullable String error) {
        if (!isFinishing()) {
            new AlertDialog.Builder(this)
                    .setTitle(error != null ? R.string.authentication_signup_failure_title :  R.string.authentication_signup_error_title)
                    .setMessage(error != null ? error : getString(R.string.authentication_signup_error_message))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    public void onFacebookLogin() {
        SoundCloudApplication app = (SoundCloudApplication) getApplication();

        app.track(Click.Login_with_facebook);
        startActivityForResult(new Intent(this, Facebook.class), 0);
    }

    @Override
    public void onTermsOfUse() {
        SoundCloudApplication app = (SoundCloudApplication) getApplication();

        app.track(Click.Signup_Signup_terms);
        startActivity(new Intent(Intent.ACTION_VIEW, TERMS_OF_USE_URL));
    }

    @Override
    public void onRecover(String email) {
        Intent recoveryIntent = new Intent(this, Recover.class);

        if (email != null && email.length() > 0) {
            recoveryIntent.putExtra("email", email);
        }

        startActivityForResult(recoveryIntent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Consts.RequestCodes.GALLERY_IMAGE_PICK:
                if (getUserDetails() != null) {
                    getUserDetails().onImagePick(resultCode, result);
                }
                break;

            case Consts.RequestCodes.GALLERY_IMAGE_TAKE:
                if (getUserDetails() != null) {
                    getUserDetails().onImageTake(resultCode, result);
                }
                break;

            case Consts.RequestCodes.IMAGE_CROP: {
                if (getUserDetails() != null) {
                    getUserDetails().onImageCrop(resultCode, result);
                }
                break;
            }
        }
    }

}
