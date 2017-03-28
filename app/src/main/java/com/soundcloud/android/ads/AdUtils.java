package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.ui.view.RoundedColorButton;
import com.soundcloud.java.functions.Predicate;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;

public final class AdUtils {

    private AdUtils() {}

    public static final Predicate<PlayQueueItem> IS_PLAYER_AD_ITEM = PlayQueueItem::isAd;

    public static final Predicate<PlayQueueItem> IS_NOT_AD = input -> !input.isAd();

    public static boolean isAd(PlaybackItem playbackItem) {
        return playbackItem.getPlaybackType() == PlaybackType.AUDIO_AD
                || playbackItem.getPlaybackType() == PlaybackType.VIDEO_AD
                || playbackItem.getUrn().isAd();
    }

    public static boolean hasAdOverlay(PlayQueueItem playQueueItem) {
        return playQueueItem.getAdData().isPresent()
                && playQueueItem.getAdData().get() instanceof OverlayAdData;
    }

    private static ColorStateList getColorStates(String focusedColor, String pressedColor, String defaultColor) {
        return new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_pressed},
                new int[]{},
        }, new int[]{Color.parseColor(focusedColor), Color.parseColor(pressedColor), Color.parseColor(defaultColor)});
    }

    static void setupCallToActionButton(PlayableAdData adData, Resources resources, RoundedColorButton button) {
        final String defaultText = resources.getString(R.string.ads_call_to_action);
        button.setText(adData.getCallToActionButtonText().or(defaultText));
        adData.getDisplayProperties().ifPresent(properties ->  {
            button.setTextColor(getColorStates(
                    properties.getFocusedTextColor(),
                    properties.getPressedTextColor(),
                    properties.getDefaultTextColor()
            ));
            button.setBackground(getColorStates(
                    properties.getFocusedBackgroundColor(),
                    properties.getPressedBackgroundColor(),
                    properties.getDefaultBackgroundColor()
            ));
        });
    }
}
