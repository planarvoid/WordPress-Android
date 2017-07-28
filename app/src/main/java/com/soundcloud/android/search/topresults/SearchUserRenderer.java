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
import java.util.Map;
import java.util.WeakHashMap;

@AutoFactory
class SearchUserRenderer implements CellRenderer<SearchItem.User> {
    private final FollowableUserItemRenderer userItemRenderer;
    private final PublishSubject<UiAction.UserClick> userClick;
    private final Map<View, UiAction.UserClick> argsMap = new WeakHashMap<>();

    SearchUserRenderer(@Provided FollowableUserItemRenderer userItemRenderer, PublishSubject<UiAction.UserClick> userClick) {
        this.userItemRenderer = userItemRenderer;
        this.userClick = userClick;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View itemView = userItemRenderer.createItemView(parent);
        itemView.setOnClickListener(view -> userClick.onNext(argsMap.get(itemView)));
        return itemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.User> items) {
        final SearchItem.User user = items.get(position);
        argsMap.put(itemView, user.clickAction());
        userItemRenderer.bindItemView(position, itemView, user.userItem(), Optional.of(user.clickAction().clickParams().get().clickSource().key));
    }

}
