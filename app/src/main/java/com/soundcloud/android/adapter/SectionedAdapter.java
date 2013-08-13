package com.soundcloud.android.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.behavior.InSection;
import com.soundcloud.android.model.behavior.Titled;
import com.soundcloud.android.view.adapter.behavior.SectionedListRow;

import android.util.SparseArray;
import android.view.View;

public abstract class SectionedAdapter<ModelType extends InSection> extends ScAdapter<ModelType> {

    @VisibleForTesting
    protected static final int VIEW_TYPE_DEFAULT = 0;
    @VisibleForTesting
    protected static final int VIEW_TYPE_SECTION = 1;

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
        return mListPositionsToSections.get(position, null) == null ? VIEW_TYPE_DEFAULT : VIEW_TYPE_SECTION;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
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
            // set section header
            Titled section = mListPositionsToSections.get(position, null);
            if (section != null) {
                ((SectionedListRow) itemView).showSectionHeaderWithText(section.getTitle(itemView.getResources()));
            } else {
                ((SectionedListRow) itemView).hideSectionHeader();
            }
        } else {
            throw new IllegalArgumentException("Cannot use a sectioned adapter without a row type that impelements SectionedListRow");
        }
    }

}
