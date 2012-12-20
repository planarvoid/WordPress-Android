package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.SoundCloudApplication.handleSilentException;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.*;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

@Tracking(page = Page.Entry_signup__details)
public class UserDetails extends RelativeLayout {
    private static final String BUNDLE_USERNAME = "BUNDLE_USERNAME";
    private static final String BUNDLE_FILE     = "BUNDLE_FILE";

    public interface UserDetailsHandler {
        void onSubmitDetails(String username, File avatarFile);
        void onSkipDetails();
    }
    public UserDetails(Context context) {
        super(context);
    }

    public UserDetails(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserDetails(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Nullable private UserDetailsHandler mUserDetailsHandler;

    @Nullable private File mAvatarFile;
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();
        final SoundCloudApplication app = SoundCloudApplication.fromContext(context);

        final EditText  username   = (EditText)  findViewById(R.id.txt_username);
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

                if (getUserDetailsHandler() != null) {
                    getUserDetailsHandler().onSkipDetails();
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_details_next);

                if (getUserDetailsHandler() != null) {
                    getUserDetailsHandler().onSubmitDetails(username.getText().toString(), mAvatarFile);
                }
            }
        });

        avatarText.setOnClickListener(new ImageUtils.ImagePickListener((Activity) context) {
            @Override
            protected File getFile() {
                mAvatarFile = createTempAvatarFile();
                return mAvatarFile;
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

    public void onImagePick(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            mAvatarFile = createTempAvatarFile();
            ImageUtils.sendCropIntent((Activity) getContext(), result.getData(), Uri.fromFile(mAvatarFile));
        }
    }

    public void onImageTake(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            ImageUtils.sendCropIntent((Activity) getContext(), Uri.fromFile(mAvatarFile));
        }
    }

    public void onImageCrop(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {

            if (result.getExtras().containsKey("error")) {
                handleSilentException("error cropping image", (Exception) result.getSerializableExtra("error"));
                Toast.makeText(getContext(),R.string.crop_image_error, Toast.LENGTH_SHORT).show();
            } else {
                setImage(mAvatarFile);
            }
        }
    }

    @Nullable
    public UserDetailsHandler getUserDetailsHandler() {
        return mUserDetailsHandler;
    }

    public void setUserDetailsHandler(@Nullable UserDetailsHandler mUserDetailsHandler) {
        this.mUserDetailsHandler = mUserDetailsHandler;
    }

    public Bundle getStateBundle() {
        EditText username = (EditText) findViewById(R.id.txt_username);

        Bundle bundle = new Bundle();
        bundle.putCharSequence(BUNDLE_USERNAME, username.getText());
        bundle.putSerializable(BUNDLE_FILE, mAvatarFile);
        return bundle;
    }

    public void setState(@Nullable Bundle bundle) {
        if (bundle == null) return;

        EditText username = (EditText) findViewById(R.id.txt_username);

        username.setText(bundle.getCharSequence(BUNDLE_USERNAME));
        setImage((File) bundle.getSerializable(BUNDLE_FILE));
    }
}
