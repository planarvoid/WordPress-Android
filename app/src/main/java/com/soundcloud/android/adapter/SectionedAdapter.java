package com.soundcloud.android.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.behavior.InSection;
import com.soundcloud.android.model.behavior.Titled;
import com.soundcloud.android.view.adapter.behavior.SectionedListRow;

import android.util.SparseArray;
import android.view.View;

public abstract class SectionedAdapter<ModelType extends InSection> extends ScAdapter<ModelType> {

    @VisibleForTesting
    public enum ViewTypes {
        DEFAULT, SECTION
    }

    private static final int INITIAL_LIST_CAPACITY = 30;

    private final SparseArray<Titled> mListPositionsToSections;
    private Titled mCurrentSection;

    public SectionedAdapter() {
        this(new SparseArray<Titled>());
    }

    protected SectionedAdapter(SparseArray<Titled> listPositionsToSections){
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
        mCurrentSection = null;
    }

    @Override
    public void addItem(ModelType item) {
        super.addItem(item);
        if (item.getSection() != mCurrentSection){
            mCurrentSection = item.getSection();
            mListPositionsToSections.put(mItems.size() - 1, mCurrentSection);
        }
    }


    @Override
    protected void bindItemView(int position, View itemView) {
        if (itemView instanceof SectionedListRow){
            final Titled section = mListPositionsToSections.get(position, null);
            final SectionedListRow sectionedListRow = (SectionedListRow) itemView;
            if (section != null) {
                sectionedListRow.showSectionHeaderWithText(section.getTitle(itemView.getResources()));
            } else {
                sectionedListRow.hideSectionHeader();
            }
        } else {
            throw new IllegalArgumentException("Cannot use a sectioned adapter without a row type that impelements SectionedListRow");
        }
    }

}
