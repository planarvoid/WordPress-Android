package com.soundcloud.android.collections;

import com.google.common.annotations.VisibleForTesting;
import rx.Observer;

import android.os.Parcelable;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public abstract class SectionedAdapter<ModelType extends Parcelable> extends ItemAdapter<ModelType> implements Observer<Section<ModelType>> {

    private static final int INITIAL_LIST_CAPACITY = 30;
    static final int ITEM_VIEW_TYPE_DEFAULT = 0;
    static final int ITEM_VIEW_TYPE_HEADER = 1;

    private final Map<Integer, RowDescriptor> mListPositionsToSections;

    public SectionedAdapter() {
        this(new HashMap<Integer, RowDescriptor>());
    }

    protected SectionedAdapter(Map<Integer, RowDescriptor> sectionHeaderPositions) {
        super(INITIAL_LIST_CAPACITY);
        mListPositionsToSections = sectionHeaderPositions;
    }

    @Override
    public int getItemViewType(int position) {
        return mListPositionsToSections.get(position).isSectionHeader ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_DEFAULT;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public void clear() {
        super.clear();
        mListPositionsToSections.clear();
    }

    public Section<ModelType> getSection(int position) {
        final RowDescriptor descriptor = mListPositionsToSections.get(position);
        return descriptor.section;
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        if (itemView instanceof SectionedListRow){
            final RowDescriptor descriptor = mListPositionsToSections.get(position);
            final SectionedListRow sectionedListRow = (SectionedListRow) itemView;

            if (descriptor.isSectionHeader) {
                sectionedListRow.showSectionHeaderWithText(itemView.getResources().getString(descriptor.section.getTitleId()));
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
        boolean isSectionHeader = true; // true only for the first item in a section
        for (ModelType item : section.getItems()) {
            RowDescriptor descriptor = new RowDescriptor();
            descriptor.section = section;
            descriptor.isSectionHeader = isSectionHeader;
            isSectionHeader = false;

            addItem(item);
            mListPositionsToSections.put(mItems.size() - 1, descriptor);
        }
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @VisibleForTesting
    protected final class RowDescriptor {

        private Section<ModelType> section;
        private boolean isSectionHeader;

    }
}
