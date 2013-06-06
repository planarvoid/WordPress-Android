package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.R;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoriesAdapterTest {

    private SuggestedUsersCategoriesAdapter adapter;
    @Mock
    private FollowStatus followStatus;

    @Before
    public void setup() throws CreateModelException {
        initMocks(this);
        adapter = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS, followStatus);
    }

    @Test
    public void shouldNotHaveFacebookLoadingSection() {
        expect(new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_EXCEPT_FACEBOOK, followStatus).getCount() ).toBe(2);
    }

    @Test
    public void shouldHaveFacebookLoadingSection() {
        expect(adapter.getCount()).toBe(3);
    }

    @Test
    public void shouldHandleUnexpectedSection() throws CreateModelException {
        SuggestedUsersCategoriesAdapter adapter1 = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_EXCEPT_FACEBOOK, followStatus);
        adapter1.addItem(facebook());
        expect(adapter1.getCount()).toBe(4);
    }

    @Test
    public void addItemShouldReplaceProgressItems() throws CreateModelException {
        adapter.addItem(audio());
        adapter.addItem(music());
        adapter.addItem(facebook());

        for (Category category : adapter.getItems()) {
            expect(category).not.toBe(Category.PROGRESS);
        }
    }

    @Test
    public void addItemShouldReplaceDummySections() throws CreateModelException {
        adapter.addItem(emptyAudio());
        adapter.addItem(music());

        expect(adapter.getItem(0)).toBe(Category.PROGRESS);
        expect(adapter.getItem(1)).not.toBe(Category.PROGRESS);
        expect(adapter.getItem(1)).not.toBe(Category.EMPTY);
        expect(adapter.getItem(music().getCategoryCount() + 1)).toBe(Category.EMPTY);
    }

    @Test
    public void emptyCategoryItemsShouldNotBeEnabled() {
        expect(adapter.isEnabled(0)).toBeFalse();
        expect(adapter.isEnabled(1)).toBeFalse();
        expect(adapter.isEnabled(2)).toBeFalse();
    }

    @Test
    public void shouldCountItems() throws CreateModelException {
        // initially, we only have 3 dummy items
        expect(adapter.getCount()).toBe(3);

        // 1 completed section, 2 more dummy sections waiting for data
        adapter.addItem(facebook());
        expect(adapter.getCount()).toBe(2 + facebook().getCategoryCount());

        adapter.addItem(audio());
        adapter.addItem(music());
        expect(adapter.getCount()).toBe(
                facebook().getCategoryCount() +
                music().getCategoryCount() +
                audio().getCategoryCount()
        );
    }

    @Test
    public void shouldBuildListPositionsToSectionsMapWhileAddingNewItems() throws CreateModelException {
        Map<Integer, SuggestedUsersCategoriesAdapter.Section> sectionMap = adapter.getListPositionsToSectionsMap();
        expect(sectionMap).not.toBeNull();
        expect(sectionMap.values()).toContainExactly(SuggestedUsersCategoriesAdapter.Section.FACEBOOK, SuggestedUsersCategoriesAdapter.Section.MUSIC, SuggestedUsersCategoriesAdapter.Section.SPEECH_AND_SOUNDS);
    }

    @Test
    public void shouldGetViewWithHeader() {
        final int positionWithHeader = 0;
        View itemLayout = adapter.getView(positionWithHeader, null, new FrameLayout(Robolectric.application));

        expect(itemLayout).not.toBeNull();
        View headerView = itemLayout.findViewById(R.id.suggested_users_list_header);
        expect(headerView).not.toBeNull();
        expect(headerView.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldGetViewWithoutHeader() throws CreateModelException {
        addAllSections();
        final int positionWithoutHeader = 1;
        View itemLayout = adapter.getView(positionWithoutHeader, null, new FrameLayout(Robolectric.application));

        expect(itemLayout).not.toBeNull();
        View headerView = itemLayout.findViewById(R.id.suggested_users_list_header);
        expect(headerView).not.toBeNull();
        expect(headerView.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldSetCorrectBucketTextForSingleUser() throws CreateModelException {
        addAllSections();
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex");
    }

    @Test
    public void shouldSetCorrectBucketTextForTwoUsers() throws CreateModelException {
        addAllSections();
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex"), buildUser("Forss")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex, Forss");
    }

    @Test
    public void shouldSetCorrectBucketTextForMultipleUsers() throws CreateModelException {
        addAllSections();
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(
                buildUser("Skrillex"), buildUser("Forss"), buildUser("Rick Astley")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex, Forss and 1 other");
    }

    @Test
    public void shouldSetCorrectBucketTextForMultipleCategoryUsersWithOneFollowing() throws CreateModelException {
        addAllSections();
        SuggestedUser followedUser = TestHelper.getModelFactory().createModel(SuggestedUser.class);
        when(followStatus.getFollowedUserIds()).thenReturn(Sets.newHashSet(followedUser.getId()));

        Category category = adapter.getItem(0);
        category.getUsers().add(followedUser);

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual(followedUser.getUsername());
    }

    @Test
    public void shouldCheckFollowButtonIfAtLeastOneUserIsFollowed() throws CreateModelException {
        addAllSections();
        SuggestedUser followedUser = TestHelper.getModelFactory().createModel(SuggestedUser.class);
        when(followStatus.getFollowedUserIds()).thenReturn(Sets.newHashSet(followedUser.getId()));

        Category category = adapter.getItem(0);
        category.getUsers().add(followedUser);

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        final CompoundButton followButton = (CompoundButton) itemLayout.findViewById(R.id.btn_user_bucket_select_all);

        expect(followButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotCheckFollowButtonIfNoUserIsFollowed() throws CreateModelException {
        addAllSections();

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        final CompoundButton followButton = (CompoundButton) itemLayout.findViewById(R.id.btn_user_bucket_select_all);

        expect(followButton.isChecked()).toBeFalse();
    }

    private CategoryGroup facebook() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.URN_FACEBOOK, 2);
    }

    private CategoryGroup music() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.URN_MUSIC, 3);
    }

    private CategoryGroup audio() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.URN_SPEECH_AND_SOUNDS, 4);
    }

    private CategoryGroup emptyAudio() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.URN_SPEECH_AND_SOUNDS, 0);
    }

    private SuggestedUser buildUser(String name) {
        SuggestedUser user = new SuggestedUser();
        user.setUsername(name);
        return user;
    }

    private void addAllSections() throws CreateModelException {
        adapter.addItem(audio());
        adapter.addItem(music());
        adapter.addItem(facebook());
    }
}
