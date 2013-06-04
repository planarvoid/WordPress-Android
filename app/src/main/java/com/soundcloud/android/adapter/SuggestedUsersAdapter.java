package com.soundcloud.android.adapter;

import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.Log;
import rx.util.functions.Action1;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SuggestedUsersAdapter extends BaseAdapter {

    private final StringBuilder mUserNamesBuilder;
    private final List<Category> mCategories;
    private final List<CategoryGroup> mCategoryGroups;
    private final Map<Integer, Section> mListPositionsToSections;

    enum Section {
        FACEBOOK(CategoryGroup.URN_FACEBOOK, R.string.onboarding_section_facebook),
        MUSIC(CategoryGroup.URN_MUSIC, R.string.onboarding_section_music),
        SPEECH_AND_SOUNDS(CategoryGroup.URN_SPEECH_AND_SOUNDS, R.string.onboarding_section_audio);

        private final ClientUri mUrn;
        private final int mLabelResId;
        private String mLabel;

        Section(String sectionUrn, int labelId) {
            mUrn = ClientUri.fromUri(sectionUrn);
            mLabelResId = labelId;
        }

        String getLabel(Resources resources) {
            if (mLabel == null) {
                mLabel = resources.getString(mLabelResId);
            }
            return mLabel;
        }

        static Section fromUrn(ClientUri urn){
            for (Section section : values()){
                if (section.mUrn.equals(urn)){
                    return section;
                }
            }
            return SPEECH_AND_SOUNDS;
        }
    }

    public SuggestedUsersAdapter() {
        mCategories = new LinkedList<Category>();
        mCategoryGroups = new LinkedList<CategoryGroup>();
        mListPositionsToSections = new HashMap<Integer, Section>();
        mUserNamesBuilder = new StringBuilder();
    }

    public void addItem(CategoryGroup categoryGroup) {
        mCategoryGroups.add(categoryGroup);
        Collections.sort(mCategoryGroups, new CategoryGroupComparator());

        mCategories.clear();
        mListPositionsToSections.clear();

        for (CategoryGroup group : mCategoryGroups) {
            mListPositionsToSections.put(mCategories.size(), Section.fromUrn(group.getUrn()));
            mCategories.addAll(group.getCategories());
        }
    }

    @Override
    public int getCount() {
        return mCategories.size();
    }

    @Override
    public Category getItem(int position) {
        return mCategories.get(position);
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
        Category category = getItem(position);

        final ItemViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ItemViewHolder();
            convertView = inflater.inflate(R.layout.suggested_users_list_item, null, false);
            convertView.setTag(viewHolder);
            viewHolder.genreTitle = (TextView) convertView.findViewById(android.R.id.text1);
            viewHolder.genreSubtitle = (TextView) convertView.findViewById(android.R.id.text2);
            viewHolder.sectionHeader = (TextView) convertView.findViewById(R.id.suggested_users_list_header);
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
        configureItemContent(category, viewHolder);

        return convertView;
    }

    private void configureItemContent(Category category, ItemViewHolder viewHolder) {
        final Resources res = viewHolder.genreTitle.getContext().getResources();
        final List<User> users = category.getUsers();
        final int numUsers = users.size();

        mUserNamesBuilder.setLength(0);
        if (numUsers == 1) {
            mUserNamesBuilder.append(users.get(0).getDisplayName());
        } else if (numUsers > 1) {
            mUserNamesBuilder.append(users.get(0).getDisplayName()).append(", ");
            mUserNamesBuilder.append(users.get(1).getDisplayName());

            if (numUsers > 2) {
                int moreUsers = numUsers - 2;
                mUserNamesBuilder.append(" ").append(res.getQuantityString(R.plurals.number_of_other_users, moreUsers, moreUsers));
            }
        }
        viewHolder.genreSubtitle.setText(mUserNamesBuilder.toString());
        viewHolder.genreTitle.setText(category.getName());
        viewHolder.toggleFollow.setChecked(category.isFollowed());
    }

    private void configureSectionHeader(int position, View convertView, ItemViewHolder viewHolder) {
        Section section = mListPositionsToSections.get(position);
        if (section != null) {
            viewHolder.sectionHeader.setText(section.getLabel(convertView.getResources()));
            viewHolder.sectionHeader.setVisibility(View.VISIBLE);
        } else {
            viewHolder.sectionHeader.setVisibility(View.GONE);
        }
    }

    public Action1<CategoryGroup> onNextCategoryGroup() {
        return new Action1<CategoryGroup>() {
            @Override
            public void call(CategoryGroup categoryGroup) {
                Log.d(SuggestedUsersAdapter.this, "adapter: got " + categoryGroup);
                addItem(categoryGroup);
            }
        };
    }

    private static class ItemViewHolder {
        public TextView genreTitle, genreSubtitle, sectionHeader;
        public ToggleButton toggleFollow;
    }

    private static class CategoryGroupComparator implements Comparator<CategoryGroup>{

        @Override
        public int compare(CategoryGroup lhs, CategoryGroup rhs) {
            return Section.fromUrn(lhs.getUrn()).compareTo(Section.fromUrn(rhs.getUrn()));
        }

        @Override
        public boolean equals(Object object) {
            return false;
        }
    }
}
