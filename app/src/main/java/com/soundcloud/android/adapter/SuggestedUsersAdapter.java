package com.soundcloud.android.adapter;

import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Genre;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.Log;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SuggestedUsersAdapter extends BaseAdapter {

    protected enum Section {
        FACEBOOK(R.string.onboarding_section_facebook),
        MUSIC(R.string.onboarding_section_music),
        AUDIO(R.string.onboarding_section_audio);

        public static Section fromGenreGrouping(Genre.Grouping grouping) {
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
        mListPositionsToSections = Maps.newHashMap();
    }

    public void addItem(GenreBucket bucket) {
        Section sectionForBucket = Section.fromGenreGrouping(bucket.getGenre().getGrouping());
        if (!mListPositionsToSections.containsValue(sectionForBucket)) {
            mListPositionsToSections.put(mGenreBuckets.size(), sectionForBucket);
        }
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

    protected Map<Integer, Section> getListPositionsToSectionsMap() {
        return Maps.newHashMap(mListPositionsToSections);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        GenreBucket genreBucket = getItem(position);

        final ItemViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ItemViewHolder();
            convertView = inflater.inflate(R.layout.suggested_users_list_item, null, false);
            convertView.setTag(viewHolder);
            viewHolder.genreTitle = (TextView) convertView.findViewById(android.R.id.text1);
            viewHolder.genreSubtitle = (TextView) convertView.findViewById(android.R.id.text2);
            viewHolder.sectionLabel = (TextView) convertView.findViewById(R.id.suggested_users_list_header_title);
            viewHolder.sectionHeader = convertView.findViewById(R.id.suggested_users_list_header);
            viewHolder.toggleFollow = (ToggleButton) convertView.findViewById(R.id.btn_user_bucket_select_all);
            viewHolder.toggleFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CompoundButton button = (CompoundButton) v;
                    final int position = (Integer) button.getTag();
                    getItem(position).setFollowed(button.isChecked());
                }
            });
        } else {
            viewHolder = (ItemViewHolder) convertView.getTag();
        }
        viewHolder.toggleFollow.setTag(position);

        configureSectionHeader(position, convertView, viewHolder);
        configureItemContent(genreBucket, viewHolder);

        return convertView;
    }

    private void configureItemContent(GenreBucket genreBucket, ItemViewHolder viewHolder) {
        final Resources res = viewHolder.genreTitle.getContext().getResources();
        final List<User> users = genreBucket.getUsers();
        final int numUsers = users.size();

        StringBuilder sb = new StringBuilder();
        if (numUsers == 1) {
            sb.append(users.get(0).getDisplayName());
        } else if (numUsers > 1) {
            sb.append(users.get(0).getDisplayName()).append(", ");
            sb.append(users.get(1).getDisplayName());

            if (numUsers > 2) {
                int moreUsers = numUsers - 2;
                sb.append(" ").append(res.getQuantityString(R.plurals.number_of_other_users, moreUsers, moreUsers));
            }
        }
        viewHolder.genreSubtitle.setText(sb.toString());
        viewHolder.genreTitle.setText(genreBucket.getGenre().getName());
        viewHolder.toggleFollow.setChecked(genreBucket.isFollowed());
    }

    private void configureSectionHeader(int position, View convertView, ItemViewHolder viewHolder) {
        Section section = mListPositionsToSections.get(position);
        if (section != null) {
            Context context = convertView.getContext();
            viewHolder.sectionLabel.setText(context.getString(section.getLabelResId()));
            viewHolder.sectionHeader.setVisibility(View.VISIBLE);
        } else {
            viewHolder.sectionHeader.setVisibility(View.GONE);
        }
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
        public View sectionHeader;
        public TextView genreTitle, genreSubtitle, sectionLabel;
        public ToggleButton toggleFollow;
    }
}
