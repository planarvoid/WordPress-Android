package com.soundcloud.android.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Section;
import com.soundcloud.android.view.adapter.behavior.SectionedListRow;
import rx.Observer;

import android.util.SparseArray;
import android.view.View;

public abstract class SectionedAdapter<ModelType> extends ScAdapter<ModelType> implements Observer<Section<ModelType>> {

    @VisibleForTesting
    protected enum ViewTypes {
        DEFAULT, SECTION
    }

    private static final int INITIAL_LIST_CAPACITY = 30;

    private final SparseArray<Section<ModelType>> mListPositionsToSections;
    private final Observer<Section<ModelType>> mDelegateObserver;

    public SectionedAdapter(Observer<Section<ModelType>> delegateObserver) {
        this(new SparseArray<Section<ModelType>>(), delegateObserver);
    }

    protected SectionedAdapter(SparseArray<Section<ModelType>> listPositionsToSections, Observer<Section<ModelType>> delegateObserver) {
        super(INITIAL_LIST_CAPACITY);
        mListPositionsToSections = listPositionsToSections;
        mDelegateObserver = delegateObserver;
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
            throw new IllegalArgumentException("Cannot use a sectioned adapter without a row type that impelements SectionedListRow");
        }
    }

    @Override
    public void onCompleted() {
        notifyDataSetChanged();
        mDelegateObserver.onCompleted();
    }

    @Override
    public void onError(Exception error) {
        mDelegateObserver.onError(error);
    }

    @Override
    public void onNext(Section<ModelType> section) {
        mListPositionsToSections.put(mItems.size(), section);
        for (ModelType item : section.getItems()){
            addItem(item);
        }
        mDelegateObserver.onNext(section);
    }

}
