package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.api.Request;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public abstract class SectionedAdapter extends LazyBaseAdapter implements SectionIndexer {

    public static final String TAG = "SectionedAdapter";

    public List<Section> sections =new ArrayList<Section>();

    public static class Section {
        public final String label;
        public final int labelId;
        public final Class<?> model;
        public final List<Parcelable> data;
        public final Request request;
        public final Uri content;
        public String nextHref;
        public int pageIndex;

        public Section(String label, Class<?> model, List<Parcelable> data, Uri content, Request request) {
            this.label = label;
            this.labelId = 0; // not used
            this.model = model;
            this.data = data;
            this.request = request;
            this.content = content;
            this.nextHref = null;

        }

        public Section(int labelId, Class<?> model, List<Parcelable> data, Uri content, Request request) {
            this.labelId = labelId;
            this.label = null;
            this.model = model;
            this.data = data;
            this.request = request;
            this.content = content;
            this.nextHref = null;
        }

        public Request getRequest(boolean refresh) {
            if (request == null) return null;
            return (refresh || TextUtils.isEmpty(nextHref)) ? new Request(request) : new Request(nextHref);
        }

        public void clear() {
            data.clear();
            nextHref = null;
            pageIndex = 0;
        }

        public void applyLabel(TextView tv) {
            if (labelId > 0) {
                tv.setText(labelId);
            } else {
                tv.setText(label);
            }
        }
    }

    public SectionedAdapter(Context context) {
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

    public List<Parcelable> getData(int index) {
        return sections.size() == 0 ? null : sections.get(index).data;
    }

    public void addItem(int index, Parcelable newItem) {
        getData(index).add(newItem);
    }

    public void clearData(){
        for (Section section : sections) {
            section.clear();
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

    public Section[] getSections() {
        return sections.toArray(new Section[sections.size()]);
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
            getSections()[getSectionForPosition(position)].applyLabel(lSectionTitle);
        } else {
            view.findViewById(R.id.listHeader).setVisibility(View.GONE);
        }
    }

    public void configurePinnedHeader(View header, int position) {
        TextView txtHeader = (TextView) header.findViewById(R.id.listHeader);
        if (getSectionForPosition(position) != -1 && getSectionForPosition(position) < getSections().length){
            getSections()[getSectionForPosition(position)].applyLabel(txtHeader);
        }
    }
}
