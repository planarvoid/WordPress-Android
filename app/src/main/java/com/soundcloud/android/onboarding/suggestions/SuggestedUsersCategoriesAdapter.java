package com.soundcloud.android.onboarding.suggestions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.collections.SingleLineCollectionTextView;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SuggestedUsersCategoriesAdapter extends BaseAdapter {

    private static final int INITIAL_LIST_CAPACITY = 30;

    private final List<Category> categories;
    private final Set<CategoryGroup> categoryGroups;

    private final SparseArray<Section> listPositionsToSections;
    private final FollowingOperations followingOperations;
    private final DefaultSubscriber<Void> mNotifyWhenDoneObserver = new DefaultSubscriber<Void>() {
        @Override
        public void onCompleted() {
            notifyDataSetChanged();
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            notifyDataSetChanged();
        }
    };
    private EnumSet<Section> activeSections;

    @Inject
    public SuggestedUsersCategoriesAdapter(FollowingOperations followingOperations) {
        this(Section.ALL_SECTIONS, followingOperations);
    }

    @VisibleForTesting
    public SuggestedUsersCategoriesAdapter(EnumSet<Section> activeSections, FollowingOperations followingOperations) {
        this.followingOperations = followingOperations;
        categories = new ArrayList<>(INITIAL_LIST_CAPACITY);
        categoryGroups = new TreeSet<>(new CategoryGroupComparator());
        listPositionsToSections = new SparseArray<>();
        this.activeSections = activeSections;
    }

    public void setActiveSections(EnumSet<Section> activeSections) {
        this.activeSections = activeSections;
    }

    public void addItem(CategoryGroup categoryGroup) {
        categoryGroups.remove(categoryGroup);
        categoryGroups.add(categoryGroup);

        if (categoryGroups.size() < activeSections.size()) {
            for (Section section : activeSections) {
                if (section.showLoading) {
                    categoryGroups.add(CategoryGroup.createProgressGroup(section.key));
                }
            }
        }

        categories.clear();
        listPositionsToSections.clear();

        final Set<SuggestedUser> uniqueSuggestedUsersSet = new HashSet<>();
        for (CategoryGroup group : categoryGroups) {
            group.removeDuplicateUsers(uniqueSuggestedUsersSet);
            listPositionsToSections.put(categories.size(), Section.fromKey(group.getKey()));
            categories.addAll(group.isEmpty() ? Lists.newArrayList(Category.empty()) : group.getNonEmptyCategories());
        }
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Category getItem(int position) {
        return categories.get(position);
    }

    public List<Category> getItems() {
        return categories;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getDisplayType().ordinal();
    }

    @Override
    public int getViewTypeCount() {
        return Category.DisplayType.values().length;
    }

    @Override
    public boolean isEnabled(int position) {
        return !getItem(position).isProgressOrEmpty();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemViewHolder viewHolder = null;

        final Category category = getItem(position);
        final View view;
        switch (category.getDisplayType()) {
            case PROGRESS:
                if (convertView == null) {
                    view = inflater.inflate(R.layout.suggested_users_category_list_loading_item, parent, false);
                    viewHolder = getItemViewHolder(view);
                } else {
                    view = convertView;
                    viewHolder = (ItemViewHolder) view.getTag();
                }
                break;

            case EMPTY:
            case ERROR:
                if (convertView == null) {
                    view = inflater.inflate(R.layout.suggested_users_category_list_empty_item, parent, false);
                    viewHolder = getItemViewHolder(view);
                    viewHolder.emptyMessage = (TextView) view.findViewById(android.R.id.text1);
                } else {
                    view = convertView;
                    viewHolder = (ItemViewHolder) view.getTag();
                }
                viewHolder.emptyMessage.setText(category.getEmptyMessage(view.getResources()));
                break;

            case DEFAULT:
            default:
                if (convertView == null) {
                    view = inflater.inflate(R.layout.suggested_users_category_list_item, parent, false);
                    viewHolder = getContentItemViewHolder(view);
                } else {
                    view = convertView;
                    viewHolder = (ItemViewHolder) view.getTag();
                }
                viewHolder.toggleFollow.setTag(position);
                configureItemContent(parent.getContext(), category, viewHolder);
                break;
        }

        configureSectionHeader(position, view, viewHolder);
        return view;
    }

    protected SparseArray<Section> getListPositionsToSectionsMap() {
        return listPositionsToSections;
    }

    private ItemViewHolder getContentItemViewHolder(View convertView) {
        ItemViewHolder viewHolder = getItemViewHolder(convertView);
        viewHolder.genreTitle = (TextView) convertView.findViewById(android.R.id.text1);
        viewHolder.genreSubtitle = (SingleLineCollectionTextView) convertView.findViewById(android.R.id.text2);
        viewHolder.toggleFollow = (CheckBox) convertView.findViewById(R.id.btn_user_bucket_select_all);
        viewHolder.toggleFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final boolean shouldFollow = ((CompoundButton) v).isChecked();
                final Category toggleCategory = getItem((Integer) v.getTag());
                final Set<Long> followedUserIds = followingOperations.getFollowedUserIds();
                Observable<Void> toggleFollowings;
                if (shouldFollow) {
                    final List<SuggestedUser> notFollowedUsers = toggleCategory.getNotFollowedUsers(followedUserIds);
                    toggleFollowings = followingOperations.addFollowingsBySuggestedUsers(notFollowedUsers);
                } else {
                    final List<SuggestedUser> followedUsers = toggleCategory.getFollowedUsers(followedUserIds);
                    toggleFollowings = followingOperations.removeFollowingsBySuggestedUsers(followedUsers);
                }
                toggleFollowings.observeOn(AndroidSchedulers.mainThread()).subscribe(mNotifyWhenDoneObserver);
            }
        });
        return viewHolder;
    }

    private ItemViewHolder getItemViewHolder(View convertView) {
        ItemViewHolder viewHolder = new ItemViewHolder();
        convertView.setTag(viewHolder);
        viewHolder.sectionHeader = (TextView) convertView.findViewById(R.id.list_section_header);
        return viewHolder;
    }

    private void configureItemContent(Context context, Category category, ItemViewHolder viewHolder) {
        viewHolder.genreTitle.setText(category.getName(context));
        viewHolder.toggleFollow.setChecked(category.isFollowed(followingOperations.getFollowedUserIds()));
        viewHolder.genreSubtitle.setDisplayItems(getSubtextUsers(category));
    }

    private void configureSectionHeader(int position, View convertView, ItemViewHolder viewHolder) {
        Section section = listPositionsToSections.get(position);
        if (section != null) {
            viewHolder.sectionHeader.setText(section.getLabel(convertView.getResources()));
            viewHolder.sectionHeader.setVisibility(View.VISIBLE);
        } else {
            viewHolder.sectionHeader.setVisibility(View.GONE);
        }
    }

    /* package */ List<String> getSubtextUsers(Category category) {
        final Set<Long> followedUserIds = followingOperations.getFollowedUserIds();
        final List<SuggestedUser> followedUsers = category.getFollowedUsers(followedUserIds);
        final List<SuggestedUser> subtextUsers = followedUsers.isEmpty() ? category.getUsers() : followedUsers;
        return Lists.transform(subtextUsers, new Function<SuggestedUser, String>() {
            @Override
            public String apply(SuggestedUser input) {
                return input.getUsername();
            }
        });

    }

    public enum Section {
        FACEBOOK(CategoryGroup.KEY_FACEBOOK, R.string.suggested_users_section_facebook, true),
        MUSIC(CategoryGroup.KEY_MUSIC, R.string.suggested_users_section_music, true),
        SPEECH_AND_SOUNDS(CategoryGroup.KEY_SPEECH_AND_SOUNDS, R.string.suggested_users_section_audio, false);

        public static final EnumSet<Section> ALL_EXCEPT_FACEBOOK = EnumSet.of(MUSIC, SPEECH_AND_SOUNDS);

        public static final EnumSet<Section> ALL_SECTIONS = EnumSet.allOf(Section.class);
        private final String key;
        private final int labelResId;
        private final boolean showLoading;
        private String label;

        Section(String key, int labelId, boolean showLoading) {
            this.key = key;
            this.labelResId = labelId;
            this.showLoading = showLoading;
        }

        String getLabel(Resources resources) {
            if (label == null) {
                label = resources.getString(labelResId);
            }
            return label;
        }

        static Section fromKey(String key) {
            for (Section section : values()) {
                if (section.key.equals(key)) {
                    return section;
                }
            }
            return SPEECH_AND_SOUNDS;
        }
    }

    private static class ItemViewHolder {
        public TextView genreTitle, sectionHeader, emptyMessage;
        public SingleLineCollectionTextView genreSubtitle;
        public CompoundButton toggleFollow;
    }

    private static class CategoryGroupComparator implements Comparator<CategoryGroup>, Serializable {

        @Override
        public int compare(CategoryGroup lhs, CategoryGroup rhs) {
            return Section.fromKey(lhs.getKey()).compareTo(Section.fromKey(rhs.getKey()));
        }
    }
}
