package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Playable;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.ToggleButton;

public class PlayableActionButtonsController {

    public interface PlayableActions {

        boolean toggleLike(Playable playable);
        boolean toggleRepost(Playable playable);
    }

    private View mRootView;

    private ToggleButton mToggleLike;
    private ToggleButton mToggleRepost;
    private ImageButton mShareButton;

    private Playable mPlayable;
    private PlayableActions mDelegate;

    public PlayableActionButtonsController(View rootView, PlayableActions delegate) {
        mRootView = rootView;
        mDelegate = delegate;

        mToggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        mToggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        mShareButton = (ImageButton) rootView.findViewById(R.id.btn_share);

        mToggleLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDelegate.toggleLike(mPlayable);
            }
        });

        mToggleRepost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDelegate.toggleRepost(mPlayable);
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = mPlayable.getShareIntent();
                if (shareIntent != null) {
                    mRootView.getContext().startActivity(shareIntent);
                }
            }
        });
    }

    public void update(@NotNull Playable playable) {
        mPlayable = playable;
        setLikes(mPlayable.likes_count, mPlayable.user_like);
        setReposts(mPlayable.reposts_count, mPlayable.user_repost);

        mShareButton.setVisibility(mPlayable.isPublic() ? View.VISIBLE : View.GONE);
    }

    public void update(ToggleButton button, int actionStringID, int descriptionPluralID, int count, boolean checked, int checkedStringId) {
        button.setTextOn(labelForCount(count));
        button.setTextOff(labelForCount(count));
        button.setChecked(checked);
        button.invalidate();

        Context context = mRootView.getContext();
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append(context.getResources().getString(actionStringID));

            if (count >= 0) {
                builder.append(", ");
                builder.append(context.getResources().getQuantityString(descriptionPluralID, count, count));
            }

            if (checked) {
                builder.append(", ");
                builder.append(context.getResources().getString(checkedStringId));
            }

            button.setContentDescription(builder.toString());
        }
    }

    private String labelForCount(int count) {
        if (count < 0) {
            return "\u2014";
        } else if (count == 0) {
            return "";
        } else {
            return String.valueOf(count);
        }
    }

    private void setLikes(int count, boolean userLiked) {
        update(mToggleLike,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                count,
                userLiked,
                R.string.accessibility_stats_user_liked);
    }

    private void setReposts(int count, boolean userReposted) {
        update(mToggleRepost,
                R.string.accessibility_repost_action,
                R.plurals.accessibility_stats_reposts,
                count,
                userReposted,
                R.string.accessibility_stats_user_reposted);
    }

}
