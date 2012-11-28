package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.RelativeLayout;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.AddUserInfoTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

@Tracking(page = Page.Entry_signup__details)
public class SignUpDetails extends RelativeLayout {

    public interface SignUpDetailsHandler {
        void onSubmitDetails(String username, File avatarFile);
        void onSkipDetails();
    }

    public SignUpDetails(Context context) {
        super(context);
    }

    public SignUpDetails(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SignUpDetails(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Nullable private SignUpDetailsHandler mSignUpDetailsHandler;
    @Nullable private File mAvatarFile;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();
        final SoundCloudApplication app = SoundCloudApplication.fromContext(context);

        final TextView  username   = (TextView)  findViewById(R.id.txt_username);
        final TextView  avatarText = (TextView)  findViewById(R.id.txt_artwork_bg);
        final ImageView avatarView = (ImageView) findViewById(R.id.artwork);
        final Button    skipButton = (Button)    findViewById(R.id.btn_skip);
        final Button    saveButton = (Button)    findViewById(R.id.btn_save);

        username.setHint(R.string.authentication_add_info_username_hint);

        username.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean done = actionId == EditorInfo.IME_ACTION_DONE;
                boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean downAction = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

                if (done || pressedEnter && downAction) {
                    return mAvatarFile == null && avatarText.performClick();
                } else {
                    return false;
                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_details_skip);

                if (getSignUpDetailsHandler() != null) {
                    getSignUpDetailsHandler().onSkipDetails();
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_details_next);

                if (getSignUpDetailsHandler() != null) {
                    getSignUpDetailsHandler().onSubmitDetails(username.getText().toString(), mAvatarFile);
                }
            }
        });

        avatarText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



            }
        });

        avatarView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mAvatarFile = null;
                avatarView.setVisibility(View.GONE);
                avatarText.setVisibility(View.VISIBLE);

                return true;
            }
        });
    }

    private void setImage(final File file) {
        if (file != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setImage("+file+")");

            final TextView  avatarText = (TextView)  findViewById(R.id.txt_artwork_bg);
            final ImageView avatarView = (ImageView) findViewById(R.id.artwork);

            mAvatarFile = file;
            ImageUtils.setImage(
                file,
                avatarView,
                (int) (getResources().getDisplayMetrics().density * 100f),
                (int) (getResources().getDisplayMetrics().density * 100f)
            );

            avatarText.setVisibility(View.GONE);
            avatarView.setVisibility(View.VISIBLE);
        }
    }

    private File createTempAvatarFile()  {
        try {
            return File.createTempFile(Long.toString(System.currentTimeMillis()), ".bmp", IOUtils.getCacheDir(getContext()));
        } catch (IOException e) {
            Log.w(TAG, "error creating avatar temp file", e);
            return null;
        }
    }

    @Nullable
    public SignUpDetailsHandler getSignUpDetailsHandler() {
        return mSignUpDetailsHandler;
    }

    public void setSignUpDetailsHandler(@Nullable SignUpDetailsHandler mSignUpDetailsHandler) {
        this.mSignUpDetailsHandler = mSignUpDetailsHandler;
    }
}
