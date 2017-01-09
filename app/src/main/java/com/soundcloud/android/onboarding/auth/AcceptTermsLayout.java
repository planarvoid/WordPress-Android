package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AcceptTermsLayout extends RelativeLayout {

    private static final String BUNDLE_SIGNUP_VIA = "BUNDLE_TERMS_SIGNUP_VIA";
    private static final String BUNDLE_SIGNUP_PARAMS = "BUNDLE_TERMS_SIGNUP_PARAMS";

    private AcceptTermsHandler acceptTermsHandler;
    private SignupVia signupVia;
    private Bundle signupParams;

    public interface AcceptTermsHandler {
        void onAcceptTerms(SignupVia signupVia, Bundle signupParams);

        void onShowTermsOfUse();

        void onShowPrivacyPolicy();

        void onRejectTerms();
    }

    public AcceptTermsLayout(Context context) {
        super(context);
    }

    public AcceptTermsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AcceptTermsLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AcceptTermsHandler getAcceptTermsHandler() {
        return acceptTermsHandler;
    }

    public void setAcceptTermsHandler(AcceptTermsHandler acceptTermsHandler) {
        this.acceptTermsHandler = acceptTermsHandler;
    }

    public void setSignupParams(SignupVia signupVia, Bundle signupParams) {
        this.signupVia = signupVia;
        this.signupParams = signupParams;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViewById(R.id.btn_cancel).setOnClickListener(v -> getAcceptTermsHandler().onRejectTerms());

        findViewById(R.id.btn_accept_terms).setOnClickListener(v -> getAcceptTermsHandler().onAcceptTerms(signupVia, signupParams));

        ScTextUtils.clickify(((TextView) findViewById(android.R.id.message)),
                             getResources().getString(R.string.terms_of_use),
                             () -> getAcceptTermsHandler().onShowTermsOfUse(), false, false);

        ScTextUtils.clickify(((TextView) findViewById(android.R.id.message)),
                             getResources().getString(R.string.privacy_policy),
                             () -> getAcceptTermsHandler().onShowPrivacyPolicy(), false, false);

    }

    public Bundle getStateBundle() {
        Bundle bundle = new Bundle();
        bundle.putBundle(BUNDLE_SIGNUP_PARAMS, signupParams);
        bundle.putCharSequence(BUNDLE_SIGNUP_VIA, signupVia.name);
        return bundle;
    }

    public void setState(@Nullable Bundle bundle) {
        if (bundle == null) {
            return;
        }
        signupVia = SignupVia.fromString(bundle.getString(BUNDLE_SIGNUP_VIA));
        signupParams = bundle.getBundle(BUNDLE_SIGNUP_PARAMS);
    }
}
