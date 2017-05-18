/*
 * Copyright (C) 2008 The Guava Authors
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

package com.soundcloud.java.collections;

import java.util.Iterator;

/**
 * An iterator that does not support {@link #remove}.
 *
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 *
 * <p><b>This class contains code derived from <a href="https://github.com/google/guava">Google Guava</a></b>
 */
public abstract class UnmodifiableIterator<E> implements Iterator<E> {
    /**
     * Constructor for use by subclasses.
     */
    protected UnmodifiableIterator() {
    }

    /**
     * Guaranteed to throw an exception and leave the underlying data unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
