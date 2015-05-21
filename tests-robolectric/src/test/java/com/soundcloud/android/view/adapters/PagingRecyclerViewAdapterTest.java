package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PagingRecyclerViewAdapterTest {

    @Mock private CellPresenter<String> cellPresenter;
    @Mock private RecyclerView recyclerView;
    @Mock private ViewGroup parent;
    @Mock private Context context;
    @Mock private View rowView;
    @Mock private ProgressCellPresenter progressCellPresenter;

    private PagingRecyclerViewAdapter<String, TestViewHolder> adapter;
    private List<String> items = Arrays.asList("one", "two", "three");


    @Before
    public void setup() {
        adapter = new PagingRecyclerViewAdapter<String, TestViewHolder>(progressCellPresenter, cellPresenter) {
            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }
        };

        Observable.just(items).subscribe(adapter);

        when(parent.getContext()).thenReturn(Robolectric.application);
        when(progressCellPresenter.createView(Robolectric.application)).thenReturn(rowView);
    }

    @Test
    public void shouldCreateProgressRowWhenInLoadingState() {
        adapter.setLoading();

        expect(adapter.onCreateViewHolder(parent, PagingAwareAdapter.PROGRESS_VIEW_TYPE).itemView).toBe(rowView);
    }

    @Test
    public void shouldBindProgressRowWithWasErrorFalseInLoadingState() throws Exception {
        adapter.setLoading();

        adapter.onBindViewHolder(new TestViewHolder(rowView), items.size());

        verify(progressCellPresenter).bindView(rowView, false);
    }

    @Test
    public void shouldBindProgressRowWithErrorTrueInErrorState() throws Exception {
        adapter.onError(new Throwable());

        adapter.onBindViewHolder(new TestViewHolder(rowView), items.size());

        verify(progressCellPresenter).bindView(rowView, true);
    }

    @Test
    public void shouldCreateNormalItemRowUsingPresenter() {
        when(cellPresenter.createItemView(parent)).thenReturn(rowView);

        final int viewType = 0;
        adapter.onCreateViewHolder(parent, viewType);

        verify(cellPresenter).createItemView(parent);
        verifyZeroInteractions(progressCellPresenter);
    }

    @Test
    public void shouldCreateProgressRow() {
        adapter.setLoading();

        adapter.onCreateViewHolder(parent, PagingAwareAdapter.PROGRESS_VIEW_TYPE);

        verify(progressCellPresenter).createView(any(Context.class));
        verifyZeroInteractions(cellPresenter);
    }

    @Test
    public void shouldBindNormalItemRowUsingPresenter() {
        final int position = items.size() - 1;
        adapter.onBindViewHolder(new TestViewHolder(rowView), position);

        verify(cellPresenter).bindItemView(position, rowView, items);

        verifyZeroInteractions(progressCellPresenter);
    }

    @Test
    public void shouldSetCustomOnErrorRetryListenerForErrorRow() {
        View.OnClickListener listener = mock(View.OnClickListener.class);
        adapter.setOnErrorRetryListener(listener);

        verify(progressCellPresenter).setRetryListener(listener);
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}
