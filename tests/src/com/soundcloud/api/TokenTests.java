package com.soundcloud.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;


public class TokenTests {
    @Test
    public void shouldBeValid() throws Exception {
        Token t = new Token("1", "2");
        assertTrue(t.valid());
    }

    @Test
    public void emptyTokenshouldBeInValid() throws Exception {
        Token invalid = new Token(null, "2");
        assertFalse(invalid.valid());
    }

    @Test
    public void shouldDetectStarScope() throws Exception {
        Token t = new Token(null, null);
        assertFalse(t.starScoped());
        t.scope = "signup *";
        assertTrue(t.starScoped());
    }

    @Test
    public void shouldDetectSignupScope() throws Exception {
        Token t = new Token(null, null);
        assertFalse(t.starScoped());
        t.scope = "signup";
        assertTrue(t.signupScoped());
    }

    @Test
    public void shouldHaveProperEqualsMethod() throws Exception {
        Token t1 = new Token("1", "2");
        Token t2 = new Token("1", "2");
        assertEquals(t1, t2);
    }
}
