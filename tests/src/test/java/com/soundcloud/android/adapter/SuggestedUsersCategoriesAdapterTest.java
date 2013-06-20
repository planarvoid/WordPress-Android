package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.operations.following.FollowStatus;
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

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoriesAdapterTest {

    private SuggestedUsersCategoriesAdapter adapter;
    private SuggestedUsersCategoriesAdapter nonFacebookAdapter;
    @Mock
    private FollowStatus followStatus;

    @Before
    public void setup() throws CreateModelException {
        initMocks(this);
        adapter = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS, followStatus);
        nonFacebookAdapter = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_EXCEPT_FACEBOOK, followStatus);
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
        expect(adapter.getItem(0).getType()).toBe(Category.Type.PROGRESS);
    }

    @Test
    public void shouldHaveMusicLoadingSectionAndNotSpeechAndSoundsLoadingsection() {
        adapter.addItem(new CategoryGroup(CategoryGroup.KEY_FACEBOOK));
        expect(adapter.getCount()).toBe(2);
        expect(adapter.getItem(0).getType()).not.toBe(Category.Type.PROGRESS);
        expect(adapter.getItem(1).getType()).toBe(Category.Type.PROGRESS);
    }

    @Test
    public void shouldHandleUnexpectedSection() throws CreateModelException {
        nonFacebookAdapter.addItem(facebook());
        expect(nonFacebookAdapter.getCount()).toBe(3); // 2 facebook sections, 1 loading section
        expect(nonFacebookAdapter.getItem(0).getType()).not.toBe(Category.Type.PROGRESS);
        expect(nonFacebookAdapter.getItem(1).getType()).not.toBe(Category.Type.PROGRESS);
        expect(nonFacebookAdapter.getItem(2).getType()).toBe(Category.Type.PROGRESS);
    }

    @Test
    public void addItemShouldReplaceProgressItems() throws CreateModelException {
        adapter.addItem(audio());
        adapter.addItem(music());
        adapter.addItem(facebook());

        for (Category category : adapter.getItems()) {
            expect(category.getType()).not.toBe(Category.Type.PROGRESS);
        }
    }

    @Test
    public void addItemShouldReplaceDummySections() throws CreateModelException {
        adapter.addItem(emptyAudio());
        adapter.addItem(music());

        expect(adapter.getItem(0).getType()).toBe(Category.Type.PROGRESS);
        expect(adapter.getItem(1).getType()).not.toBe(Category.Type.PROGRESS);
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
        Map<Integer, SuggestedUsersCategoriesAdapter.Section> sectionMap = adapter.getListPositionsToSectionsMap();
        expect(sectionMap).not.toBeNull();
        expect(sectionMap.values()).toContainExactly(SuggestedUsersCategoriesAdapter.Section.FACEBOOK, SuggestedUsersCategoriesAdapter.Section.MUSIC, SuggestedUsersCategoriesAdapter.Section.SPEECH_AND_SOUNDS);
    }

    @Test
    public void shouldGetViewWithHeader() throws CreateModelException {
        addAllSections();
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
        SuggestedUser followedUser = TestHelper.getModelFactory().createModel(SuggestedUser.class);
        when(followStatus.getFollowedUserIds()).thenReturn(Sets.newHashSet(followedUser.getId()));

        Category category = adapter.getItem(0);
        category.getUsers().add(followedUser);
        expect(adapter.getSubtextUsers(category)).toContainExactly(followedUser.getUsername());
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
