package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.model.User;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * A builder class to build or configure {@link EmptyListView}s. Note that it is essential that instances of this class must be
 * retainable across configuration changes, so do NOT hold strong references to views or Context in here!
 */
public class EmptyViewBuilder {

    private int mImage;
    private String mMessageText, mActionText, mSecondaryText;
    private Intent mAction;

    public EmptyListView build(Context context) {
        EmptyListView view = new EmptyListView(context);
        if (mMessageText != null) view.setMessageText(mMessageText);
        if (mActionText != null) view.setActionText(mActionText);
        if (mSecondaryText != null) view.setSecondaryText(mSecondaryText);
        if (mImage > 0) view.setImage(mImage);
        view.setButtonActions(mAction);
        return view;
    }

    public EmptyViewBuilder forContent(final Context context, final Uri contentUri, @Nullable final User user) {

        switch (Content.match(contentUri)) {
            case ME_SOUND_STREAM:
                mImage = R.drawable.empty_stream;
                if (Consts.StringValues.ERROR.equals(contentUri.getQueryParameter(Consts.Keys.ONBOARDING))) {
                    mMessageText = context.getString(R.string.error_onboarding_fail);
                } else {
                    mMessageText = context.getString(R.string.list_empty_stream_message);
                    mActionText = context.getString(R.string.list_empty_stream_action);
                    mAction = new Intent(Actions.WHO_TO_FOLLOW);
                }
                break;

            case ME_ACTIVITIES:
                mImage = R.drawable.empty_activity;
                mMessageText = context.getString(R.string.list_empty_activity_message);
                mSecondaryText = context.getString(R.string.list_empty_activity_secondary);
                break;

            // user browser specific
            case ME_SOUNDS:
                mImage = R.drawable.empty_sounds;
                mMessageText = context.getString(R.string.list_empty_user_sounds_message);
                break;

            case USER_SOUNDS:
                mImage = R.drawable.empty_sounds;
                mMessageText = getTextForUser(context, R.string.empty_user_tracks_text, user);
                break;

            case ME_PLAYLISTS:
                mImage = R.drawable.empty_playlists;
                mMessageText = context.getString(R.string.list_empty_you_playlists_message);
                break;

            case USER_PLAYLISTS:
                mImage = R.drawable.empty_playlists;
                mMessageText = getTextForUser(context, R.string.list_empty_user_playlists_message, user);
                break;

            case ME_LIKES:
                mMessageText = context.getString(R.string.list_empty_user_likes_message);
                mImage = R.drawable.empty_like;
                break;

            case USER_LIKES:
                mImage = R.drawable.empty_like;
                mMessageText = getTextForUser(context, R.string.empty_user_likes_text, user);
                break;

            case ME_FOLLOWERS:
                mImage = R.drawable.empty_followers;
                mSecondaryText = context.getString(R.string.list_empty_user_followers_secondary);
                mMessageText = context.getString(R.string.list_empty_user_followers_message);
                break;

            case USER_FOLLOWERS:
                mImage = R.drawable.empty_followers;
                mMessageText = getTextForUser(context, R.string.empty_user_followers_text, user);
                break;

            case ME_FOLLOWINGS:
                mImage = R.drawable.empty_following;
                mMessageText = context.getString(R.string.list_empty_user_following_message);
                mActionText = context.getString(R.string.list_empty_user_following_action);
                mAction = new Intent(Actions.WHO_TO_FOLLOW);
                break;

            case USER_FOLLOWINGS:
                mImage = R.drawable.empty_following;
                mMessageText = getTextForUser(context, R.string.empty_user_followings_text, user);
                break;
        }

        return this;
    }

    public EmptyViewBuilder withMessageText(@Nullable String messageText) {
        mMessageText = messageText;
        return this;
    }

    public EmptyViewBuilder withSecondaryText(@Nullable String secondaryText) {
        mSecondaryText = secondaryText;
        return this;
    }

    public EmptyViewBuilder withImage(int imageResourceId) {
        mImage = imageResourceId;
        return this;
    }

    public void configureForSearch(EmptyListView emptyListView) {
        emptyListView.setImage(R.drawable.empty_search);
        emptyListView.setMessageText(R.string.search_empty);
        emptyListView.setSecondaryText(R.string.search_empty_subtext);
    }

    private String getTextForUser(Context context, int userBasedText, @Nullable User user) {
        return context.getString(userBasedText,
                user == null || user.username == null ? context.getString(R.string.this_user)
                        : user.username
        );
    }
}
