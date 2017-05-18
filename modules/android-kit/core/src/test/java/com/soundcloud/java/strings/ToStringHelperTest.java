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

package com.soundcloud.java.strings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.soundcloud.java.objects.MoreObjects;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link MoreObjects#toStringHelper(Object)}.
 *
 * @author Jason Lee
 */
public class ToStringHelperTest {

    @Test
    public void testConstructor_instance() {
        String toTest = MoreObjects.toStringHelper(this).toString();
        assertEquals("ToStringHelperTest{}", toTest);
    }

    @Test
    public void testConstructorLenient_instance() {
        String toTest = MoreObjects.toStringHelper(this).toString();
        assertTrue(toTest, toTest.matches(".*\\{\\}"));
    }

    @Test
    public void testConstructor_innerClass() {
        String toTest = MoreObjects.toStringHelper(new TestClass()).toString();
        assertEquals("TestClass{}", toTest);
    }

    @Test
    public void testConstructorLenient_innerClass() {
        String toTest = MoreObjects.toStringHelper(new TestClass()).toString();
        assertTrue(toTest, toTest.matches(".*\\{\\}"));
    }

    @Test
    public void testConstructor_anonymousClass() {
        String toTest = MoreObjects.toStringHelper(new Object() {
        }).toString();
        assertEquals("{}", toTest);
    }

    @Test
    public void testConstructorLenient_anonymousClass() {
        String toTest = MoreObjects.toStringHelper(new Object() {
        }).toString();
        assertTrue(toTest, toTest.matches(".*\\{\\}"));
    }

    @Test
    public void testConstructor_classObject() {
        String toTest = MoreObjects.toStringHelper(TestClass.class).toString();
        assertEquals("TestClass{}", toTest);
    }

    @Test
    public void testConstructorLenient_classObject() {
        String toTest = MoreObjects.toStringHelper(TestClass.class).toString();
        assertTrue(toTest, toTest.matches(".*\\{\\}"));
    }

    @Test
    public void testConstructor_stringObject() {
        String toTest = MoreObjects.toStringHelper("FooBar").toString();
        assertEquals("FooBar{}", toTest);
    }

    @Test
    public void testToStringHelper_localInnerClass() {
        // Local inner classes have names ending like "Outer.$1Inner"
        class LocalInnerClass {
        }
        String toTest = MoreObjects.toStringHelper(new LocalInnerClass()).toString();
        assertEquals("LocalInnerClass{}", toTest);
    }

    @Test
    public void testToStringHelperLenient_localInnerClass() {
        class LocalInnerClass {
        }
        String toTest = MoreObjects.toStringHelper(new LocalInnerClass()).toString();
        assertTrue(toTest, toTest.matches(".*\\{\\}"));
    }

    @Test
    public void testToStringHelper_localInnerNestedClass() {
        class LocalInnerClass {
            class LocalInnerNestedClass {
            }
        }
        String toTest = MoreObjects.toStringHelper(new LocalInnerClass().new LocalInnerNestedClass())
                .toString();
        assertEquals("LocalInnerNestedClass{}", toTest);
    }

    @Test
    public void testToStringHelperLenient_localInnerNestedClass() {
        class LocalInnerClass {
            class LocalInnerNestedClass {
            }
        }
        String toTest = MoreObjects.toStringHelper(new LocalInnerClass().new LocalInnerNestedClass())
                .toString();
        assertTrue(toTest, toTest.matches(".*\\{\\}"));
    }

    @Test
    public void testToStringHelper_moreThanNineAnonymousClasses() {
        // The nth anonymous class has a name ending like "Outer.$n"
        Object o1 = new Object() {
        };
        Object o2 = new Object() {
        };
        Object o3 = new Object() {
        };
        Object o4 = new Object() {
        };
        Object o5 = new Object() {
        };
        Object o6 = new Object() {
        };
        Object o7 = new Object() {
        };
        Object o8 = new Object() {
        };
        Object o9 = new Object() {
        };
        Object o10 = new Object() {
        };
        String toTest = MoreObjects.toStringHelper(o10).toString();
        assertEquals("{}", toTest);
    }

    @Test
    public void testToStringHelperLenient_moreThanNineAnonymousClasses() {
        // The nth anonymous class has a name ending like "Outer.$n"
        Object o1 = new Object() {
        };
        Object o2 = new Object() {
        };
        Object o3 = new Object() {
        };
        Object o4 = new Object() {
        };
        Object o5 = new Object() {
        };
        Object o6 = new Object() {
        };
        Object o7 = new Object() {
        };
        Object o8 = new Object() {
        };
        Object o9 = new Object() {
        };
        Object o10 = new Object() {
        };
        String toTest = MoreObjects.toStringHelper(o10).toString();
        assertTrue(toTest, toTest.matches(".*\\{\\}"));
    }

    // all remaining test are on an inner class with various fields
    @Test
    public void testToString_oneField() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", "Hello")
                .toString();
        assertEquals("TestClass{field1=Hello}", toTest);
    }

    @Test
    public void testToString_oneIntegerField() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", Integer.valueOf(42))
                .toString();
        assertEquals("TestClass{field1=42}", toTest);
    }

    @Test
    public void testToString_nullInteger() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", null)
                .toString();
        assertEquals("TestClass{field1=null}", toTest);
    }

    @Test
    public void testToStringLenient_oneField() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", "Hello")
                .toString();
        assertTrue(toTest, toTest.matches(".*\\{field1\\=Hello\\}"));
    }

    @Test
    public void testToStringLenient_oneIntegerField() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", Integer.valueOf(42))
                .toString();
        assertTrue(toTest, toTest.matches(".*\\{field1\\=42\\}"));
    }

    @Test
    public void testToStringLenient_nullInteger() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", null)
                .toString();
        assertTrue(toTest, toTest.matches(".*\\{field1\\=null\\}"));
    }

    @Test
    public void testToString_complexFields() {

        Map<String, Integer> map = new HashMap<>();
        map.put("abc", 1);
        map.put("def", 2);
        map.put("ghi", 3);
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", "This is string.")
                .add("field2", Arrays.asList("abc", "def", "ghi"))
                .add("field3", map)
                .toString();
        final String expected = "TestClass{"
                + "field1=This is string., field2=[abc, def, ghi], field3={abc=1, def=2, ghi=3}}";

        assertEquals(expected, toTest);
    }

    @Test
    public void testToStringLenient_complexFields() {

        Map<String, Integer> map = new HashMap<>();
        map.put("abc", 1);
        map.put("def", 2);
        map.put("ghi", 3);
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", "This is string.")
                .add("field2", Arrays.asList("abc", "def", "ghi"))
                .add("field3", map)
                .toString();
        final String expectedRegex = ".*\\{"
                + "field1\\=This is string\\., "
                + "field2\\=\\[abc, def, ghi\\], "
                + "field3=\\{abc\\=1, def\\=2, ghi\\=3\\}\\}";

        assertTrue(toTest, toTest.matches(expectedRegex));
    }

    @Test
    public void testToString_addWithNullName() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(new TestClass());
        try {
            helper.add(null, "Hello");
            fail("No exception was thrown.");
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testToString_addWithNullValue() {
        final String result = MoreObjects.toStringHelper(new TestClass())
                .add("Hello", null)
                .toString();

        assertEquals("TestClass{Hello=null}", result);
    }

    @Test
    public void testToStringLenient_addWithNullValue() {
        final String result = MoreObjects.toStringHelper(new TestClass())
                .add("Hello", null)
                .toString();
        assertTrue(result, result.matches(".*\\{Hello\\=null\\}"));
    }

    @Test
    public void testToString_ToStringTwice() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(new TestClass())
                .add("field1", 1)
                .addValue("value1")
                .add("field2", "value2");
        final String expected = "TestClass{field1=1, value1, field2=value2}";

        assertEquals(expected, helper.toString());
        // Call toString again
        assertEquals(expected, helper.toString());

        // Make sure the cached value is reset when we modify the helper at all
        final String expected2 = "TestClass{field1=1, value1, field2=value2, 2}";
        helper.addValue(2);
        assertEquals(expected2, helper.toString());
    }

    @Test
    public void testToString_addValue() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", 1)
                .addValue("value1")
                .add("field2", "value2")
                .addValue(2)
                .toString();
        final String expected = "TestClass{field1=1, value1, field2=value2, 2}";

        assertEquals(expected, toTest);
    }

    @Test
    public void testToStringLenient_addValue() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", 1)
                .addValue("value1")
                .add("field2", "value2")
                .addValue(2)
                .toString();
        final String expected = ".*\\{field1\\=1, value1, field2\\=value2, 2\\}";

        assertTrue(toTest, toTest.matches(expected));
    }

    @Test
    public void testToString_addValueWithNullValue() {
        final String result = MoreObjects.toStringHelper(new TestClass())
                .addValue(null)
                .addValue("Hello")
                .addValue(null)
                .toString();
        final String expected = "TestClass{null, Hello, null}";

        assertEquals(expected, result);
    }

    @Test
    public void testToStringLenient_addValueWithNullValue() {
        final String result = MoreObjects.toStringHelper(new TestClass())
                .addValue(null)
                .addValue("Hello")
                .addValue(null)
                .toString();
        final String expected = ".*\\{null, Hello, null\\}";

        assertTrue(result, result.matches(expected));
    }

    @Test
    public void testToStringOmitNullValues_oneField() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .add("field1", null)
                .toString();
        assertEquals("TestClass{}", toTest);
    }

    @Test
    public void testToStringOmitNullValues_manyFieldsFirstNull() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .add("field1", null)
                .add("field2", "Googley")
                .add("field3", "World")
                .toString();
        assertEquals("TestClass{field2=Googley, field3=World}", toTest);
    }

    @Test
    public void testToStringOmitNullValues_manyFieldsOmitAfterNull() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .add("field1", null)
                .add("field2", "Googley")
                .add("field3", "World")
                .omitNullValues()
                .toString();
        assertEquals("TestClass{field2=Googley, field3=World}", toTest);
    }

    @Test
    public void testToStringOmitNullValues_manyFieldsLastNull() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .add("field1", "Hello")
                .add("field2", "Googley")
                .add("field3", null)
                .toString();
        assertEquals("TestClass{field1=Hello, field2=Googley}", toTest);
    }

    @Test
    public void testToStringOmitNullValues_oneValue() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .addValue(null)
                .toString();
        assertEquals("TestClass{}", toTest);
    }

    @Test
    public void testToStringOmitNullValues_manyValuesFirstNull() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .addValue(null)
                .addValue("Googley")
                .addValue("World")
                .toString();
        assertEquals("TestClass{Googley, World}", toTest);
    }

    @Test
    public void testToStringOmitNullValues_manyValuesLastNull() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .addValue("Hello")
                .addValue("Googley")
                .addValue(null)
                .toString();
        assertEquals("TestClass{Hello, Googley}", toTest);
    }

    @Test
    public void testToStringOmitNullValues_differentOrder() {
        String expected = "TestClass{field1=Hello, field2=Googley, field3=World}";
        String toTest1 = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .add("field1", "Hello")
                .add("field2", "Googley")
                .add("field3", "World")
                .toString();
        String toTest2 = MoreObjects.toStringHelper(new TestClass())
                .add("field1", "Hello")
                .add("field2", "Googley")
                .omitNullValues()
                .add("field3", "World")
                .toString();
        assertEquals(expected, toTest1);
        assertEquals(expected, toTest2);
    }

    @Test
    public void testToStringOmitNullValues_canBeCalledManyTimes() {
        String toTest = MoreObjects.toStringHelper(new TestClass())
                .omitNullValues()
                .omitNullValues()
                .add("field1", "Hello")
                .omitNullValues()
                .add("field2", "Googley")
                .omitNullValues()
                .add("field3", "World")
                .toString();
        assertEquals("TestClass{field1=Hello, field2=Googley, field3=World}",
                toTest);
    }

    /**
     * Test class for testing formatting of inner classes.
     */
    private static class TestClass {
    }

}
