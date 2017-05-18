package com.soundcloud.android.presentation;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.List;

public class PagingRecyclerItemAdapterTest extends AndroidUnitTest {

    @Mock private View itemView;
    @Mock private CellRenderer<String> cellRenderer;
    @Mock private ProgressCellRenderer progressCellRenderer;
    @Mock private RecyclerView.AdapterDataObserver adapterObserver;

    private ViewGroup parentView = new FrameLayout(context());
    private List<String> items = Arrays.asList("one", "two", "three");
    
    // test subject
    private PagingRecyclerItemAdapter<String, TestViewHolder> adapter;

    @Before
    public void setUp() throws Exception {
        adapter = buildPagingAdapter(cellRenderer);
        Observable.just(items).subscribe(adapter);
    }

    @Test
    public void shouldCreateProgressRowWhenInLoadingState() {
        when(progressCellRenderer.createView(context())).thenReturn(itemView);
        adapter.setLoading();

        assertThat(adapter.onCreateViewHolder(parentView, ViewTypes.PROGRESS_VIEW_TYPE).itemView).isSameAs(itemView);
    }

    @Test
    public void shouldBindProgressRowWithWasErrorFalseInLoadingState() throws Exception {
        adapter.setLoading();

        adapter.onBindViewHolder(new TestViewHolder(itemView), items.size());

        verify(progressCellRenderer).bindView(itemView, false);
    }

    @Test
    public void shouldBindProgressRowWithErrorTrueInErrorState() throws Exception {
        adapter.onError(new Throwable());

        adapter.onBindViewHolder(new TestViewHolder(itemView), items.size());

        verify(progressCellRenderer).bindView(itemView, true);
    }

    @Test
    public void shouldCreateNormalItemRowUsingPresenter() {
        when(cellRenderer.createItemView(parentView)).thenReturn(itemView);

        final int viewType = 0;
        adapter.onCreateViewHolder(parentView, viewType);

        verify(cellRenderer).createItemView(parentView);
        verifyZeroInteractions(progressCellRenderer);
    }

    @Test
    public void shouldCreateProgressRow() {
        when(progressCellRenderer.createView(context())).thenReturn(itemView);
        Observable.just(items).subscribe(adapter);
        adapter.setLoading();

        adapter.onCreateViewHolder(parentView, ViewTypes.PROGRESS_VIEW_TYPE);

        verify(progressCellRenderer).createView(context());
        verifyZeroInteractions(cellRenderer);
    }

    @Test
    public void shouldNotifyDataSetChangedWhenChangingLoadingState() {
        adapter.registerAdapterDataObserver(adapterObserver);

        adapter.setLoading();

        verify(adapterObserver).onChanged();
    }

    @Test
    public void shouldBindNormalItemRowUsingPresenter() {
        final int position = items.size() - 1;
        adapter.onBindViewHolder(new TestViewHolder(itemView), position);

        verify(cellRenderer).bindItemView(position, itemView, items);

        verifyZeroInteractions(progressCellRenderer);
    }

    @Test
    public void shouldSetCustomOnErrorRetryListenerForErrorRow() {
        View.OnClickListener listener = mock(View.OnClickListener.class);
        adapter.setOnErrorRetryListener(listener);

        verify(progressCellRenderer).setRetryListener(listener);
    }

    @Test
    public void shouldReportItemCountPlusOneWhenLoadingSoProgressRowIsVisible() throws Exception {
        adapter.setLoading();

        assertThat(adapter.getItemCount()).isEqualTo(items.size() + 1);
    }

    @Test
    public void shouldReportItemCountPlusOneWhenFailedSoRetryRowIsVisible() throws Exception {
        adapter.onError(new Exception());

        assertThat(adapter.getItemCount()).isEqualTo(items.size() + 1);
    }

    private PagingRecyclerItemAdapter<String, TestViewHolder> buildPagingAdapter(CellRenderer<String> cellRenderer) {
        return new PagingRecyclerItemAdapter<String, TestViewHolder>(cellRenderer, progressCellRenderer) {
            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }

            @Override
            public int getBasicItemViewType(int position) {
                return 0;
            }
        };
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {
        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}
