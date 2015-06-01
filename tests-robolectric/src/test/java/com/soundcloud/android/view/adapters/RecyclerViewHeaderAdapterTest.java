package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RecyclerViewHeaderAdapterTest {

    private static final int BASIC_ITEM_TYPE = 0;

    @Mock private View itemView;
    @Mock private ViewGroup parent;
    @Mock private CellRenderer<String> cellRenderer;
    @Mock private View.OnClickListener clickListener;
    @Mock private RecyclerView recyclerView;
    @Mock private HeaderCellRenderer headerCellRenderer;
    private Context context = Robolectric.application;

    private List<String> items = Arrays.asList("one", "two", "three");
    private RecyclerViewAdapter<String, TestViewHolder> adapter;

    @Before
    public void setUp() throws Exception {
        adapter = buildAdapter(cellRenderer);
        Observable.just(items).subscribe(adapter);
        when(parent.getContext()).thenReturn(context);
    }

    @Test
    public void shouldCreateHeaderView() {
        when(headerCellRenderer.createView(context)).thenReturn(itemView);

        final View rowView = adapter.onCreateViewHolder(parent, RecyclerViewAdapter.HEADER_VIEW_TYPE).itemView;

        expect(rowView).toBe(itemView);
    }

    @Test
    public void shouldCreateNormalView() {
        when(cellRenderer.createItemView(parent)).thenReturn(itemView);

        final View rowView = adapter.onCreateViewHolder(parent, RecyclerViewAdapter.DEFAULT_VIEW_TYPE).itemView;

        expect(rowView).toBe(itemView);
    }

    @Test
    public void shouldReturnHeaderViewWithoutBinding() {
        adapter.onBindViewHolder(new TestViewHolder(itemView), 0);

        verifyZeroInteractions(cellRenderer);
    }

    @Test
    public void shouldBindItemView() {
        adapter.onBindViewHolder(new TestViewHolder(itemView), 1);

        verify(cellRenderer).bindItemView(0, itemView, items);
    }

    @Test
    public void getItemCountIncludesHeaderItem() throws Exception {
        expect(adapter.getItemCount()).toBe(items.size() + 1);
    }

    @Test
    public void returnsBasicItemType() throws Exception {
        expect(adapter.getItemViewType(1)).toEqual(BASIC_ITEM_TYPE);
    }


    @Test
    public void adjustPositionForHeaderReturnsAdapterPositionMinusOneForHeader() throws Exception {
        expect(adapter.adjustPositionForHeader(1)).toEqual(0);
    }

    private RecyclerViewAdapter<String, TestViewHolder> buildAdapter(CellRenderer<String> cellPresenter) {
        return new RecyclerViewAdapter<String, TestViewHolder>(cellPresenter) {
            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }

            @Nullable
            @Override
            protected HeaderCellRenderer getHeaderCellRenderer() {
                return headerCellRenderer;
            }

            @Override
            public int getBasicItemViewType(int position) {
                return BASIC_ITEM_TYPE;
            }
        };
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}