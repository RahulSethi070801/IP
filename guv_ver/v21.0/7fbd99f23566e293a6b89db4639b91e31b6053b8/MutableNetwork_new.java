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

import com.google.common.annotations.Beta;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * A subtype of {@link Network} which permits mutations.
 * Users should generally use the {@link Network} interface where possible.
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public interface MutableNetwork<N, E> extends Network<N, E> {

  /**
   * Adds {@code node} to this graph (optional operation).
   *
   * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be; they must also be non-null.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean addNode(N node);

  /**
   * Adds {@code edge} to this graph, connecting {@code nodeA} to {@code nodeB}
   * (optional operation).
   *
   * <p><b>Edges must be unique</b>, just as {@code Map} keys must be; they must also be non-null.
   *
   * <p>If {@code edge} already connects {@code nodeA} to {@code nodeB} in this graph
   * (in the specified order if order is significant, as for directed graphs, else in any order),
   * then this method will have no effect and will return {@code false}.
   *
   * <p>Behavior if {@code nodeA} and {@code nodeB} are not already elements of the graph is
   * unspecified. Suggested behaviors include (a) silently adding {@code nodeA} and {@code nodeB}
   * to the graph or (b) throwing {@code IllegalArgumentException}.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws IllegalArgumentException if {@code edge} already exists and connects nodes other than
   *     {@code nodeA} and {@code nodeB}, or if the graph is not a multigraph and {@code nodeA} is
   *     already connected to {@code nodeB}
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean addEdge(E edge, N nodeA, N nodeB);

  /**
   * Removes {@code node} from this graph, if it is present (optional operation).
   * In general, all edges incident to {@code node} in this graph will also be removed.
   * (This is not true for hyperedges.)
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean removeNode(Object node);

  /**
   * Removes {@code edge} from this graph, if it is present (optional operation).
   * In general, nodes incident to {@code edge} are unaffected (although implementations may choose
   * to disallow certain configurations, e.g., isolated nodes).
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean removeEdge(Object edge);
}
