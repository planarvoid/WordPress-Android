package com.soundcloud.android.search;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.text.TextUtils;

public class SearchFragment extends ScListFragment {
    private @Nullable Search mCurrentSearch;

    public SearchFragment() {
        super();
    }

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putParcelable("contentUri", Content.SEARCH.uri);
        fragment.setArguments(args);
        return fragment;
    }

    public void setCurrentSearch(Search currentSearch) {
        if (mCurrentSearch != currentSearch) {
            mCurrentSearch = currentSearch;
            SearchAdapter adapter = (SearchAdapter) getListAdapter();
            if (adapter != null) {
                adapter.setCurrentSearch(currentSearch);
            }
            reset();
        }
    }

    @Override
    protected Screen getScreen() {
        return mCurrentSearch.getScreen();
    }

    @Override
    protected boolean canAppend() {
        return mCurrentSearch != null && super.canAppend();
    }

    @Override
    protected Request getRequest(boolean isRefresh) {
        if (mCurrentSearch != null && (TextUtils.isEmpty(mNextHref) || isRefresh)) {
            // first page
            return mCurrentSearch.request();
        } else {
            return super.getRequest(isRefresh);
        }
    }
}
