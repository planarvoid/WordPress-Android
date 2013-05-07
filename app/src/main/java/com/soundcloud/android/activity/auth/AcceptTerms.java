package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AcceptTerms extends RelativeLayout {

    private static final String BUNDLE_SIGNUP_VIA    = "BUNDLE_TERMS_SIGNUP_VIA";
    private static final String BUNDLE_SIGNUP_PARAMS = "BUNDLE_TERMS_SIGNUP_PARAMS";

    private AcceptTermsHandler mAcceptTermsHandler;
    private SignupVia mSignupVia;
    private Bundle mSignupParams;

    public interface AcceptTermsHandler {
        void onAcceptTerms(SignupVia signupVia, Bundle signupParams);
        void onShowTermsOfUse();
        void onShowPrivacyPolicy();
        void onShowCookiePolicy();
        void onCancel();
    }

    public AcceptTerms(Context context) {
        super(context);
    }

    public AcceptTerms(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AcceptTerms(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AcceptTermsHandler getAcceptTermsHandler() {
        return mAcceptTermsHandler;
    }

    public void setAcceptTermsHandler(AcceptTermsHandler acceptTermsHandler) {
        mAcceptTermsHandler = acceptTermsHandler;
    }

    public void setSignupParams(SignupVia signupVia, Bundle signupParams){
        mSignupVia = signupVia;
        mSignupParams = signupParams;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();
        final SoundCloudApplication app = SoundCloudApplication.fromContext(context);

        findViewById(R.id.btn_cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getAcceptTermsHandler().onCancel();
            }
        });

        findViewById(R.id.btn_continue).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getAcceptTermsHandler().onAcceptTerms(mSignupVia, mSignupParams);
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(android.R.id.message)),
                getResources().getString(R.string.terms_of_use),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        getAcceptTermsHandler().onShowTermsOfUse();
                    }
                }, false, false);

        ScTextUtils.clickify(((TextView) findViewById(android.R.id.message)),
                getResources().getString(R.string.privacy),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        getAcceptTermsHandler().onShowPrivacyPolicy();
                    }
                }, false, false);

    }

    public Bundle getStateBundle() {
        Bundle bundle = new Bundle();
        bundle.putBundle(BUNDLE_SIGNUP_PARAMS, mSignupParams);
        bundle.putCharSequence(BUNDLE_SIGNUP_VIA, mSignupVia.name);
        return bundle;
    }

    public void setState(@Nullable Bundle bundle) {
        if (bundle == null) return;
        mSignupVia = SignupVia.fromString(bundle.getString(BUNDLE_SIGNUP_VIA));
        mSignupParams = bundle.getBundle(BUNDLE_SIGNUP_PARAMS);
    }
}
