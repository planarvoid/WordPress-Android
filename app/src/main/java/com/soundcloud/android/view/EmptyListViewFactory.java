package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * A builder class to create {@link EmptyListView}s. Note that it is essential that instances of this class must be
 * retainable across configuration changes, so do NOT hold strong references to views or Context in here!
 */
public class EmptyListViewFactory {

    private int mImage;
    private String mMessageText, mActionText, mSecondaryText;
    private Intent mPrimaryAction, mSecondaryAction;
    private boolean mImageClickable;

    public EmptyListView build(Activity context) {
        EmptyListView view = new EmptyListView(context);
        if (mMessageText != null) view.setMessageText(mMessageText);
        if (mActionText != null) view.setActionText(mActionText);
        if (mSecondaryText != null) view.setSecondaryText(mSecondaryText);
        if (mImage > 0) view.setImage(mImage);
        view.setButtonActions(mPrimaryAction, mSecondaryAction);
        if (mImageClickable) {
            view.setImageActions(mPrimaryAction, mSecondaryAction);
        }
        return view;
    }

    public EmptyListViewFactory forContent(final Context context, final Uri contentUri, @Nullable final User user) {

        switch (Content.match(contentUri)) {
            case ME_SOUND_STREAM:
                mImage = R.drawable.empty_follow;
                if (Consts.StringValues.ERROR.equals(contentUri.getQueryParameter(Consts.Keys.ONBOARDING))){
                    mMessageText = context.getString(R.string.error_onboarding_fail);
                } else {
                    mMessageText = context.getString(R.string.list_empty_stream_message);
                    mSecondaryText = context.getString(R.string.list_empty_stream_secondary);
                    mActionText = context.getString(R.string.list_empty_stream_action);
                    mPrimaryAction = new Intent(Actions.WHO_TO_FOLLOW);
                    mSecondaryAction = new Intent(Actions.FRIEND_FINDER);
                }
                break;

            case ME_ACTIVITIES:
                if (showRecordingTeaser(context)) {
                    mMessageText = context.getString(R.string.list_empty_activity_nosounds_message);
                    mSecondaryText = context.getString(R.string.list_empty_activity_nosounds_secondary);
                    mActionText = context.getString(R.string.list_empty_activity_nosounds_action);
                    mImage = R.drawable.empty_rec;
                    mImageClickable = true;
                    mPrimaryAction = new Intent(Actions.RECORD);
                } else {
                    mMessageText = context.getString(R.string.list_empty_activity_message);
                    mSecondaryText = context.getString(R.string.list_empty_activity_secondary);
                    mActionText = context.getString(R.string.list_empty_activity_action);
                    mImage = R.drawable.empty_share;
                    mPrimaryAction = new Intent(Actions.YOUR_SOUNDS);
                }
                mSecondaryAction = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101"));
                break;

            // user browser specific
            case ME_SOUNDS:
                mMessageText = context.getString(R.string.list_empty_user_sounds_message);
                mActionText = context.getString(R.string.list_empty_user_sounds_action);
                mImage = R.drawable.empty_rec;
                mPrimaryAction = new Intent(Actions.RECORD);
                break;

            case USER_SOUNDS:
                mMessageText = getTextForUser(context, R.string.empty_user_tracks_text, user);
                break;

            case ME_PLAYLISTS:
                mMessageText = context.getString(R.string.list_empty_you_sets_message);
                break;

            case USER_PLAYLISTS:
                mMessageText = getTextForUser(context, R.string.list_empty_user_sets_message, user);
                break;

            case ME_LIKES:
                mMessageText = context.getString(R.string.list_empty_user_likes_message);
                mActionText = context.getString(R.string.list_empty_user_likes_action);
                mImage = R.drawable.empty_like;
                mPrimaryAction = new Intent(Actions.WHO_TO_FOLLOW);
                mSecondaryAction = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101"));
                break;

            case USER_LIKES:
                mMessageText = getTextForUser(context, R.string.empty_user_likes_text, user);
                break;

            case ME_FOLLOWERS:
                if (showRecordingTeaser(context)) {
                    mMessageText = context.getString(R.string.list_empty_user_followers_nosounds_message);
                    mActionText = context.getString(R.string.list_empty_user_followers_nosounds_action);
                    mImage = R.drawable.empty_share;
                    mPrimaryAction = new Intent(Actions.RECORD);
                } else {
                    mMessageText = context.getString(R.string.list_empty_user_followers_message);
                    mActionText = context.getString(R.string.list_empty_user_followers_action);
                    mImage = R.drawable.empty_rec;
                    mPrimaryAction = new Intent(Actions.YOUR_SOUNDS);
                }
                break;

            case USER_FOLLOWERS:
                mMessageText = getTextForUser(context, R.string.empty_user_followers_text, user);
                break;

            case ME_FOLLOWINGS:
                mMessageText = context.getString(R.string.list_empty_user_following_message);
                mActionText = context.getString(R.string.list_empty_user_following_action);
                mImage = R.drawable.empty_follow_3row;
                mPrimaryAction = new Intent(Actions.WHO_TO_FOLLOW);
                break;

            case USER_FOLLOWINGS:
                mMessageText = getTextForUser(context, R.string.empty_user_followings_text, user);
                break;
        }

        return this;
    }

    public EmptyListViewFactory withMessageText(@Nullable String messageText) {
        mMessageText = messageText;
        return this;
    }

    public EmptyListViewFactory withActionText(@Nullable String actionText) {
        mActionText = actionText;
        return this;
    }

    public EmptyListViewFactory withImage(int imageId) {
        mImage = imageId;
        return this;
    }

    public EmptyListViewFactory withPrimaryAction(Intent action) {
        mPrimaryAction = action;
        return this;
    }

    private String getTextForUser(Context context, int userBasedText, @Nullable User user) {
        return context.getString(userBasedText,
                user == null || user.username == null ? context.getString(R.string.this_user)
                        : user.username);
    }

    private boolean showRecordingTeaser(Context context) {
        User loggedInUser = SoundCloudApplication.fromContext(context).getLoggedInUser();
        return loggedInUser != null && loggedInUser.track_count <= 0;
    }
}
