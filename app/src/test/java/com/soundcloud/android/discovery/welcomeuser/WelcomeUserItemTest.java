package com.soundcloud.android.discovery.welcomeuser;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WelcomeUserItemTest extends AndroidUnitTest {

    private static final String FIRST_NAME = "FirstName";
    private final Date fiveHoursAgo = new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(5, TimeUnit.HOURS));
    private final Date now = new Date();

    @Test
    public void newUser() throws Exception {
        User user = ModelFixtures.userBuilder(false)
                                 .signupDate(Optional.of(now))
                                 .build();
        assertThat(createWelcomeUserItem(user).isNewSignup()).isTrue();
    }

    @Test
    public void notNewUser() throws Exception {
        User user = ModelFixtures.userBuilder(false)
                                 .signupDate(Optional.of(fiveHoursAgo))
                                 .build();
        assertThat(createWelcomeUserItem(user).isNewSignup()).isFalse();
    }

    @Test
    public void usesUsername() throws Exception {
        User user = ModelFixtures.userBuilder(false)
                                 .firstName(Optional.absent())
                                 .build();
        assertThat(createWelcomeUserItem(user).userName()).isEqualTo(user.username());
    }

    @Test
    public void usesFirstName() throws Exception {
        User user = ModelFixtures.userBuilder(false)
                                 .firstName(Optional.of(FIRST_NAME))
                                 .build();
        assertThat(createWelcomeUserItem(user).userName()).isEqualTo(FIRST_NAME);
    }

    private WelcomeUserItem createWelcomeUserItem(User user) {
        return (WelcomeUserItem) WelcomeUserItem.create(user);
    }
}
