package com.soundcloud.android.collections;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ItemAdapterTest {

    private ItemAdapter<Track> adapter = new ItemAdapter<Track>(10) {
        @Override
        protected TextView createItemView(int position, ViewGroup parent) {
            return new TextView(parent.getContext());
        }

        @Override
        protected void bindItemView(int position, View itemView) {
            ((TextView) itemView).setText(getItem(position).getTitle());
        }
    };

    @Test
    public void shouldAddItems() {
        expect(adapter.getCount()).toBe(0);
        adapter.addItem(new Track());
        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void shouldGetItem() {
        Track item = new Track();
        adapter.addItem(item);
        expect(adapter.getItem(0)).toBe(item);
    }

    @Test
    public void shouldGetItems() {
        adapter.addItem(new Track(1));
        adapter.addItem(new Track(2));

        List<Track> items = adapter.getItems();

        expect(items.size()).toEqual(2);
        expect(items.get(0)).toEqual(new Track(1));
        expect(items.get(1)).toEqual(new Track(2));
    }

    @Test
    public void shouldDefaultToIdentityForItemIdFunction() {
        expect(adapter.getItemId(1)).toBe(1L);
    }

    @Test
    public void shouldCreateAndBindNewItemView() {
        Track item = new Track();
        item.setTitle("New track");
        adapter.addItem(item);

        View itemView = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(((TextView) itemView).getText()).toEqual("New track");
    }

    @Test
    public void shouldConvertItemView() {
        Track item = new Track();
        item.setTitle("New track");
        adapter.addItem(item);

        TextView convertView = new TextView(Robolectric.application);
        convertView.setText("Old track");
        View itemView = adapter.getView(0, convertView, new FrameLayout(Robolectric.application));
        expect(itemView).toBe(convertView);
        expect(((TextView) itemView).getText()).toEqual("New track");
    }

    @Test
    public void shouldSaveAllItemsInSaveInstanceState() {
        Bundle bundle = new Bundle();

        adapter.saveInstanceState(bundle);
        expect(bundle.containsKey(ItemAdapter.EXTRA_KEY_ITEMS)).toBeTrue();

        ArrayList<Parcelable> savedItems = bundle.getParcelableArrayList(ItemAdapter.EXTRA_KEY_ITEMS);
        expect(savedItems.size()).toEqual(adapter.getCount());
        for (int i = 0; i < adapter.getCount(); i++) {
            expect(savedItems.get(i)).toEqual(adapter.getItem(i));
        }
    }

    @Test
    public void shouldRestoreAllItemsInRestoreInstanceState() {
        expect(adapter.getCount()).toBe(0);

        Bundle bundle = new Bundle();
        ArrayList<Track> tracks = Lists.newArrayList(new Track(1), new Track(2));
        bundle.putParcelableArrayList(ItemAdapter.EXTRA_KEY_ITEMS, tracks);

        adapter.restoreInstanceState(bundle);
        expect(adapter.getCount()).toBe(2);

        for (int i = 0; i < adapter.getCount(); i++) {
            expect(adapter.getItem(i)).toEqual(tracks.get(i));
        }
    }
}
