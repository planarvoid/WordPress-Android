package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.PlayQueueManager.RepeatMode.REPEAT_NONE;
import static com.soundcloud.android.playback.playqueue.QueueUtils.ALPHA_DISABLED;
import static com.soundcloud.android.playback.playqueue.QueueUtils.ALPHA_ENABLED;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;

import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class MagicBoxPlayQueueItemRenderer implements CellRenderer<MagicBoxPlayQueueUIItem> {

    interface MagicBoxListener {

        void clicked();

        void toggle(boolean checked);

    }

    private final PlayQueueManager playQueueManager;
    private Optional<MagicBoxListener> magicBoxListener = Optional.absent();

    @Inject
    MagicBoxPlayQueueItemRenderer(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_magic_box_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, final View itemView, List<MagicBoxPlayQueueUIItem> items) {
        final boolean isRepeat = !items.get(position).getRepeatMode().equals(REPEAT_NONE);
        setAlpha(itemView, isRepeat);
        setupToggle(itemView, isRepeat);

        itemView.setOnClickListener(view -> {
            final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
            if (currentPlayQueueItem.isTrack()) {
                if (magicBoxListener.isPresent()) magicBoxListener.get().clicked();
            }
        });
    }

    private void setupToggle(final View itemView, final boolean isRepeat) {
        final SwitchCompat toggle = (SwitchCompat) itemView.findViewById(R.id.toggle_auto_play);
        ViewUtils.extendTouchArea(toggle);
        toggle.setChecked(playQueueManager.isAutoPlay());
        toggle.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (magicBoxListener.isPresent()) magicBoxListener.get().toggle(checked);
            setAlpha(itemView, isRepeat);
        });
    }

    private void setAlpha(View itemView, boolean isRepeat) {
        boolean isAutoPlay = playQueueManager.isAutoPlay();
        float alpha = isRepeat || !isAutoPlay ? ALPHA_DISABLED : ALPHA_ENABLED;
        float buttonAlpha = isRepeat ? ALPHA_DISABLED : ALPHA_ENABLED;

        itemView.findViewById(R.id.station_icon).setAlpha(alpha);
        itemView.findViewById(R.id.toggle_auto_play_label).setAlpha(alpha);
        itemView.findViewById(R.id.toggle_auto_play_description).setAlpha(alpha);
        itemView.findViewById(R.id.toggle_auto_play).setAlpha(buttonAlpha);
    }

    public void setMagicBoxListener(MagicBoxListener magicboxListener) {
        this.magicBoxListener = Optional.fromNullable(magicboxListener);
    }

}
