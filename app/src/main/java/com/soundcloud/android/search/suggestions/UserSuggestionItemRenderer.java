package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.checks.Preconditions;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UserSuggestionItemRenderer implements CellRenderer<UserSuggestionItemRenderer> {

    interface OnUserClickListener {
        void onUserClicked(String searchQuery);
    }

    private OnUserClickListener onUserClickListener;

    @Inject
    UserSuggestionItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return null;
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSuggestionItemRenderer> items) {

    }

    void setOnUserClickListener(OnUserClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.onUserClickListener = listener;
    }
}
