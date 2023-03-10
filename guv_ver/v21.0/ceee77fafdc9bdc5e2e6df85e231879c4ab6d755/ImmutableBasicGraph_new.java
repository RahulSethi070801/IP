/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.graph.BasicGraph.Presence;

/**
 * A {@link BasicGraph} whose relationships are constant. Instances of this class may be obtained
 * with {@link #copyOf(BasicGraph)}.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public class ImmutableBasicGraph<N>
    extends ImmutableValueGraph<N, Presence> implements BasicGraph<N> {

  private ImmutableBasicGraph(BasicGraph<N> graph) {
    super(graph);
  }

  /**
   * Returns an immutable copy of {@code graph}.
   */
  @SuppressWarnings("unchecked")
  public static <N> ImmutableBasicGraph<N> copyOf(BasicGraph<N> graph) {
    return (graph instanceof ImmutableBasicGraph)
        ? (ImmutableBasicGraph<N>) graph
        : new ImmutableBasicGraph<N>(graph);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N> ImmutableBasicGraph<N> copyOf(ImmutableBasicGraph<N> graph) {
    return checkNotNull(graph);
  }
}
