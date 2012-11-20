package com.soundcloud.android.activity.auth;

import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.tour.TourLayout;
import com.soundcloud.api.Token;
import net.hockeyapp.android.UpdateManager;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public class Start extends AccountAuthenticatorActivity {
    private static final int RECOVER_CODE    = 1;
    private static final int SUGGESTED_USERS = 2;

    public static final String FB_CONNECTED_EXTRA    = "facebook_connected";
    public static final String TOUR_BACKGROUND_EXTRA = "tour_background";

    private ViewPager mViewPager;
    private View[] mViews;
    private int[]  mBackgroundIds;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.start);

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        mViewPager = (ViewPager) findViewById(R.id.tour_view);
        mViews = new View[]{
            new TourLayout(this, R.layout.tour_page_1),
            new TourLayout(this, R.layout.tour_page_2),
            new TourLayout(this, R.layout.tour_page_3)
        };

        mBackgroundIds = new int[]{
            R.drawable.tour_image_1,
            R.drawable.tour_image_2,
        };

        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mViews.length;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View v = mViews[position];
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
                app.track(Click.Login_with_facebook);
                startActivityForResult(new Intent(Start.this, Facebook.class), 0);
            }
        });

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Login);

                Intent loginIntent = new Intent(Start.this, Login.class);
                loginIntent.putExtra(TOUR_BACKGROUND_EXTRA, getBackgroundId());

                startActivityForResult(loginIntent, 0);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup);

                Intent signUpIntent = new Intent(Start.this, SignUp.class);
                signUpIntent.putExtra(TOUR_BACKGROUND_EXTRA, getBackgroundId());

                startActivityForResult(signUpIntent, 0);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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

    public int getBackgroundId() {
        int i = mViewPager.getCurrentItem();

        return mBackgroundIds[i];
    }
}
