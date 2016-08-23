package com.soundcloud.android.collection;

import static butterknife.ButterKnife.findById;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public abstract class SimpleHeaderRenderer<T>
        implements CellRenderer<T>, PopupMenuWrapper.PopupMenuWrapperListener {

    public interface MenuClickListener {
        void onClearClicked();
    }

    private final MenuClickListener clickListener;
    private final PopupMenuWrapper.Factory popupMenuFactory;

    public SimpleHeaderRenderer(@Nullable MenuClickListener listener,
                                PopupMenuWrapper.Factory popupMenuFactory) {
        this.clickListener = listener;
        this.popupMenuFactory = popupMenuFactory;
    }

    public abstract String getTitle(T item);
    public abstract String getMenuActionText();

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.header_with_menu, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<T> items) {
        final T header = items.get(position);
        itemView.setEnabled(false);
        setTitle(itemView, header);
        setMenu(itemView);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.clear_history:
                if (clickListener != null) {
                    clickListener.onClearClicked();
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

    private void setMenu(View itemView) {
        final View overflowButton = findById(itemView, R.id.overflow_button);
        final PopupMenuWrapper popupMenu = popupMenuFactory.build(itemView.getContext(), overflowButton);

        popupMenu.inflate(R.menu.simple_header_menu_actions);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.setItemText(R.id.clear_history, getMenuActionText());

        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });
    }


    private void setTitle(View itemView, T header) {
        ButterKnife.<TextView>findById(itemView, R.id.header_text).setText(getTitle(header));
    }
}
