/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.java.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.Serializable;

/**
 * Tests for {@link SerializableTester}.
 *
 * @author Nick Kralevich
 */
public class SerializableTesterTest {

    @Test
    public void testStringAssertions() {
        String original = "hello world";
        String copy = SerializableTester.reserializeAndAssert(original);
        assertEquals(original, copy);
        assertNotSame(original, copy);
    }

    @Test
    public void testClassWhichDoesNotImplementEquals() {
        ClassWhichDoesNotImplementEquals orig =
                new ClassWhichDoesNotImplementEquals();
        boolean errorNotThrown = false;
        try {
            SerializableTester.reserializeAndAssert(orig);
            errorNotThrown = true;
        } catch (AssertionError error) {
            // expected
            assertContains("must be Object#equals to", error.getMessage());
        }
        assertFalse(errorNotThrown);
    }

    @Test
    public void testClassWhichIsAlwaysEqualButHasDifferentHashcodes() {
        ClassWhichIsAlwaysEqualButHasDifferentHashcodes orig =
                new ClassWhichIsAlwaysEqualButHasDifferentHashcodes();
        boolean errorNotThrown = false;
        try {
            SerializableTester.reserializeAndAssert(orig);
            errorNotThrown = true;
        } catch (AssertionError error) {
            // expected
            assertContains("must be equal to the Object#hashCode", error.getMessage());
        }
        assertFalse(errorNotThrown);
    }

    @Test
    public void testObjectWhichIsEqualButChangesClass() {
        ObjectWhichIsEqualButChangesClass orig =
                new ObjectWhichIsEqualButChangesClass();
        boolean errorNotThrown = false;
        try {
            SerializableTester.reserializeAndAssert(orig);
            errorNotThrown = true;
        } catch (AssertionError error) {
            // expected
            assertContains("expected:<class ", error.getMessage());
        }
        assertFalse(errorNotThrown);
    }

    private static class ClassWhichDoesNotImplementEquals
            implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private static class ClassWhichIsAlwaysEqualButHasDifferentHashcodes
            implements Serializable {
        private static final long serialVersionUID = 2L;

        @Override
        public boolean equals(Object other) {
            return (other instanceof ClassWhichIsAlwaysEqualButHasDifferentHashcodes);
        }
    }

    private static class ObjectWhichIsEqualButChangesClass
            implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean equals(Object other) {
            return (other instanceof ObjectWhichIsEqualButChangesClass
                    || other instanceof OtherForm);
        }

        @Override
        public int hashCode() {
            return 1;
        }

        private Object writeReplace() {
            return new OtherForm();
        }

        private static class OtherForm implements Serializable {
            @Override
            public boolean equals(Object other) {
                return (other instanceof ObjectWhichIsEqualButChangesClass
                        || other instanceof OtherForm);
            }

            @Override
            public int hashCode() {
                return 1;
            }
        }
    }

    private static void assertContains(String expectedSubstring, String actual) {
        // TODO(kevinb): use a Truth assertion here
        if (!actual.contains(expectedSubstring)) {
            fail("expected <" + actual + "> to contain <" + expectedSubstring + ">");
        }
    }
}
