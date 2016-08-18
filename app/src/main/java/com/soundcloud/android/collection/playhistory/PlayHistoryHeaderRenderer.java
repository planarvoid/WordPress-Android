package com.soundcloud.android.collection.playhistory;

import static butterknife.ButterKnife.findById;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

@AutoFactory
class PlayHistoryHeaderRenderer
        implements CellRenderer<PlayHistoryItemHeader>, PopupMenuWrapper.PopupMenuWrapperListener {

    private final PlayHistoryAdapter.PlayHistoryClickListener clickListener;
    private final Resources resources;
    private final PopupMenuWrapper.Factory popupMenuFactory;

    PlayHistoryHeaderRenderer(@Nullable PlayHistoryAdapter.PlayHistoryClickListener listener,
                              @Provided Resources resources,
                              @Provided PopupMenuWrapper.Factory popupMenuFactory) {
        this.clickListener = listener;
        this.resources = resources;
        this.popupMenuFactory = popupMenuFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.play_history_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlayHistoryItemHeader> items) {
        final PlayHistoryItemHeader header = items.get(position);
        itemView.setEnabled(false);
        setTitle(itemView, header);
        setMenu(itemView, header);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.clear_history:
                if (clickListener != null) {
                    clickListener.onClearHistoryClicked();
                }
                return true;
            default:
                throw new IllegalArgumentException("Unknown menu item id");
        }
    }

    @Override
    public void onDismiss() {
        // no-op
    }

    private void setMenu(View itemView, PlayHistoryItemHeader header) {
        final View overflowButton = findById(itemView, R.id.play_history_overflow_button);
        final PopupMenuWrapper popupMenu = popupMenuFactory.build(itemView.getContext(), overflowButton);

        popupMenu.inflate(R.menu.play_history_actions);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.setItemEnabled(R.id.clear_history, header.trackCount() > 0);

        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });
    }

    private void setTitle(View itemView, PlayHistoryItemHeader header) {
        final int trackCount = header.trackCount();

        String title = resources.getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
        ButterKnife.<TextView>findById(itemView, R.id.header_text).setText(title);
    }
}
