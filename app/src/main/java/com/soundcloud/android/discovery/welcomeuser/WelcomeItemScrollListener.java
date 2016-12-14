package com.soundcloud.android.discovery.welcomeuser;

import com.soundcloud.android.discovery.DiscoveryAdapter;
import com.soundcloud.android.discovery.DiscoveryItem;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class WelcomeItemScrollListener extends RecyclerView.OnScrollListener {

    private final DiscoveryAdapter.DiscoveryItemListenerBucket listener;

    public WelcomeItemScrollListener(DiscoveryAdapter.DiscoveryItemListenerBucket listener) {
        this.listener = listener;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        float welcomeCardHeight = getWelcomeCardHeight(recyclerView);

        float translation = updateWelcomeTranslation(recyclerView, welcomeCardHeight, dy);
        updateSearchTranslation(getView(recyclerView, DiscoveryItem.Kind.SearchItem), translation, welcomeCardHeight);
    }

    private void updateSearchTranslation(View view, float dy, float height) {
        if (view == null) {
            return;
        }

        float translation = Math.max(Math.min(dy, height), 0);
        view.setTranslationY(translation);
    }

    private float updateWelcomeTranslation(RecyclerView recyclerView, float height, int dy) {
        View view = getView(recyclerView, DiscoveryItem.Kind.WelcomeUserItem);
        if (view == null) {
            return 0;
        }

        float translation = view.getTranslationY() + dy;
        float alpha = 1 - translation / height;

        view.setAlpha(alpha);
        if (Float.compare(translation, height) >= 0 || Float.compare(alpha, 0f) <= 0) {
            listener.dismissWelcomeUserItem(getViewPosition(recyclerView, DiscoveryItem.Kind.WelcomeUserItem));
            return 0;
        }
        view.setTranslationY(translation);
        return translation;
    }

    private float getWelcomeCardHeight(RecyclerView recyclerView) {
        View view = getView(recyclerView, DiscoveryItem.Kind.WelcomeUserItem);
        if (view == null) {
            return 0f;
        }

        return view.getHeight();
    }

    private View getView(RecyclerView recyclerView, DiscoveryItem.Kind kind) {
        int viewPosition = getViewPosition(recyclerView, kind);
        return recyclerView.getLayoutManager().findViewByPosition(viewPosition);
    }

    private int getViewPosition(RecyclerView recyclerView, DiscoveryItem.Kind kind) {
        int viewPosition = -1;

        int itemCount = recyclerView.getAdapter().getItemCount();
        for (int i = 0; i < itemCount; i++) {
            DiscoveryItem.Kind itemKind = ((DiscoveryAdapter) recyclerView.getAdapter()).getItem(i)
                                                                                        .getKind();
            if (itemKind == kind) {
                viewPosition = i;
            }
        }
        return viewPosition;
    }
}
