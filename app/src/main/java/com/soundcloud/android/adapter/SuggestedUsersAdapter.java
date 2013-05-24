package com.soundcloud.android.adapter;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Genre;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.utils.Log;
import rx.util.functions.Action1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SuggestedUsersAdapter extends BaseAdapter {

    // maps genre buckets to list sections based on a genre's grouping field
    private static final Function<GenreBucket, Section> FN_BUCKETS_TO_SECTIONS = new Function<GenreBucket, Section>() {
        @Override
        public Section apply(GenreBucket genreBucket) {
            Genre.Grouping grouping = genreBucket.getGenre().getGrouping();
            switch (grouping) {
                case FACEBOOK_FRIENDS:
                case FACEBOOK_LIKES:
                    return Section.FACEBOOK;
                case MUSIC:
                    return Section.MUSIC;
                default: // let's be forwards compatible, can't hurt to place unknown things in the most generic bucket
                    return Section.AUDIO;
            }
        }
    };

    enum ItemViewType { GENRE_BUCKET, HEADER }

    enum Section {
        FACEBOOK(R.string.onboarding_section_facebook),
        MUSIC(R.string.onboarding_section_music),
        AUDIO(R.string.onboarding_section_audio);

        Section(int labelResId) {
            mLabelResId = labelResId;
        }

        public int getLabelResId() {
            return mLabelResId;
        }

        private int mLabelResId;
    }

    private final List<GenreBucket> mGenreBuckets;
    private Map<Integer, Section> mListPositionsToSections;

    public SuggestedUsersAdapter() {
        mGenreBuckets = new LinkedList<GenreBucket>();
    }

    public void addItem(GenreBucket bucket) {
        mGenreBuckets.add(bucket);
    }

    @Override
    public int getCount() {
        return mGenreBuckets.size();
    }

    @Override
    public GenreBucket getItem(int position) {
        return mGenreBuckets.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void notifyDataSetChanged() {
        mListPositionsToSections = getListPositionsToSectionsMap();
        super.notifyDataSetChanged();
    }

    private Map<Integer, Section> getListPositionsToSectionsMap() {
        Multimap<Section, GenreBucket> bucketsBySection = Multimaps.index(mGenreBuckets, FN_BUCKETS_TO_SECTIONS);

        final int FACEBOOK_HEADER_POS = 0;
        final int MUSIC_HEADER_POS = bucketsBySection.get(Section.FACEBOOK).size() + 1;
        final int AUDIO_HEADER_POS = bucketsBySection.get(Section.MUSIC).size() + MUSIC_HEADER_POS + 1;

        Map<Integer, Section> sections = Maps.newHashMapWithExpectedSize(Section.values().length);
        sections.put(FACEBOOK_HEADER_POS, Section.FACEBOOK);
        sections.put(MUSIC_HEADER_POS, Section.MUSIC);
        sections.put(AUDIO_HEADER_POS, Section.AUDIO);

        return sections;
    }

    @Override
    public int getItemViewType(int position) {
        // the section data will only be available after the data has been fully loaded
        if (mListPositionsToSections == null) return IGNORE_ITEM_VIEW_TYPE;

        if (mListPositionsToSections.containsKey(position)) {
            return ItemViewType.HEADER.ordinal();
        }

        return ItemViewType.GENRE_BUCKET.ordinal();
    }

    @Override
    public int getViewTypeCount() {
        return ItemViewType.values().length;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int itemViewType = getItemViewType(position);
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View itemLayout;
        if (itemViewType == ItemViewType.HEADER.ordinal()) {
            itemLayout = setupHeaderItem(position, convertView, inflater);
        } else {
            itemLayout = setupGenreBucketItem(position, convertView, inflater);
        }

        return itemLayout;
    }

    private View setupHeaderItem(int position, View convertView, LayoutInflater inflater) {
        HeaderViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.list_section_header, null, false);
            convertView.setTag(viewHolder);
            viewHolder.mSectionLabel = (TextView) convertView.findViewById(android.R.id.text1);
        } else {
            viewHolder = (HeaderViewHolder) convertView.getTag();
        }

        Context context = convertView.getContext();
        Section section = mListPositionsToSections.get(position);
        viewHolder.mSectionLabel.setText(context.getString(section.getLabelResId()));
        return convertView;
    }

    private View setupGenreBucketItem(int position, View convertView, LayoutInflater inflater) {
        ItemViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ItemViewHolder();
            convertView = inflater.inflate(R.layout.suggested_users_list_item, null, false);
            convertView.setTag(viewHolder);
            viewHolder.mGenreTitle = (TextView) convertView.findViewById(android.R.id.text1);
            viewHolder.mGenreSubtitle = (TextView) convertView.findViewById(android.R.id.text2);
        } else {
            viewHolder = (ItemViewHolder) convertView.getTag();
        }

        GenreBucket bucket = mGenreBuckets.get(position);

        viewHolder.mGenreTitle.setText(bucket.getGenre().getName());
        if (bucket.hasUsers()) {
            // TODO
            viewHolder.mGenreSubtitle.setText(bucket.getUsers().size() + " users");
        }

        return convertView;
    }

    public Action1<GenreBucket> onNextGenreBucket() {
        return new Action1<GenreBucket>() {
            @Override
            public void call(GenreBucket bucket) {
                Log.d(SuggestedUsersAdapter.this, "adapter: got " + bucket);
                addItem(bucket);
            }
        };
    }

    private static class ItemViewHolder {
        public TextView mGenreTitle, mGenreSubtitle;
    }

    private static class HeaderViewHolder {
        public TextView mSectionLabel;
    }
}
