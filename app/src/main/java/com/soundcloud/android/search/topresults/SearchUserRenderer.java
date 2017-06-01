package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.adapters.FollowableUserItemRenderer;
import com.soundcloud.java.optional.Optional;
import io.reactivex.subjects.PublishSubject;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory
class SearchUserRenderer implements CellRenderer<SearchItem.User> {
    private final FollowableUserItemRenderer userItemRenderer;
    private final PublishSubject<SearchItem.User> userClick;

    SearchUserRenderer(@Provided FollowableUserItemRenderer userItemRenderer, PublishSubject<SearchItem.User> userClick) {
        this.userItemRenderer = userItemRenderer;
        this.userClick = userClick;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return userItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.User> items) {
        final SearchItem.User user = items.get(position);
        itemView.setOnClickListener(view -> userClick.onNext(user));
        userItemRenderer.bindItemView(position, itemView, user.userItem(), Optional.of(user.source().key));
    }

}
