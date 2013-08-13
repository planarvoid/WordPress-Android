package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class ScAdapterTest {

    private ScAdapter<Track> adapter = new ScAdapter<Track>(10) {
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
}
