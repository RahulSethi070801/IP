/*
 * Copyright (C) 2016 The Guava Authors
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
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A {@link ValueGraph} whose relationships and edge values are constant. Instances of this class
 * may be obtained with {@link #copyOf(ValueGraph)}.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public class ImmutableValueGraph<N, V> extends ConfigurableValueGraph<N, V> {

  ImmutableValueGraph(ValueGraph<N, V> graph) {
    super(ValueGraphBuilder.from(graph), getNodeConnections(graph), graph.edges().size());
  }

  /**
   * Returns an immutable copy of {@code graph}.
   */
  @SuppressWarnings("unchecked")
  public static <N, V> ImmutableValueGraph<N, V> copyOf(ValueGraph<N, V> graph) {
    return (graph instanceof ImmutableValueGraph)
        ? (ImmutableValueGraph<N, V>) graph
        : new ImmutableValueGraph<N, V>(graph);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N, V> ImmutableValueGraph<N, V> copyOf(ImmutableValueGraph<N, V> graph) {
    return checkNotNull(graph);
  }

  private static <N, V> ImmutableMap<N, GraphConnections<N, V>> getNodeConnections(
      ValueGraph<N, V> graph) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
    // whatever ordering the graph's nodes do, so ImmutableSortedMap is unnecessary even if the
    // input nodes are sorted.
    ImmutableMap.Builder<N, GraphConnections<N, V>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, connectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  private static <N, V> GraphConnections<N, V> connectionsOf(final ValueGraph<N, V> graph,
      final N node) {
    Function<N, V> successorNodeToValueFn = new Function<N, V>() {
      @Override
      public V apply(N successorNode) {
        return graph.edgeValue(node, successorNode);
      }
    };
    return graph.isDirected()
        ? DirectedGraphConnections.ofImmutable(
            graph.predecessors(node),
            Maps.asMap(graph.successors(node), successorNodeToValueFn))
        : UndirectedGraphConnections.ofImmutable(
            Maps.asMap(graph.adjacentNodes(node), successorNodeToValueFn));
  }
}
