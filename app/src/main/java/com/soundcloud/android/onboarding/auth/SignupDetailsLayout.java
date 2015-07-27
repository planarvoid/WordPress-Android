package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class SignupDetailsLayout extends RelativeLayout {
    private static final String BUNDLE_USERNAME = "BUNDLE_USERNAME";
    private static final String BUNDLE_FILE = "BUNDLE_FILE";
    @Nullable private UserDetailsHandler userDetailsHandler;
    @Nullable private File avatarFile;

    public SignupDetailsLayout(Context context) {
        super(context);
    }

    public SignupDetailsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SignupDetailsLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setImage(final File file) {
        if (file != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "setImage(" + file + ")");
            }

            final TextView avatarText = (TextView) findViewById(R.id.txt_artwork_bg);
            final ImageView avatarView = (ImageView) findViewById(R.id.artwork);

            avatarFile = file;
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

    public File generateTempAvatarFile() {
        avatarFile = ImageUtils.createTempAvatarFile();
        avatarFile.setWritable(true, false);
        return avatarFile;
    }

    public void onImagePick(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            File tmpAvatar = ImageUtils.createTempAvatarFile();
            if (tmpAvatar != null) {
                avatarFile = tmpAvatar;
                ImageUtils.sendCropIntent((Activity) getContext(), result.getData(), Uri.fromFile(avatarFile));
            }
        }
    }

    public void onImageTake(int resultCode) {
        if (avatarFile != null) {
            if (resultCode == Activity.RESULT_OK) {
                ImageUtils.sendCropIntent((Activity) getContext(), Uri.fromFile(avatarFile));
            } else {
                resetAvatarFile();
            }
        }
    }

    public void onImageCrop(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            setImage(avatarFile);
        } else if (resultCode == Crop.RESULT_ERROR) {
            ErrorUtils.handleSilentException("error cropping image", Crop.getError(result));
            Toast.makeText(getContext(), R.string.crop_image_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    public UserDetailsHandler getUserDetailsHandler() {
        return userDetailsHandler;
    }

    public void setUserDetailsHandler(@Nullable UserDetailsHandler mUserDetailsHandler) {
        this.userDetailsHandler = mUserDetailsHandler;
    }

    public Bundle getStateBundle() {
        EditText username = (EditText) findViewById(R.id.txt_username);

        Bundle bundle = new Bundle();
        bundle.putCharSequence(BUNDLE_USERNAME, username.getText());
        bundle.putSerializable(BUNDLE_FILE, avatarFile);
        return bundle;
    }

    public void setState(@Nullable Bundle bundle) {
        if (bundle == null) {
            return;
        }

        EditText username = (EditText) findViewById(R.id.txt_username);

        username.setText(bundle.getCharSequence(BUNDLE_USERNAME));
        setImage((File) bundle.getSerializable(BUNDLE_FILE));
    }

    @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    protected void onFinishInflate() {
        super.onFinishInflate();

        final EditText username = (EditText) findViewById(R.id.txt_username);
        final TextView avatarText = (TextView) findViewById(R.id.txt_artwork_bg);
        final ImageView avatarView = (ImageView) findViewById(R.id.artwork);
        final Button skipButton = (Button) findViewById(R.id.btn_skip);
        final Button saveButton = (Button) findViewById(R.id.btn_save);

        username.setHint(R.string.authentication_add_info_username_hint);

        username.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean done = actionId == EditorInfo.IME_ACTION_DONE;
                boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean downAction = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

                if (done || pressedEnter && downAction) {
                    return avatarFile == null && avatarText.performClick();
                } else {
                    return false;
                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getUserDetailsHandler() != null) {
                    getUserDetailsHandler().onSkipUserDetails();
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getUserDetailsHandler() != null) {
                    getUserDetailsHandler().onSubmitUserDetails(username.getText().toString(), avatarFile);
                }
            }
        });

        avatarText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentActivity activity = userDetailsHandler.getFragmentActivity();
                ImageUtils.showImagePickerDialog(activity, generateTempAvatarFile());

            }
        });

        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), R.string.authentication_clear_image, Toast.LENGTH_LONG).show();
            }
        });

        avatarView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                resetAvatarFile();
                avatarView.setVisibility(View.GONE);
                avatarText.setVisibility(View.VISIBLE);

                return true;
            }
        });
    }

    private void resetAvatarFile() {
        avatarFile = null;
    }

    @VisibleForTesting
    void setAvatarTemporaryFile(File file) {
        avatarFile = file;
    }

    public interface UserDetailsHandler {
        void onSubmitUserDetails(String username, File avatarFile);

        void onSkipUserDetails();

        FragmentActivity getFragmentActivity();
    }
}
