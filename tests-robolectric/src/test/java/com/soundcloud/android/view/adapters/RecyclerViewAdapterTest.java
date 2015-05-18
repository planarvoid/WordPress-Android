package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class RecyclerViewAdapterTest {

    @Mock private View itemView;
    @Mock private ViewGroup parent;
    @Mock private CellPresenter<String> cellPresenter;

    private RecyclerViewAdapter<String, TestViewHolder> adapter;
    private TestViewHolder viewHolder;

    @Before
    public void setUp() throws Exception {
        adapter = buildAdapter(cellPresenter);
        viewHolder = new TestViewHolder(itemView);
    }

    @Test
    public void shouldAddItems() {
        expect(adapter.getItemCount()).toBe(0);
        adapter.addItem("item");
        expect(adapter.getItemCount()).toBe(1);
    }

    @Test
    public void shouldAddItemsFromObservableSequence() {
        Observable.just(Arrays.asList("one", "two", "three")).subscribe(adapter);
        expect(adapter.getItemCount()).toBe(3);
    }

    @Test
    public void shouldCreateItemViewWithPresenter() {
        FrameLayout parent = mock(FrameLayout.class);
        adapter.addItem("item");
        adapter.onCreateViewHolder(parent, 0);
        verify(cellPresenter).createItemView(parent);
    }

    @Test
    public void shouldCreateItemViewForTwoDifferentViewTypes() {
        CellPresenter presenterOne = mock(CellPresenter.class);
        CellPresenter presenterTwo = mock(CellPresenter.class);
        adapter = buildAdapter(new CellPresenterBinding<String>(0, presenterOne), new CellPresenterBinding<String>(1, presenterTwo));

        adapter.onCreateViewHolder(parent, 0);
        verify(presenterOne).createItemView(parent);

        adapter.onCreateViewHolder(parent, 1);
        verify(presenterTwo).createItemView(parent);
    }

    @Test
    public void shouldBindItemView() {
        when(cellPresenter.createItemView(parent)).thenReturn(itemView);
        adapter.addItem("item");

        adapter.onBindViewHolder(viewHolder, 0);
        verify(cellPresenter).bindItemView(0, itemView, Arrays.asList("item"));
    }

    private RecyclerViewAdapter<String, TestViewHolder> buildAdapter(final CellPresenterBinding... bindings) {
        return new RecyclerViewAdapter<String, TestViewHolder>(bindings) {
            @Override
            public int getItemViewType(int position) {
                return position;
            }

            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }
        };
    }

    private RecyclerViewAdapter<String, TestViewHolder> buildAdapter(CellPresenter<String> cellPresenter) {
        return new RecyclerViewAdapter<String, TestViewHolder>(cellPresenter) {
            @Override
            protected TestViewHolder createViewHolder(View itemView) {
                return new TestViewHolder(itemView);
            }
        };
    }

    private static class TestViewHolder extends RecyclerView.ViewHolder {

        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}