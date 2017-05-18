/*
 * Copyright (C) 2008 Google Inc.
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

package com.soundcloud.java.reflect;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import java.lang.reflect.Type;


/**
 * Represents a generic type {@code T}. Java doesn't yet provide a way to
 * represent generic types, so this class does. Forces clients to create a
 * subclass of this class which enables retrieval the type information even at
 * runtime.
 *
 * <p>For example, to create a type literal for {@code List<String>}, you can
 * create an empty anonymous inner class:
 *
 * <p>
 * {@code TypeToken<List<String>> list = new TypeToken<List<String>>() {};}
 *
 * <p>This syntax cannot be used to create type literals that have wildcard
 * parameters, such as {@code Class<?>} or {@code List<? extends CharSequence>}.
 *
 * <p><b>This class contains code derived from <a href="https://github.com/google/gson">Google GSON</a></b>
 *
 * @author Bob Lee
 * @author Sven Mawson
 * @author Jesse Wilson
 */
public class TypeToken<T> {
    private final Class<? super T> rawType;
    private final Type type;
    private final int hashCode;

    /**
     * Gets type literal for the given {@code Type} instance.
     */
    public static TypeToken<?> of(Type type) {
        return new TypeToken<>(type);
    }

    /**
     * Gets type literal for the given {@code Class} instance.
     */
    public static <T> TypeToken<T> of(Class<T> type) {
        return new TypeToken<>(type);
    }

    /**
     * Constructs a new type literal. Derives represented class from type
     * parameter.
     *
     * <p>Clients create an empty anonymous subclass. Doing so embeds the type
     * parameter in the anonymous class's type hierarchy so we can reconstitute it
     * at runtime despite erasure.
     */
    @SuppressWarnings("unchecked")
    protected TypeToken() {
        this.type = Types.getSuperclassTypeParameter(getClass());
        this.rawType = (Class<? super T>) Types.getRawType(type);
        this.hashCode = type.hashCode();
    }

    /**
     * Unsafe. Constructs a type literal manually.
     */
    @SuppressWarnings("unchecked")
    TypeToken(Type type) {
        this.type = Types.canonicalize(checkNotNull(type));
        this.rawType = (Class<? super T>) Types.getRawType(this.type);
        this.hashCode = this.type.hashCode();
    }

    /**
     * Returns the raw (non-generic) type for this type.
     */
    public final Class<? super T> getRawType() {
        return rawType;
    }

    /**
     * Gets underlying {@code Type} instance.
     */
    public final Type getType() {
        return type;
    }

    @Override
    public final int hashCode() {
        return this.hashCode;
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof TypeToken<?>
                && Types.equals(type, ((TypeToken<?>) o).type);
    }

    @Override
    public final String toString() {
        return Types.typeToString(type);
    }

}
