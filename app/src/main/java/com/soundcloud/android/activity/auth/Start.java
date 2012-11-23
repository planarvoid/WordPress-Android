package com.soundcloud.android.activity.auth;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.soundcloud.android.*;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.SignupTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.tour.TourLayout;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;
import net.hockeyapp.android.UpdateManager;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public class Start extends AccountAuthenticatorActivity implements Login.LoginHandler, SignUp.SignUpHandler {
    private static final int RECOVER_CODE    = 1;
    private static final int SUGGESTED_USERS = 2;

    private static final File SIGNUP_LOG = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".dr");

    public static final String FB_CONNECTED_EXTRA    = "facebook_connected";
    public static final String TOUR_BACKGROUND_EXTRA = "tour_background";

    private static final Uri TERMS_OF_USE_URL = Uri.parse("http://m.soundcloud.com/terms-of-use");

    public static final int THROTTLE_WINDOW = 60 * 60 * 1000;
    public static final int THROTTLE_AFTER_ATTEMPT = 3;

    private ViewPager mViewPager;
    private TourLayout[] mTourPages;

    @Nullable private Login  mLogin;
    @Nullable private SignUp mSignUp;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.start);

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

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

                getLogin().setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup);

                if (shouldThrottleSignup(Start.this)) {

                } else {
                    getSignUp().setVisibility(View.VISIBLE);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                handleActivityResult(requestCode, data);
                break;
        }
    }

    private void handleActivityResult(int requestCode, Intent data) {
        switch (requestCode) {
            case 0:
                SoundCloudApplication app = (SoundCloudApplication) getApplication();
                final String error = data.getStringExtra("error");
                if (error == null) {
                    final User user = data.getParcelableExtra("user");
                    final Token token = (Token) data.getSerializableExtra("token");

                    SignupVia via = SignupVia.fromIntent(data);
                    // API signup will already have created the account
                    if (SignupVia.API == via || app.addUserAccount(user, token, via)) {
                        final Bundle result = new Bundle();
                        result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
                        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
                        setAccountAuthenticatorResult(result);

                        sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                                .putExtra("user", user)
                                .putExtra("signed_up", via.name));

                        if (via != SignupVia.UNKNOWN) {
                            startActivityForResult(
                                new Intent(this, SuggestedUsers.class).putExtra(FB_CONNECTED_EXTRA, via.isFacebook()),
                                    SUGGESTED_USERS);
                        } else {
                            finish();
                        }
                    } else {
                        AndroidUtils.showToast(this, R.string.error_creating_account);
                    }
                } else {
                    AndroidUtils.showToast(this, error);
                }
                break;
            case SUGGESTED_USERS:
                handleSuggestedUsersReturned(data);
                break;

            case RECOVER_CODE:
                handleRecoverResult(this, data);
                break;
        }
    }

    private void handleSuggestedUsersReturned(Intent data) {
        finish();
    }

    static void handleRecoverResult(Context context, Intent data) {
        final boolean success = data.getBooleanExtra("success", false);
        if (success) {
            AndroidUtils.showToast(context, R.string.authentication_recover_password_success);
        } else {
            String error = data.getStringExtra("error");
            AndroidUtils.showToast(context,
                    error == null ?
                            context.getString(R.string.authentication_recover_password_failure) :
                            context.getString(R.string.authentication_recover_password_failure_reason, error));
        }
    }

    public Login getLogin() {
        if (mLogin == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.login_stub);

            mLogin = (Login) stub.inflate();
            mLogin.setLoginHandler(this);
        }

        return mLogin;
    }

    public View getSignUp() {
        if (mSignUp == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.sign_up_stub);

            mSignUp = (SignUp) stub.inflate();
            mSignUp.setSignUpHandler(this);
        }

        return mSignUp;
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
    }

    @Override
    public void onSignUp(final String email, final String password) {
        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

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

                    final Bundle param = new Bundle();
                    param.putString("username", email);
                    param.putString("password", password);
                    new GetTokensTask(mApi) {
                        @Override protected void onPostExecute(Token token) {
                            if (token != null) {
                                startActivityForResult(new Intent(Start.this, SignupDetails.class)
                                        .putExtra(SignupVia.EXTRA, signedUp ? SignupVia.API.name : null)
                                        .putExtra("user", user)
                                        .putExtra("token", token), 0);
                            } else {
                                signupFail(null);
                            }
                        }
                    }.execute(param);
                } else {
                    signupFail(getFirstError());
                }
            }
        }.execute(email, password);
    }

    protected void signupFail(@Nullable String error) {
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

    @Override
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
}
