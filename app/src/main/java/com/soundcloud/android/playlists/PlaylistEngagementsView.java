package com.soundcloud.android.playlists;

import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.widget.ToggleButton;

public abstract class PlaylistEngagementsView {

    private final Context context;
    private final Resources resources;

    private OnEngagementListener listener;

    protected PlaylistEngagementsView(Context context, Resources resources) {
        this.context = context;
        this.resources = resources;
    }

    public OnEngagementListener getListener() {
        return listener;
    }

    abstract void setInfoText(String message);

    abstract void onViewCreated(View view);

    abstract void onDestroyView();

    void setOnEngagement(OnEngagementListener listener){
        this.listener = listener;
    }

    abstract void showOfflineAvailability(boolean isAvailable);

    abstract void showUpsell();

    abstract void hideOfflineContentOptions();

    abstract void showPublicOptionsForYourTrack();

    abstract void showPublicOptions(int repostsCount, boolean repostedByUser);

    abstract void hidePublicOptions();

    abstract void updateLikeItem(int likesCount, boolean likedByUser);

    public interface OnEngagementListener {
        void onToggleLike(boolean isLiked);
        void onToggleRepost(boolean isReposted);
        void onShare();
        void onMakeOfflineAvailable(boolean isMarkedForOffline);
        void onUpsell();
    }

    protected void updateToggleButton(@Nullable ToggleButton button, int actionStringID, int descriptionPluralID, int count, boolean checked,
                                    int checkedStringId) {
        final String buttonLabel = ScTextUtils.shortenLargeNumber(count);
        button.setTextOn(buttonLabel);
        button.setTextOff(buttonLabel);
        button.setChecked(checked);
        button.invalidate();

        if (AndroidUtils.accessibilityFeaturesAvailable(context)
                && TextUtils.isEmpty(button.getContentDescription())) {
            final StringBuilder builder = new StringBuilder();
            builder.append(resources.getString(actionStringID));

            if (count >= 0) {
                builder.append(", ");
                builder.append(resources.getQuantityString(descriptionPluralID, count, count));
            }

            if (checked) {
                builder.append(", ");
                builder.append(resources.getString(checkedStringId));
            }

            button.setContentDescription(builder.toString());
        }
    }
}
