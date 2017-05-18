/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.java.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.java.reflect.Types;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public final class TypesTest {

    @Test
    public void testNewParameterizedTypeWithoutOwner() throws Exception {
        // List<A>. List is a top-level class
        Type type = Types.newParameterizedTypeWithOwner(null, List.class, A.class);
        assertEquals(A.class, getFirstTypeArgument(type));

        // A<B>. A is a static inner class.
        type = Types.newParameterizedTypeWithOwner(null, A.class, B.class);
        assertEquals(B.class, getFirstTypeArgument(type));

        final class D {
        }
        try {
            // D<A> is not allowed since D is not a static inner class
            Types.newParameterizedTypeWithOwner(null, D.class, A.class);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // A<D> is allowed.
        type = Types.newParameterizedTypeWithOwner(null, A.class, D.class);
        assertEquals(D.class, getFirstTypeArgument(type));
    }

    @Test
    public void testGetFirstTypeArgument() throws Exception {
        assertNull(getFirstTypeArgument(A.class));

        Type type = Types.newParameterizedTypeWithOwner(null, A.class, B.class, C.class);
        assertEquals(B.class, getFirstTypeArgument(type));
    }

    @Test
    public void testParameterizedType() throws Exception {
        ParameterizedType constructedType = Types.newParameterizedTypeWithOwner(null, List.class, String.class);
        Type actualType = new TypeToken<List<String>>() {
        }.getType();
        assertNull(constructedType.getOwnerType());
        assertEquals(String.class, constructedType.getActualTypeArguments()[0]);
        assertEquals(List.class, constructedType.getRawType());
        assertEquals(actualType, constructedType);
        assertEquals(actualType.hashCode(), constructedType.hashCode());
    }

    @Test
    public void testParameterizedTypeEquality() throws Exception {
        ParameterizedType constructedType = Types.newParameterizedTypeWithOwner(null, List.class, String.class);
        Type differentParameterizedType = new TypeToken<List<Integer>>() {
        }.getType();
        assertFalse(differentParameterizedType.equals(constructedType));
        assertFalse(constructedType.equals(differentParameterizedType));
    }

    private static final class A {
    }

    private static final class B {
    }

    private static final class C {
    }

    /**
     * Given a parameterized type A&lt;B,C&gt;, returns B. If the specified type is not
     * a generic type, returns null.
     */
    public static Type getFirstTypeArgument(Type type) throws Exception {
        if (!(type instanceof ParameterizedType)) return null;
        ParameterizedType ptype = (ParameterizedType) type;
        Type[] actualTypeArguments = ptype.getActualTypeArguments();
        if (actualTypeArguments.length == 0) return null;
        return Types.canonicalize(actualTypeArguments[0]);
    }
}
