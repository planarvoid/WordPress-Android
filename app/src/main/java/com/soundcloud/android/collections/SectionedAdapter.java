package com.soundcloud.android.collections;

import com.google.common.annotations.VisibleForTesting;
import rx.Observer;

import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;

public abstract class SectionedAdapter<ModelType extends Parcelable> extends ItemAdapter<ModelType> implements Observer<Section<ModelType>> {

    @VisibleForTesting
    protected enum ViewTypes {
        DEFAULT, SECTION
    }

    private static final int INITIAL_LIST_CAPACITY = 30;

    private final SparseArray<Section<ModelType>> mListPositionsToSections;

    public SectionedAdapter() {
        this(new SparseArray<Section<ModelType>>());
    }

    protected SectionedAdapter(SparseArray<Section<ModelType>> listPositionsToSections) {
        super(INITIAL_LIST_CAPACITY);
        mListPositionsToSections = listPositionsToSections;
    }

    @Override
    public int getItemViewType(int position) {
        return mListPositionsToSections.get(position, null) == null ?
                ViewTypes.DEFAULT.ordinal() : ViewTypes.SECTION.ordinal();
    }

    @Override
    public int getViewTypeCount() {
        return ViewTypes.values().length;
    }

    @Override
    public void clear() {
        super.clear();
        mListPositionsToSections.clear();
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        if (itemView instanceof SectionedListRow){
            final Section section = mListPositionsToSections.get(position, null);
            final SectionedListRow sectionedListRow = (SectionedListRow) itemView;
            if (section != null) {
                sectionedListRow.showSectionHeaderWithText(itemView.getResources().getString(section.getTitleId()));
            } else {
                sectionedListRow.hideSectionHeader();
            }
        } else {
            throw new IllegalArgumentException("Cannot use a sectioned adapter without a row type that implements SectionedListRow");
        }
    }

    @Override
    public void onCompleted() {
        notifyDataSetChanged();
    }

    @Override
    public void onNext(Section<ModelType> section) {
        mListPositionsToSections.put(mItems.size(), section);
        for (ModelType item : section.getItems()){
            addItem(item);
        }
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }
}
