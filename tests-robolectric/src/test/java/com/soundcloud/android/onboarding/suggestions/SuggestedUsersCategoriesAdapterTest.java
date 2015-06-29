package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.util.SparseArray;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoriesAdapterTest {

    private SuggestedUsersCategoriesAdapter adapter;
    private SuggestedUsersCategoriesAdapter nonFacebookAdapter;

    @Mock
    private FollowingOperations followingOperations;

    @Before
    public void setup() throws CreateModelException {
        adapter = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS, followingOperations);
        nonFacebookAdapter = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_EXCEPT_FACEBOOK, followingOperations);
    }

    @Test
    public void shouldHaveNoSectionsAtStart() {
        expect(adapter.getCount()).toBe(0);
    }

    @Test
    public void shouldNotHaveFacebookLoadingSection() {
        nonFacebookAdapter.addItem(new CategoryGroup(CategoryGroup.KEY_MUSIC));
        nonFacebookAdapter.addItem(new CategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS));
        expect(nonFacebookAdapter.getCount() ).toBe(2);
    }

    @Test
    public void shouldHaveFacebookLoadingSection() {
        adapter.addItem(new CategoryGroup(CategoryGroup.KEY_MUSIC));
        adapter.addItem(new CategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS));
        expect(adapter.getCount()).toBe(3);
        expect(adapter.getItem(0).getDisplayType()).toBe(Category.DisplayType.PROGRESS);
    }

    @Test
    public void shouldHaveMusicLoadingSectionAndNotSpeechAndSoundsLoadingsection() {
        adapter.addItem(new CategoryGroup(CategoryGroup.KEY_FACEBOOK));
        expect(adapter.getCount()).toBe(2);
        expect(adapter.getItem(0).getDisplayType()).not.toBe(Category.DisplayType.PROGRESS);
        expect(adapter.getItem(1).getDisplayType()).toBe(Category.DisplayType.PROGRESS);
    }

    @Test
    public void shouldHandleUnexpectedSection() throws CreateModelException {
        nonFacebookAdapter.addItem(facebook());
        expect(nonFacebookAdapter.getCount()).toBe(3); // 2 facebook sections, 1 loading section
        expect(nonFacebookAdapter.getItem(0).getDisplayType()).not.toBe(Category.DisplayType.PROGRESS);
        expect(nonFacebookAdapter.getItem(1).getDisplayType()).not.toBe(Category.DisplayType.PROGRESS);
        expect(nonFacebookAdapter.getItem(2).getDisplayType()).toBe(Category.DisplayType.PROGRESS);
    }

    @Test
    public void addItemShouldReplaceProgressItems() throws CreateModelException {
        adapter.addItem(audio());
        adapter.addItem(music());
        adapter.addItem(facebook());

        for (Category category : adapter.getItems()) {
            expect(category.getDisplayType()).not.toBe(Category.DisplayType.PROGRESS);
        }
    }

    @Test
    public void addItemShouldReplaceDummySections() throws CreateModelException {
        adapter.addItem(emptyAudio());
        adapter.addItem(music());

        expect(adapter.getItem(0).getDisplayType()).toBe(Category.DisplayType.PROGRESS);
        expect(adapter.getItem(1).getDisplayType()).not.toBe(Category.DisplayType.PROGRESS);
        expect(adapter.getItem(1).isErrorOrEmpty()).toBeFalse();
        expect(adapter.getItem(music().getCategoryCount() + 1).isErrorOrEmpty()).toBeTrue();
    }

    @Test
    public void emptyCategoryItemsShouldNotBeEnabled() throws CreateModelException {
        adapter.addItem(audio());
        expect(adapter.isEnabled(0)).toBeFalse();
    }

    @Test
    public void shouldCountItems() throws CreateModelException {
        // initially, we only have 3 dummy items
        expect(adapter.getCount()).toBe(0);

        // 1 completed section, 1 loading section for audio
        adapter.addItem(facebook());
        expect(adapter.getCount()).toBe(1 + facebook().getCategoryCount());

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
        addAllSections();
        SparseArray<SuggestedUsersCategoriesAdapter.Section> sectionMap = adapter.getListPositionsToSectionsMap();
        expect(sectionMap).not.toBeNull();
        expect(sectionMap.get(0)).toBe(SuggestedUsersCategoriesAdapter.Section.FACEBOOK);
        expect(sectionMap.get(2)).toBe(SuggestedUsersCategoriesAdapter.Section.MUSIC);
        expect(sectionMap.get(5)).toBe(SuggestedUsersCategoriesAdapter.Section.SPEECH_AND_SOUNDS);
    }

    @Test
    public void shouldGetViewWithHeader() throws CreateModelException {
        addAllSections();
        final int positionWithHeader = 0;
        View itemLayout = adapter.getView(positionWithHeader, null, new FrameLayout(Robolectric.application));

        expect(itemLayout).not.toBeNull();
        View headerView = itemLayout.findViewById(R.id.list_section_header);
        expect(headerView).not.toBeNull();
        expect(headerView.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldGetViewWithoutHeader() throws CreateModelException {
        addAllSections();
        final int positionWithoutHeader = 1;
        View itemLayout = adapter.getView(positionWithoutHeader, null, new FrameLayout(Robolectric.application));

        expect(itemLayout).not.toBeNull();
        View headerView = itemLayout.findViewById(R.id.list_section_header);
        expect(headerView).not.toBeNull();
        expect(headerView.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldGetCorrectUserlistForSingleUser() throws CreateModelException {
        addAllSections();
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex")));
        expect(adapter.getSubtextUsers(bucket)).toContainExactly("Skrillex");
    }

    @Test
    public void shouldGetCorrectUserlistForTwoUsers() throws CreateModelException {
        addAllSections();
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex"), buildUser("Forss")));
        expect(adapter.getSubtextUsers(bucket)).toContainExactly("Skrillex", "Forss");
    }

    @Test
    public void shouldGetCorrectUserlistForMultipleUsers() throws CreateModelException {
        addAllSections();
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(
                buildUser("Skrillex"), buildUser("Forss"), buildUser("Rick Astley")));
        expect(adapter.getSubtextUsers(bucket)).toContainExactly("Skrillex", "Forss", "Rick Astley");
    }

    @Test
    public void shouldGetCorrectUserlistForMultipleCategoryUsersWithOneFollowing() throws CreateModelException {
        addAllSections();
        SuggestedUser followedUser = ModelFixtures.create(SuggestedUser.class);
        when(followingOperations.getFollowedUserIds()).thenReturn(Collections.singleton(followedUser.getId()));

        Category category = adapter.getItem(0);
        category.getUsers().add(followedUser);
        expect(adapter.getSubtextUsers(category)).toContainExactly(followedUser.getUsername());
    }

    @Test
    public void shouldCheckFollowButtonIfAtLeastOneUserIsFollowed() throws CreateModelException {
        addAllSections();
        SuggestedUser followedUser = ModelFixtures.create(SuggestedUser.class);
        when(followingOperations.getFollowedUserIds()).thenReturn(Collections.singleton(followedUser.getId()));

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

    @Test
    public void shouldFollowBySuggestedUsersOnClick() throws CreateModelException {
        nonFacebookAdapter.addItem(audio());
        nonFacebookAdapter.addItem(music());
        List<SuggestedUser> users = nonFacebookAdapter.getItem(0).getUsers();
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(followingOperations.addFollowingsBySuggestedUsers(users)).thenReturn(observable);

        View itemLayout = nonFacebookAdapter.getView(0, null, new FrameLayout(Robolectric.application));
        itemLayout.findViewById(R.id.btn_user_bucket_select_all).performClick();

        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldRemoveDuplicateUsers() throws CreateModelException {
        CategoryGroup cat1 = TestHelper.buildCategoryGroup(CategoryGroup.KEY_MUSIC, 2);
        nonFacebookAdapter.addItem(cat1);

        CategoryGroup cat2 = TestHelper.buildCategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS, 2);
        // make the first user in the second group be the same as the first user in the first group
        cat2.getCategories().get(0).getUsers().set(0,cat1.getCategories().get(0).getUsers().get(0));
        expect(cat2.getCategories().get(0).getUsers().size()).toBe(3); // untouched category

        nonFacebookAdapter.addItem(cat2);
        expect(cat2.getCategories().get(0).getUsers().size()).toBe(2); // one removed user
    }

    private CategoryGroup facebook() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_FACEBOOK, 2);
    }

    private CategoryGroup music() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_MUSIC, 3);
    }

    private CategoryGroup audio() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS, 4);
    }

    private CategoryGroup emptyAudio() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS, 0);
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
