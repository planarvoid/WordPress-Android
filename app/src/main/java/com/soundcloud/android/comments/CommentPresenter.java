package com.soundcloud.android.comments;

import com.soundcloud.android.view.adapters.CellPresenter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class CommentPresenter implements CellPresenter<Comment> {
    @Override
    public View createItemView(int position, ViewGroup parent) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<Comment> items) {
        ((TextView) itemView).setText(items.get(position).getText());
    }
}
