package com.soundcloud.android.discovery.welcomeuser;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.profile.ProfileUser;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WelcomeUserItemTest extends AndroidUnitTest {

    private static final String FIRST_NAME = "FirstName";
    private final Date fiveHoursAgo = new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(5, TimeUnit.HOURS));
    private Date now = new Date();

    @Test
    public void newUser() throws Exception {
        ProfileUser profileUser = ModelFixtures.profileUser(now);
        assertThat(createWelcomeUserItem(profileUser).isNewSignup()).isTrue();
    }

    @Test
    public void notNewUser() throws Exception {
        ProfileUser profileUser = ModelFixtures.profileUser(fiveHoursAgo);
        assertThat(createWelcomeUserItem(profileUser).isNewSignup()).isFalse();
    }

    @Test
    public void usesUsername() throws Exception {
        ProfileUser profileUser = ModelFixtures.profileUserWithName(null);
        assertThat(createWelcomeUserItem(profileUser).userName().startsWith("user")).isTrue();
    }

    @Test
    public void usesFirstName() throws Exception {
        ProfileUser profileUser = ModelFixtures.profileUserWithName(FIRST_NAME);
        assertThat(createWelcomeUserItem(profileUser).userName()).isEqualTo(FIRST_NAME);
    }

    private WelcomeUserItem createWelcomeUserItem(ProfileUser profileUser) {
        return (WelcomeUserItem) WelcomeUserItem.create(profileUser);
    }
}
