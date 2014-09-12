package com.soundcloud.android.testsupport.blueprints;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UserBlueprintTest {

    @Test
    public void shouldGenerateUsersWithRunningIds() throws CreateModelException {
        ModelFactory modelFactory = TestHelper.getModelFactory();

        PublicApiUser user1 = modelFactory.createModel(PublicApiUser.class);
        PublicApiUser user2 = modelFactory.createModel(PublicApiUser.class);
        PublicApiUser user3 = modelFactory.createModel(PublicApiUser.class);

        expect(user1.getId()).toBeGreaterThan(0L);
        expect(user2.getId()).toEqual(user1.getId() + 1);
        expect(user3.getId()).toEqual(user2.getId() + 1);
    }

    @Test
    public void shouldGenerateNewUsersWithUniqueUsernames() throws CreateModelException {
        ModelFactory modelFactory = TestHelper.getModelFactory();

        PublicApiUser user1 = modelFactory.createModel(PublicApiUser.class);
        PublicApiUser user2 = modelFactory.createModel(PublicApiUser.class);

        expect(user1.getUsername()).toEqual("user" + user1.getId());
        expect(user2.getUsername()).toEqual("user" + user2.getId());
    }

}
