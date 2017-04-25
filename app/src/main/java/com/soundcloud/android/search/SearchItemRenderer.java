package com.soundcloud.android.search;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class SearchItemRenderer implements CellRenderer {

    public interface SearchListener {
        void onSearchClicked(Context context);
    }

    private SearchListener searchListener;

    @Inject
    public SearchItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List unused) {
        ButterKnife.bind(this, itemView);
    }

    @OnClick(R.id.search_item)
    public void onSearchClick(View searchView) {
        if (searchListener != null) {
            searchListener.onSearchClicked(searchView.getContext());
        }
    }

    public void setSearchListener(SearchListener searchListener) {
        checkNotNull(searchListener);
        this.searchListener = searchListener;
    }
}
