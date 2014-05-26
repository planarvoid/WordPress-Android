package com.soundcloud.android.explore;

import com.soundcloud.android.Consts;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.model.ExploreGenre;
import rx.Observer;

import javax.inject.Inject;


class ExploreGenresAdapter extends ItemAdapter<ExploreGenre> implements Observer<GenreSection<ExploreGenre>> {

    static final int ITEM_VIEW_TYPE_DEFAULT = 0;
    static final int ITEM_VIEW_TYPE_HEADER = 1;

    private final GenreCellPresenter cellPresenter;

    @Inject
    ExploreGenresAdapter(GenreCellPresenter cellPresenter) {
        super(cellPresenter, Consts.LIST_PAGE_SIZE);
        this.cellPresenter = cellPresenter;
    }

    @Override
    public int getItemViewType(int position) {
        return cellPresenter.isSectionHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_DEFAULT;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public void clear() {
        super.clear();
        cellPresenter.clearSections();
    }

    @Override
    public void onCompleted() {
        notifyDataSetChanged();
    }

    @Override
    public void onNext(GenreSection<ExploreGenre> section) {
        boolean isSectionHeader = true; // true only for the first item in a section
        for (ExploreGenre item : section.getItems()) {
            addItem(item);
            cellPresenter.setSectionForPosition(items.size() - 1, section, isSectionHeader);
            isSectionHeader = false;
        }
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

}
