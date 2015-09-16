package com.soundcloud.android.onboarding.auth;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.util.AttributeSet;

public class SignupMethodLayout extends AuthLayout {
    @NotNull private SignUpMethodHandler handler; // null at creation, but must be populated before using

    public SignupMethodLayout(Context context) {
        super(context);
    }

    public SignupMethodLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SignupMethodLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    @Override
    protected SignUpMethodHandler getAuthHandler() {
        return handler;
    }

    public void setSignUpMethodHandler(@NotNull SignUpMethodHandler handler) {
        this.handler = handler;
    }

    @OnClick(R.id.signup_with_email)
    void onSignUpWithEmailClicked() {
        handler.onEmailAuth();
    }

    public interface SignUpMethodHandler extends AuthHandler {
        void onEmailAuth();
    }
}
