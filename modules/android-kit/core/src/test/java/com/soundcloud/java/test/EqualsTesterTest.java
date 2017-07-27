/*
 * Copyright (C) 2007 The Guava Authors
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

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link EqualsTester}.
 *
 * @author Jim McMaster
 */
public class EqualsTesterTest {
    private ValidTestObject reference;
    private EqualsTester equalsTester;
    private ValidTestObject equalObject1;
    private ValidTestObject equalObject2;
    private ValidTestObject notEqualObject1;

    @Before
    public void setUp() throws Exception {
        reference = new ValidTestObject(1, 2);
        equalsTester = new EqualsTester();
        equalObject1 = new ValidTestObject(1, 2);
        equalObject2 = new ValidTestObject(1, 2);
        notEqualObject1 = new ValidTestObject(0, 2);
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullReference() {
        equalsTester.addEqualityGroup((Object) null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddTwoEqualObjectsAtOnceWithNull() {
        equalsTester.addEqualityGroup(reference, equalObject1, null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullEqualObject() {
        equalsTester.addEqualityGroup(reference, null);
    }

    /**
     * Test adding objects only by addEqualityGroup, with no reference object
     * specified in the constructor.
     */
    @Test
    public void testAddEqualObjectWithOArgConstructor() {
        equalsTester.addEqualityGroup(equalObject1, notEqualObject1);
        try {
            equalsTester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e,
                    equalObject1 + " [group 1, item 1] must be Object#equals to "
                            + notEqualObject1 + " [group 1, item 2]");
            return;
        }
        fail("Should get not equal to equal object error");
    }

    /**
     * Test EqualsTester with no equals or not equals objects.  This checks
     * proper handling of null, incompatible class and reflexive tests
     */
    @Test
    public void testTestEqualsEmptyLists() {
        equalsTester.addEqualityGroup(reference);
        equalsTester.testEquals();
    }

    /**
     * Test EqualsTester after populating equalObjects.  This checks proper
     * handling of equality and verifies hashCode for valid objects
     */
    @Test
    public void testTestEqualsEqualsObjects() {
        equalsTester.addEqualityGroup(reference, equalObject1, equalObject2);
        equalsTester.testEquals();
    }

    /**
     * Test proper handling of case where an object is not equal to itself
     */
    @Test
    public void testNonreflexiveEquals() {
        Object obj = new NonReflexiveObject();
        equalsTester.addEqualityGroup(obj);
        try {
            equalsTester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e, obj + " must be Object#equals to itself");
            return;
        }
        fail("Should get non-reflexive error");
    }

    /**
     * Test proper handling where an object tests equal to null
     */
    @Test
    public void testInvalidEqualsNull() {
        Object obj = new InvalidEqualsNullObject();
        equalsTester.addEqualityGroup(obj);
        try {
            equalsTester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e, obj + " must not be Object#equals to null");
            return;
        }
        fail("Should get equal to null error");
    }

    /**
     * Test proper handling where an object incorrectly tests for an
     * incompatible class
     */
    @Test
    public void testInvalidEqualsIncompatibleClass() {
        Object obj = new InvalidEqualsIncompatibleClassObject();
        equalsTester.addEqualityGroup(obj);
        try {
            equalsTester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e,
                    obj
                            + " must not be Object#equals to an arbitrary object of another class");
            return;
        }
        fail("Should get equal to incompatible class error");
    }

    /**
     * Test proper handling where an object is not equal to one the user has
     * said should be equal
     */
    @Test
    public void testInvalidNotEqualsEqualObject() {
        equalsTester.addEqualityGroup(reference, notEqualObject1);
        try {
            equalsTester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(e, reference + " [group 1, item 1]");
            assertErrorMessage(e, notEqualObject1 + " [group 1, item 2]");
            return;
        }
        fail("Should get not equal to equal object error");
    }

    /**
     * Test for an invalid hashCode method, i.e., one that returns different
     * value for objects that are equal according to the equals method
     */
    @Test
    public void testInvalidHashCode() {
        Object a = new InvalidHashCodeObject(1, 2);
        Object b = new InvalidHashCodeObject(1, 2);
        equalsTester.addEqualityGroup(a, b);
        try {
            equalsTester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e, "the Object#hashCode (" + a.hashCode() + ") of " + a
                            + " [group 1, item 1] must be equal to the Object#hashCode ("
                            + b.hashCode() + ") of " + b);
            return;
        }
        fail("Should get invalid hashCode error");
    }

    @Test(expected = NullPointerException.class)
    public void testNullEqualityGroup() {
        EqualsTester tester = new EqualsTester();
        tester.addEqualityGroup((Object[]) null);
    }

    @Test
    public void testNullObjectInEqualityGroup() {
        EqualsTester tester = new EqualsTester();
        try {
            tester.addEqualityGroup(1, null, 3);
            fail();
        } catch (NullPointerException e) {
            assertErrorMessage(e, "at index 1");
        }
    }

    @Test
    public void testSymmetryBroken() {
        EqualsTester tester = new EqualsTester()
                .addEqualityGroup(named("foo").addPeers("bar"), named("bar"));
        try {
            tester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e,
                    "bar [group 1, item 2] must be Object#equals to foo [group 1, item 1]");
            return;
        }
        fail("should failed because symmetry is broken");
    }

    @Test
    public void testTransitivityBrokenInEqualityGroup() {
        EqualsTester tester = new EqualsTester()
                .addEqualityGroup(
                        named("foo").addPeers("bar", "baz"),
                        named("bar").addPeers("foo"),
                        named("baz").addPeers("foo"));
        try {
            tester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e,
                    "bar [group 1, item 2] must be Object#equals to baz [group 1, item 3]");
            return;
        }
        fail("should failed because transitivity is broken");
    }

    @Test
    public void testUnequalObjectsInEqualityGroup() {
        EqualsTester tester = new EqualsTester()
                .addEqualityGroup(named("foo"), named("bar"));
        try {
            tester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e,
                    "foo [group 1, item 1] must be Object#equals to bar [group 1, item 2]");
            return;
        }
        fail("should failed because of unequal objects in the same equality group");
    }

    @Test
    public void testTransitivityBrokenAcrossEqualityGroups() {
        EqualsTester tester = new EqualsTester()
                .addEqualityGroup(
                        named("foo").addPeers("bar"),
                        named("bar").addPeers("foo", "x"))
                .addEqualityGroup(
                        named("baz").addPeers("x"),
                        named("x").addPeers("baz", "bar"));
        try {
            tester.testEquals();
        } catch (AssertionError e) {
            assertErrorMessage(
                    e,
                    "bar [group 1, item 2] must not be Object#equals to x [group 2, item 2]");
            return;
        }
        fail("should failed because transitivity is broken");
    }

    @Test
    public void testEqualityGroups() {
        new EqualsTester()
                .addEqualityGroup(
                        named("foo").addPeers("bar"), named("bar").addPeers("foo"))
                .addEqualityGroup(named("baz"), named("baz"))
                .testEquals();
    }

    private static void assertErrorMessage(Throwable e, String message) {
        // TODO(kevinb): use a Truth assertion here
        if (!e.getMessage().contains(message)) {
            fail("expected <" + e.getMessage() + "> to contain <" + message + ">");
        }
    }

    /**
     * Test class with valid equals and hashCode methods.  Testers created
     * with instances of this class should always pass.
     */
    private static class ValidTestObject {
        private int aspect1;
        private int aspect2;

        ValidTestObject(int aspect1, int aspect2) {
            this.aspect1 = aspect1;
            this.aspect2 = aspect2;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ValidTestObject)) {
                return false;
            }
            ValidTestObject other = (ValidTestObject) o;
            if (aspect1 != other.aspect1) {
                return false;
            }
            if (aspect2 != other.aspect2) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 37 * result + aspect1;
            result = 37 * result + aspect2;
            return result;
        }
    }

    /**
     * Test class with invalid hashCode method.
     */
    private static class InvalidHashCodeObject {
        private int aspect1;
        private int aspect2;

        InvalidHashCodeObject(int aspect1, int aspect2) {
            this.aspect1 = aspect1;
            this.aspect2 = aspect2;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof InvalidHashCodeObject)) {
                return false;
            }
            InvalidHashCodeObject other = (InvalidHashCodeObject) o;
            if (aspect1 != other.aspect1) {
                return false;
            }
            if (aspect2 != other.aspect2) {
                return false;
            }
            return true;
        }
    }

    /**
     * Test class that violates reflexitivity.  It is not equal to itself
     */
    private static class NonReflexiveObject {

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    /**
     * Test class that returns true if the test object is null
     */
    private static class InvalidEqualsNullObject {

        @Override
        public boolean equals(Object o) {
            return o == this || o == null;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    /**
     * Test class that returns true even if the test object is of the wrong class
     */
    private static class InvalidEqualsIncompatibleClassObject {

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static NamedObject named(String name) {
        return new NamedObject(name);
    }

    private static class NamedObject {
        private final Set<String> peerNames = new HashSet<>();

        private final String name;

        NamedObject(String name) {
            this.name = name;
        }

        NamedObject addPeers(String... names) {
            peerNames.addAll(Arrays.asList(names));
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NamedObject) {
                NamedObject that = (NamedObject) obj;
                return name.equals(that.name) || peerNames.contains(that.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
