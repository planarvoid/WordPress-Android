package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.playqueue.QueueUtils.getAlpha;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class HeaderPlayQueueItemRenderer implements CellRenderer<HeaderPlayQueueUIItem> {

    @Inject
    public HeaderPlayQueueItemRenderer(){}

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_header_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<HeaderPlayQueueUIItem> items) {
        final HeaderPlayQueueUIItem item = items.get(position);
        final TextView textHolder = (TextView) itemView.findViewById(R.id.title);
        textHolder.setText(item.getHeader());
        itemView.setAlpha(getAlpha(item.getRepeatMode(), item.getPlayState()));
    }
}
