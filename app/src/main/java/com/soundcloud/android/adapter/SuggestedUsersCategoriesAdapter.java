package com.soundcloud.android.adapter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.rx.observers.ScObserver;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SuggestedUsersCategoriesAdapter extends BaseAdapter {

    private static final int PROGRESS_VIEW_TYPE = 0;
    private static final int EMPTY_VIEW_TYPE = 1;
    private static final int DEFAULT_VIEW_TYPE = 2;
    private static final int INITIAL_LIST_CAPACITY = 30;
    private static final String TAG = "Sugg_User_Cat_Adp";

    private final StringBuilder mUserNamesBuilder;
    private final List<Category> mCategories;
    private final Set<CategoryGroup> mCategoryGroups;
    private final Map<Integer, Section> mListPositionsToSections;
    private final FollowingOperations mFollowingOperations;
    private FollowStatus mFollowStatus;

    public enum Section {
        FACEBOOK(CategoryGroup.KEY_FACEBOOK, R.string.suggested_users_section_facebook),
        MUSIC(CategoryGroup.KEY_MUSIC, R.string.suggested_users_section_music),
        SPEECH_AND_SOUNDS(CategoryGroup.KEY_SPEECH_AND_SOUNDS, R.string.suggested_users_section_audio);
        public static final EnumSet<Section> ALL_EXCEPT_FACEBOOK = EnumSet.of(MUSIC, SPEECH_AND_SOUNDS);
        public static final EnumSet<Section> ALL_SECTIONS = EnumSet.allOf(Section.class);

        private final String mKey;
        private final int mLabelResId;
        private String mLabel;

        Section(String key, int labelId) {
            mKey = key;
            mLabelResId = labelId;
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
        mFollowingOperations = new FollowingOperations().scheduleFromActivity();
        mCategories = new ArrayList<Category>(INITIAL_LIST_CAPACITY);
        mCategoryGroups = new TreeSet<CategoryGroup>(new CategoryGroupComparator());
        mListPositionsToSections = new HashMap<Integer, Section>();
        mUserNamesBuilder = new StringBuilder();
        mFollowStatus = followStatus;

        for (Section section : activeSections) {
            CategoryGroup categoryGroup = new CategoryGroup(section.mKey.toString());
            categoryGroup.setCategories(Lists.newArrayList(Category.PROGRESS));
            addItem(categoryGroup);
        }
    }

    public void addItem(CategoryGroup categoryGroup) {
        mCategoryGroups.remove(categoryGroup);
        if (categoryGroup.getCategoryCount() == 0) {
            categoryGroup.setCategories(Lists.newArrayList(Category.EMPTY));
        }
        mCategoryGroups.add(categoryGroup);

        mCategories.clear();
        mListPositionsToSections.clear();

        for (CategoryGroup group : mCategoryGroups) {
            mListPositionsToSections.put(mCategories.size(), Section.fromKey(group.getKey()));
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

    public List<Category> getItems() {
        return mCategories;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        final Category item = getItem(position);
        if (item == Category.EMPTY) {
            return EMPTY_VIEW_TYPE;
        } else if (item == Category.PROGRESS) {
            return PROGRESS_VIEW_TYPE;
        }
        return DEFAULT_VIEW_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == DEFAULT_VIEW_TYPE;
    }

    protected Map<Integer, Section> getListPositionsToSectionsMap() {
        return Maps.newHashMap(mListPositionsToSections);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final ItemViewHolder viewHolder;

        final int itemViewType = getItemViewType(position);
        if (itemViewType != DEFAULT_VIEW_TYPE) {
            if (convertView == null) {
                viewHolder = new ItemViewHolder();
                final int layout = itemViewType == PROGRESS_VIEW_TYPE ?
                        R.layout.suggested_users_category_list_loading_item : R.layout.suggested_users_category_list_empty_item;

                convertView = inflater.inflate(layout, parent, false);
                convertView.setTag(viewHolder);
                viewHolder.sectionHeader = (TextView) convertView.findViewById(R.id.suggested_users_list_header);
            } else {
                viewHolder = (ItemViewHolder) convertView.getTag();
            }

        } else {
            final Category category = getItem(position);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.suggested_users_category_list_item, null, false);
                viewHolder = getItemViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ItemViewHolder) convertView.getTag();
            }
            viewHolder.toggleFollow.setTag(position);
            configureItemContent(category, viewHolder);
        }

        configureSectionHeader(position, convertView, viewHolder);
        return convertView;
    }

    private ItemViewHolder getItemViewHolder(View convertView) {
        ItemViewHolder viewHolder;
        viewHolder = new ItemViewHolder();

        viewHolder.genreTitle = (TextView) convertView.findViewById(android.R.id.text1);
        viewHolder.genreSubtitle = (TextView) convertView.findViewById(android.R.id.text2);
        viewHolder.sectionHeader = (TextView) convertView.findViewById(R.id.suggested_users_list_header);
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

    private void configureItemContent(Category category, ItemViewHolder viewHolder) {
        final Resources res = viewHolder.genreTitle.getContext().getResources();

        final Set<Long> followedUserIds = mFollowStatus.getFollowedUserIds();
        final List<SuggestedUser> followedUsers = category.getFollowedUsers(followedUserIds);
        final List<SuggestedUser> subTextUsers = followedUsers.isEmpty() ? category.getUsers() : followedUsers;
        final int numUsers = subTextUsers.size();

        mUserNamesBuilder.setLength(0);
        if (numUsers == 1) {
            mUserNamesBuilder.append(subTextUsers.get(0).getUsername());
        } else if (numUsers > 1) {
            mUserNamesBuilder.append(subTextUsers.get(0).getUsername()).append(", ");
            mUserNamesBuilder.append(subTextUsers.get(1).getUsername());

            if (numUsers > 2) {
                int moreUsers = numUsers - 2;
                mUserNamesBuilder.append(" ").append(res.getQuantityString(R.plurals.number_of_other_users, moreUsers, moreUsers));
            }
        }
        viewHolder.genreSubtitle.setText(mUserNamesBuilder.toString());
        viewHolder.genreTitle.setText(category.getName());
        final boolean followed = category.isFollowed(followedUserIds);
        viewHolder.toggleFollow.setChecked(followed);
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
                Log.d(TAG, "adapter: got " + categoryGroup);
                addItem(categoryGroup);
            }
        };
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
        public TextView genreTitle, genreSubtitle, sectionHeader;
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
