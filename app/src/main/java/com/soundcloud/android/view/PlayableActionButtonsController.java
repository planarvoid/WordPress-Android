package com.soundcloud.android.view;

import static com.soundcloud.android.service.playback.CloudPlaybackService.*;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.ToggleButton;

public class PlayableActionButtonsController {

    private View mRootView;

    private ToggleButton mToggleLike;
    private ToggleButton mToggleRepost;
    private ImageButton mShareButton;

    private Playable mPlayable;

    public PlayableActionButtonsController(View rootView) {
        mRootView = rootView;

        mToggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        mToggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        mShareButton = (ImageButton) rootView.findViewById(R.id.btn_share);

        mToggleLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String action = mToggleLike.isChecked() ? ADD_LIKE_ACTION : REMOVE_LIKE_ACTION;
                Intent intent = new Intent(action);
                intent.setData(mPlayable.toUri());
                view.getContext().startService(intent);
            }
        });

        mToggleRepost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String action = mToggleRepost.isChecked() ? ADD_REPOST_ACTION : REMOVE_REPOST_ACTION;
                Intent intent = new Intent(action);
                intent.setData(mPlayable.toUri());
                view.getContext().startService(intent);
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
        if (AndroidUtils.accessibilityFeaturesAvailable(context)) {
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

    String labelForCount(int count) {
        if (count < 0) {
            return "\u2014";
        } else if (count == 0) {
            return "";
        } else if (count >= 1000) {
            return "1k+";
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
