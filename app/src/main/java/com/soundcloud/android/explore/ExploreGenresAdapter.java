package com.soundcloud.android.explore;

import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.view.adapters.ItemAdapter;
import rx.Observer;

import javax.inject.Inject;


class ExploreGenresAdapter extends ItemAdapter<ExploreGenre> implements Observer<GenreSection<ExploreGenre>> {

    private final GenreCellPresenter cellPresenter;

    @Inject
    ExploreGenresAdapter(GenreCellPresenter cellPresenter) {
        super(cellPresenter);
        this.cellPresenter = cellPresenter;
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
