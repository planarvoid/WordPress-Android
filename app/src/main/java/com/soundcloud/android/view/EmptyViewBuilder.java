package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * A builder class to build or configure {@link EmptyView}s. Note that it is essential that instances of this class must be
 * retainable across configuration changes, so do NOT hold strong references to views or Context in here!
 */
public class EmptyViewBuilder {

    private int image;
    private String messageText, actionText, secondaryText;
    private Intent action;

    public EmptyView build(Context context) {
        EmptyView view = new EmptyView(context);
        if (messageText != null) {
            view.setMessageText(messageText);
        }
        if (actionText != null) {
            view.setActionText(actionText);
        }
        if (secondaryText != null) {
            view.setSecondaryText(secondaryText);
        }
        if (image > 0) {
            view.setImage(image);
        }
        view.setButtonActions(action);
        return view;
    }

    // we can remove this method once we lose ScListFragment
    @Deprecated
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public EmptyViewBuilder forContent(final Context context, final Uri contentUri, @Nullable final String username) {

        switch (Content.match(contentUri)) {

            case ME_ACTIVITIES:
                image = R.drawable.empty_activity;
                messageText = context.getString(R.string.list_empty_activity_message);
                secondaryText = context.getString(R.string.list_empty_activity_secondary);
                break;

            // profile specific
            case ME_SOUNDS:
                image = R.drawable.empty_sounds;
                messageText = context.getString(R.string.list_empty_user_sounds_message);
                break;

            case USER_SOUNDS:
                image = R.drawable.empty_sounds;
                messageText = getTextForUser(context, R.string.empty_user_tracks_text, username);
                break;

            case ME_PLAYLISTS:
                image = R.drawable.empty_playlists;
                messageText = context.getString(R.string.list_empty_you_playlists_message);
                break;

            case USER_PLAYLISTS:
                image = R.drawable.empty_playlists;
                messageText = getTextForUser(context, R.string.list_empty_user_playlists_message, username);
                break;

            case ME_LIKES:
                messageText = context.getString(R.string.list_empty_user_likes_message);
                image = R.drawable.empty_like;
                break;

            case USER_LIKES:
                image = R.drawable.empty_like;
                messageText = getTextForUser(context, R.string.empty_user_likes_text, username);
                break;

            case ME_FOLLOWERS:
                image = R.drawable.empty_followers;
                secondaryText = context.getString(R.string.list_empty_user_followers_secondary);
                messageText = context.getString(R.string.list_empty_user_followers_message);
                break;

            case USER_FOLLOWERS:
                image = R.drawable.empty_followers;
                messageText = getTextForUser(context, R.string.empty_user_followers_text, username);
                break;

            case ME_FOLLOWINGS:
                image = R.drawable.empty_following;
                messageText = context.getString(R.string.list_empty_user_following_message);
                actionText = context.getString(R.string.list_empty_user_following_action);
                action = new Intent(Actions.WHO_TO_FOLLOW);
                break;

            case USER_FOLLOWINGS:
                image = R.drawable.empty_following;
                messageText = getTextForUser(context, R.string.empty_user_followings_text, username);
                break;

            default:
                break;
        }

        return this;
    }

    public EmptyViewBuilder withMessageText(@Nullable String messageText) {
        this.messageText = messageText;
        return this;
    }

    public EmptyViewBuilder withSecondaryText(@Nullable String secondaryText) {
        this.secondaryText = secondaryText;
        return this;
    }

    public EmptyViewBuilder withImage(int imageResourceId) {
        image = imageResourceId;
        return this;
    }

    public void configureForSearch(EmptyView emptyView) {
        emptyView.setImage(R.drawable.empty_search);
        emptyView.setMessageText(R.string.search_empty);
        emptyView.setSecondaryText(R.string.search_empty_subtext);
    }

    private String getTextForUser(Context context, int userBasedText, @Nullable String username) {
        return context.getString(userBasedText,
                ScTextUtils.isBlank(username) ? context.getString(R.string.this_user) : username
        );
    }
}
