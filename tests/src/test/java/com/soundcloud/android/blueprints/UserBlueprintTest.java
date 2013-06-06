package com.soundcloud.android.blueprints;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UserBlueprintTest {

    @Test
    public void shouldGenerateUsersWithRunningIds() throws CreateModelException {
        ModelFactory modelFactory = TestHelper.getModelFactory();

        User user1 = modelFactory.createModel(User.class);
        User user2 = modelFactory.createModel(User.class);
        User user3 = modelFactory.createModel(User.class);

        expect(user1.getId()).toBeGreaterThan(0L);
        expect(user2.getId()).toBe(user1.getId() + 1);
        expect(user3.getId()).toBe(user2.getId() + 1);
    }

    @Test
    public void shouldGenerateNewUsersWithUniqueUsernames() throws CreateModelException {
        ModelFactory modelFactory = TestHelper.getModelFactory();

        User user1 = modelFactory.createModel(User.class);
        User user2 = modelFactory.createModel(User.class);

        expect(user1.getUsername()).toEqual("user" + user1.getId());
        expect(user2.getUsername()).toEqual("user" + user2.getId());
    }

}
