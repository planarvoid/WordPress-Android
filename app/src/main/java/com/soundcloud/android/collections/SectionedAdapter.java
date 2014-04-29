package com.soundcloud.android.collections;

import com.google.common.annotations.VisibleForTesting;
import rx.Observer;

import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;

public abstract class SectionedAdapter<ModelType extends Parcelable> extends ItemAdapter<ModelType> implements Observer<Section<ModelType>> {

    private static final int INITIAL_LIST_CAPACITY = 30;
    static final int ITEM_VIEW_TYPE_DEFAULT = 0;
    static final int ITEM_VIEW_TYPE_HEADER = 1;

    private final SparseArray<RowDescriptor> listPositionsToSections;

    public SectionedAdapter() {
        this(new SparseArray<RowDescriptor>());
    }

    protected SectionedAdapter(SparseArray<RowDescriptor> sectionHeaderPositions) {
        super(INITIAL_LIST_CAPACITY);
        listPositionsToSections = sectionHeaderPositions;
    }

    @Override
    public int getItemViewType(int position) {
        return listPositionsToSections.get(position).isSectionHeader ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_DEFAULT;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public void clear() {
        super.clear();
        listPositionsToSections.clear();
    }

    public Section<ModelType> getSection(int position) {
        final RowDescriptor descriptor = listPositionsToSections.get(position);
        return descriptor.section;
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        if (itemView instanceof SectionedListRow){
            RowDescriptor descriptor = listPositionsToSections.get(position);
            SectionedListRow sectionedListRow = (SectionedListRow) itemView;

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
            listPositionsToSections.put(items.size() - 1, descriptor);
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
