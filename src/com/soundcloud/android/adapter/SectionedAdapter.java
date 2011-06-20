package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.api.Request;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SectionedAdapter extends LazyBaseAdapter implements SectionIndexer {

    public static final String TAG = "SectionedAdapter";

    public List<Section> sections =new ArrayList<Section>();

    public static class Section {
        public final String label;
        public final Class<?> model;
        public final List<Parcelable> data;
        public final Request request;

        public Section(String label, Class<?> model, List<Parcelable> data, Request request) {
            this.label = label;
            this.model = model;
            this.data = data;
            this.request = request;
        }
    }

    public SectionedAdapter(ScActivity context) {
        super(context, new ArrayList<Parcelable>(), Track.class);
    }

    @Override
    public List<Parcelable> getData() {
        List<Parcelable> data = new ArrayList<Parcelable>();
        for (Section section : sections){
            data.addAll(section.data);
        }
        return data;
    }

    @Override
    public int getCount() {
        int count = 0;
        for (Section section : sections){
            count += section.data.size();
        }
        return count;
    }

    public Object getItem(int location) {
         int c = 0;
        for (Section section : sections) {
            if (section.data.size() > 0) {
                if (location >= c && location < c + section.data.size()) {
                    return section.data.get(location - c);
                }
                c += section.data.size();
            }

        }
        return null;
    }

    public Class<?> getLoadModel(int index) {
        return sections.size() == 0 ? null :  sections.get(index).model;
    }

    public Request getRequest(int index) {
        return sections.size() == 0 ? null :  sections.get(index).request;
    }

    public List<Parcelable> getData(int index) {
        return sections.size() == 0 ? null : sections.get(index).data;
    }

    public void clearData(){
          for (Section section : sections) {
              section.data.clear();
          }
    }

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        View res = super.getView(position, convertView, parent);
        final int section = getSectionForPosition(position);
        boolean displaySectionHeaders = (getPositionForSection(section) == position);
        bindSectionHeader(res, position, displaySectionHeaders);
        return res;
    }

    public int getPositionForSection(int section) {
        if (section < 0) section = 0;
        if (section >= sections.size()) section = sections.size() - 1;
        int c = 0;
        for (int i = 0; i < sections.size(); i++) {
            if (section == i) {
                return c;
            }
            c += sections.get(i).data.size();
        }
        return 0;
    }

    public int getSectionForPosition(int position) {
        int c = 0;
        for (int i = 0; i < sections.size(); i++) {
            if (position >= c && position < c + sections.get(i).data.size()) {
                return i;
            }
            c += sections.get(i).data.size();
        }
        return -1;
    }

    public String[] getSections() {
        String[] res = new String[sections.size()];
        for (int i = 0; i < sections.size(); i++) {
            res[i] = sections.get(i).label;
        }
        return res;
    }


    /**
     * Header logic taken from http://code.google.com/p/android-amazing-listview/source/browse/trunk/
     */

    public static final int PINNED_HEADER_GONE = 0;
    public static final int PINNED_HEADER_VISIBLE = 1;
    public static final int PINNED_HEADER_PUSHED_UP = 2;

    public int getPinnedHeaderState(int position) {
        if (position < 0 || getCount() == 0) {
            return PINNED_HEADER_GONE;
        }

        int section = getSectionForPosition(position);
        int nextSectionPosition = getPositionForSection(section + 1);
        if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
            return PINNED_HEADER_PUSHED_UP;
        }

        return PINNED_HEADER_VISIBLE;
    }

    public void onScroll(SectionedListView view, int firstVisibleItem) {
        view.configureHeaderView(firstVisibleItem);
    }


    protected void bindSectionHeader(View view, int position, boolean displaySectionHeader) {
        if (displaySectionHeader) {
            view.findViewById(R.id.listHeader).setVisibility(View.VISIBLE);
            TextView lSectionTitle = (TextView) view.findViewById(R.id.listHeader);
            lSectionTitle.setText(getSections()[getSectionForPosition(position)]);
        } else {
            view.findViewById(R.id.listHeader).setVisibility(View.GONE);
        }
    }

    public void configurePinnedHeader(View header, int position, int bgColor, int txtColor) {
        TextView txtHeader = (TextView) header.findViewById(R.id.listHeader);
        txtHeader.setText(getSections()[getSectionForPosition(position)]);
        txtHeader.setBackgroundColor(bgColor);
        txtHeader.setTextColor(txtColor);
    }
}
