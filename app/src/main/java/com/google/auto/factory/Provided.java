/**
 * Copyright (C) 2013 Google, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.auto.factory;

import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Target;

/**
 * COPIED HERE BECAUSE OF THIS ISSUE:
 * https://github.com/google/auto/issues/268*
 *
 * An annotation to be applied to parameters that should be provided by an
 * {@linkplain javax.inject.Inject injected} {@link javax.inject.Provider} in a generated factory.
 *
 * @author Gregory Kick
 */
@Target(PARAMETER)
public @interface Provided {
}