package com.soundcloud.android.view.adapters;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * This class is merely an adapter around our old ListRow types until we've ported search over to api-mobile
 */
public class LegacyUserRowPresenter implements CellPresenter<ScResource> {

    public static final int TYPE_USER = LegacyUserRowPresenter.class.hashCode();

    private final ImageOperations imageOperations;
    private final Screen originScreen;

    public LegacyUserRowPresenter(ImageOperations imageOperations, Screen originScreen) {
        this.imageOperations = imageOperations;
        this.originScreen = originScreen;
    }

    @Override
    public IconLayout createItemView(int position, ViewGroup parent) {
        return new UserlistRow(parent.getContext(), originScreen, imageOperations);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ScResource> items) {
        ((ListRow) itemView).display(position, items.get(position));
    }

    @Override
    public int getItemViewType() {
        return TYPE_USER;
    }
}
