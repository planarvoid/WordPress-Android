package com.soundcloud.android.adapter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ScObserver;
import com.soundcloud.android.view.SingleLineCollectionTextView;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SuggestedUsersCategoriesAdapter extends BaseAdapter {

    private static final int INITIAL_LIST_CAPACITY = 30;

    private final List<Category> mCategories;
    private final Set<CategoryGroup> mCategoryGroups;
    private final Map<Integer, Section> mListPositionsToSections;
    private final FollowingOperations mFollowingOperations;
    private final EnumSet<Section> mActiveSections;
    private FollowStatus mFollowStatus;

    public enum Section {
        FACEBOOK(CategoryGroup.KEY_FACEBOOK, R.string.suggested_users_section_facebook, true),
        MUSIC(CategoryGroup.KEY_MUSIC, R.string.suggested_users_section_music, true),
        SPEECH_AND_SOUNDS(CategoryGroup.KEY_SPEECH_AND_SOUNDS, R.string.suggested_users_section_audio, false);

        public static final EnumSet<Section> ALL_EXCEPT_FACEBOOK = EnumSet.of(MUSIC, SPEECH_AND_SOUNDS);
        public static final EnumSet<Section> ALL_SECTIONS = EnumSet.allOf(Section.class);

        private final String mKey;
        private final int mLabelResId;
        private final boolean mShowLoading;

        private String mLabel;

        Section(String key, int labelId, boolean showLoading) {
            mKey = key;
            mLabelResId = labelId;
            mShowLoading = showLoading;
        }

        String getLabel(Resources resources) {
            if (mLabel == null) {
                mLabel = resources.getString(mLabelResId);
            }
            return mLabel;
        }

        static Section fromKey(String key) {
            for (Section section : values()) {
                if (section.mKey.equals(key)) {
                    return section;
                }
            }
            return SPEECH_AND_SOUNDS;
        }
    }

    public SuggestedUsersCategoriesAdapter(EnumSet<Section> activeSections) {
        this(activeSections, FollowStatus.get());
    }

    public SuggestedUsersCategoriesAdapter(EnumSet<Section> activeSections, FollowStatus followStatus) {
        mFollowingOperations = new FollowingOperations().observeOn(ScSchedulers.UI_SCHEDULER);
        mCategories = new ArrayList<Category>(INITIAL_LIST_CAPACITY);
        mCategoryGroups = new TreeSet<CategoryGroup>(new CategoryGroupComparator());
        mListPositionsToSections = new HashMap<Integer, Section>();
        mFollowStatus = followStatus;
        mActiveSections = activeSections;
    }

    public void addItem(CategoryGroup categoryGroup) {
        mCategoryGroups.remove(categoryGroup);
        mCategoryGroups.add(categoryGroup);

        if (mCategoryGroups.size() < mActiveSections.size()){
            for (Section section : mActiveSections){
                if (section.mShowLoading) {
                    mCategoryGroups.add(CategoryGroup.createProgressGroup(section.mKey));
                }
            }
        }

        mCategories.clear();
        mListPositionsToSections.clear();

        for (CategoryGroup group : mCategoryGroups) {
            mListPositionsToSections.put(mCategories.size(), Section.fromKey(group.getKey()));
            mCategories.addAll(group.isEmpty() ? Lists.newArrayList(Category.empty()) : group.getCategories());
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

    public List<Category> getItems() {
        return mCategories;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType().ordinal();
    }

    @Override
    public int getViewTypeCount() {
        return Category.Type.values().length;
    }

    @Override
    public boolean isEnabled(int position) {
        return !getItem(position).isProgressOrEmpty();
    }

    protected Map<Integer, Section> getListPositionsToSectionsMap() {
        return Maps.newHashMap(mListPositionsToSections);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemViewHolder viewHolder = null;

        final Category category = getItem(position);
        switch (category.getType()){
            case PROGRESS:
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.suggested_users_category_list_loading_item, parent, false);
                    viewHolder = getItemViewHolder(convertView);
                } else {
                    viewHolder = (ItemViewHolder) convertView.getTag();
                }
                break;

            case EMPTY:
            case ERROR:
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.suggested_users_category_list_empty_item, parent, false);
                    viewHolder = getItemViewHolder(convertView);
                    viewHolder.emptyMessage = (TextView) convertView.findViewById(android.R.id.text1);
                } else {
                    viewHolder = (ItemViewHolder) convertView.getTag();
                }
                viewHolder.emptyMessage.setText(category.getEmptyMessage(convertView.getResources()));
                break;

            case DEFAULT:
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.suggested_users_category_list_item, parent, false);
                    viewHolder = getContentItemViewHolder(convertView);
                } else {
                    viewHolder = (ItemViewHolder) convertView.getTag();
                }
                viewHolder.toggleFollow.setTag(position);
                configureItemContent(category, viewHolder);
                break;
        }

        configureSectionHeader(position, convertView, viewHolder);
        return convertView;
    }

    private ItemViewHolder getContentItemViewHolder(View convertView) {
        ItemViewHolder viewHolder = getItemViewHolder(convertView);
        viewHolder.genreTitle = (TextView) convertView.findViewById(android.R.id.text1);
        viewHolder.genreSubtitle = (SingleLineCollectionTextView) convertView.findViewById(android.R.id.text2);
        viewHolder.toggleFollow = (ToggleButton) convertView.findViewById(R.id.btn_user_bucket_select_all);
        viewHolder.toggleFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final boolean shouldFollow = ((CompoundButton) v).isChecked();
                final Category toggleCategory = getItem((Integer) v.getTag());
                final Set<Long> followedUserIds = FollowStatus.get().getFollowedUserIds();
                if (shouldFollow) {
                    final List<SuggestedUser> notFollowedUsers = toggleCategory.getNotFollowedUsers(followedUserIds);
                    mFollowingOperations.addFollowingsBySuggestedUsers(notFollowedUsers).subscribe(mNotifyWhenDoneObserver);
                } else {
                    final List<SuggestedUser> followedUsers = toggleCategory.getFollowedUsers(followedUserIds);
                    mFollowingOperations.removeFollowingsBySuggestedUsers(followedUsers).subscribe(mNotifyWhenDoneObserver);
                }
            }
        });
        return viewHolder;
    }

    private ItemViewHolder getItemViewHolder(View convertView) {
        ItemViewHolder viewHolder = new ItemViewHolder();
        convertView.setTag(viewHolder);
        viewHolder.sectionHeader = (TextView) convertView.findViewById(R.id.suggested_users_list_header);
        return viewHolder;
    }

    private void configureItemContent(Category category, ItemViewHolder viewHolder) {
        viewHolder.genreTitle.setText(category.getName());
        viewHolder.toggleFollow.setChecked(category.isFollowed(mFollowStatus.getFollowedUserIds()));
        viewHolder.genreSubtitle.setDisplayItems(getSubtextUsers(category));
    }

    /* package */ List<String> getSubtextUsers(Category category) {
        final Set<Long> followedUserIds = mFollowStatus.getFollowedUserIds();
        final List<SuggestedUser> followedUsers = category.getFollowedUsers(followedUserIds);
        final List<SuggestedUser> subtextUsers =  followedUsers.isEmpty() ? category.getUsers() : followedUsers;
        return Lists.transform(subtextUsers, new Function<SuggestedUser, String>() {
            @Override
            public String apply(SuggestedUser input) {
                return input.getUsername();
            }
        });

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

    private final ScObserver mNotifyWhenDoneObserver = new ScObserver() {
        @Override
        public void onCompleted() {
            notifyDataSetChanged();
        }

        @Override
        public void onError(Exception e) {
            notifyDataSetChanged();
        }
    };

    private static class ItemViewHolder {
        public TextView genreTitle, sectionHeader, emptyMessage;
        public SingleLineCollectionTextView genreSubtitle;
        public ToggleButton toggleFollow;
    }

    private static class CategoryGroupComparator implements Comparator<CategoryGroup> {

        @Override
        public int compare(CategoryGroup lhs, CategoryGroup rhs) {
            return Section.fromKey(lhs.getKey()).compareTo(Section.fromKey(rhs.getKey()));
        }

        @Override
        public boolean equals(Object object) {
            return false;
        }
    }
}
