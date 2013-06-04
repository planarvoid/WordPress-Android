package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersAdapterTest {

    private SuggestedUsersAdapter adapter;

    @Before
    public void setup() throws CreateModelException {
        adapter = new SuggestedUsersAdapter();
        adapter.addItem(audio());
        adapter.addItem(music());
        adapter.addItem(facebook());
    }

    @Test
    public void shouldBuildListPositionsToSectionsMapWhileAddingNewItems() throws CreateModelException {
        Map<Integer, SuggestedUsersAdapter.Section> sectionMap = adapter.getListPositionsToSectionsMap();
        expect(sectionMap).not.toBeNull();
        expect(sectionMap.values()).toContainExactly(SuggestedUsersAdapter.Section.FACEBOOK, SuggestedUsersAdapter.Section.MUSIC, SuggestedUsersAdapter.Section.SPEECH_AND_SOUNDS);

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
    public void shouldGetViewWithoutHeader() {
        final int positionWithoutHeader = 1;
        View itemLayout = adapter.getView(positionWithoutHeader, null, new FrameLayout(Robolectric.application));

        expect(itemLayout).not.toBeNull();
        View headerView = itemLayout.findViewById(R.id.suggested_users_list_header);
        expect(headerView).not.toBeNull();
        expect(headerView.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldSetCorrectBucketTextForSingleUser() {
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex");
    }

    @Test
    public void shouldSetCorrectBucketTextForTwoUsers() {
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex"), buildUser("Forss")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex, Forss");
    }

    @Test
    public void shouldSetCorrectBucketTextForMultipleUsers() {
        Category bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(
                buildUser("Skrillex"), buildUser("Forss"), buildUser("Rick Astley")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex, Forss and 1 other");
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

    private User buildUser(String name) {
        User user = new User();
        user.username = name;
        return user;
    }
}
