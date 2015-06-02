package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RecyclerViewPagingAdapterTest {

    @Mock private View itemView;
    @Mock private ViewGroup parent;
    @Mock private CellRenderer<String> cellRenderer;
    @Mock private ProgressCellRenderer progressCellRenderer;

    private List<String> items = Arrays.asList("one", "two", "three");
    private PagingRecyclerViewAdapter<String, TestViewHolder> adapter;

    @Before
    public void setUp() throws Exception {
        adapter = buildPagingAdapter(cellRenderer);
        Observable.just(items).subscribe(adapter);
        when(parent.getContext()).thenReturn(Robolectric.application);
    }

    @Test
    public void shouldCreateProgressRowWhenInLoadingState() {
        adapter = buildPagingAdapter(cellRenderer);
        when(progressCellRenderer.createView(Robolectric.application)).thenReturn(itemView);
        adapter.setLoading();

        expect(adapter.onCreateViewHolder(parent, ViewTypes.PROGRESS_VIEW_TYPE).itemView).toBe(itemView);
    }

    @Test
    public void shouldBindProgressRowWithWasErrorFalseInLoadingState() throws Exception {
        adapter = buildPagingAdapter(cellRenderer);
        Observable.just(items).subscribe(adapter);
        adapter.setLoading();

        adapter.onBindViewHolder(new TestViewHolder(itemView), items.size());

        verify(progressCellRenderer).bindView(itemView, false);
    }

    @Test
    public void shouldBindProgressRowWithErrorTrueInErrorState() throws Exception {
        adapter = buildPagingAdapter(cellRenderer);
        Observable.just(items).subscribe(adapter);
        adapter.onError(new Throwable());

        adapter.onBindViewHolder(new TestViewHolder(itemView), items.size());

        verify(progressCellRenderer).bindView(itemView, true);
    }

    @Test
    public void shouldCreateNormalItemRowUsingPresenter() {
        adapter = buildPagingAdapter(cellRenderer);
        when(cellRenderer.createItemView(parent)).thenReturn(itemView);

        final int viewType = 0;
        adapter.onCreateViewHolder(parent, viewType);

        verify(cellRenderer).createItemView(parent);
        verifyZeroInteractions(progressCellRenderer);
    }

    @Test
    public void shouldCreateProgressRow() {
        adapter = buildPagingAdapter(cellRenderer);
        Observable.just(items).subscribe(adapter);
        adapter.setLoading();

        adapter.onCreateViewHolder(parent, ViewTypes.PROGRESS_VIEW_TYPE);

        verify(progressCellRenderer).createView(Robolectric.application);
        verifyZeroInteractions(cellRenderer);
    }

    @Test
    public void shouldBindNormalItemRowUsingPresenter() {
        adapter = buildPagingAdapter(cellRenderer);
        Observable.just(items).subscribe(adapter);
        final int position = items.size() - 1;
        adapter.onBindViewHolder(new TestViewHolder(itemView), position);

        verify(cellRenderer).bindItemView(position, itemView, items);

        verifyZeroInteractions(progressCellRenderer);
    }

    @Test
    public void shouldSetCustomOnErrorRetryListenerForErrorRow() {
        adapter = buildPagingAdapter(cellRenderer);
        View.OnClickListener listener = mock(View.OnClickListener.class);
        adapter.setOnErrorRetryListener(listener);

        verify(progressCellRenderer).setRetryListener(listener);
    }

    private PagingRecyclerViewAdapter<String, TestViewHolder> buildPagingAdapter(CellRenderer<String> cellRenderer) {
        return new PagingRecyclerViewAdapter<String, TestViewHolder>(cellRenderer, progressCellRenderer) {
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
