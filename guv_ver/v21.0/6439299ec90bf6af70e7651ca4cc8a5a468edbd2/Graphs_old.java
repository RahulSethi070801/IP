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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.GraphConstants.ENDPOINTS_GRAPH_DIRECTEDNESS;
import static com.google.common.graph.GraphConstants.NETWORK_WITH_PARALLEL_EDGE;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Static utility methods for {@link Graph} instances.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
public final class Graphs {

  private static final String GRAPH_FORMAT = "%s, nodes: %s, edges: %s";

  private Graphs() {}

  // Graph query methods

  private static <N> Set<Endpoints<N>> endpointsInternal(final Graph<N> graph) {
    if (graph instanceof Network && !allowsParallelEdges(graph)) {
      // Use an optimized implementation for networks without parallel edges.
      return endpointsSimpleNetwork(castToNetwork(graph));
    }

    return new AbstractSet<Endpoints<N>>() {
      @Override
      public Iterator<Endpoints<N>> iterator() {
        return graph.isDirected()
            ? new DirectedEndpointsIterator<N>(graph)
            : new UndirectedEndpointsIterator<N>(graph);
      }

      @Override
      public int size() {
        boolean directed = graph.isDirected();
        long endpointsCount = 0L;
        for (N node : graph.nodes()) {
          Set<N> successors = graph.successors(node);
          endpointsCount += successors.size();
          if (!directed && successors.contains(node)) {
            endpointsCount++; // count self-loops twice in the undirected case
          }
        }
        if (!directed) {
          // In undirected graphs, every pair of adjacent nodes has been counted twice.
          checkState((endpointsCount & 1) == 0);
          endpointsCount >>>= 1;
        }
        return Ints.saturatedCast(endpointsCount);
      }

      @Override
      public boolean contains(Object obj) {
        if (!(obj instanceof Endpoints)) {
          return false;
        }
        return containsEndpoints(graph, (Endpoints<?>) obj);
      }
    };
  }

  private static <N> Set<Endpoints<N>> endpointsSimpleNetwork(final Network<N, ?> graph) {
    checkState(!graph.allowsParallelEdges());
    return new AbstractSet<Endpoints<N>>() {
      @Override
      public Iterator<Endpoints<N>> iterator() {
        return Iterators.transform(
            graph.edges().iterator(),
            new Function<Object, Endpoints<N>>() {
              @Override
              public Endpoints<N> apply(Object edge) {
                return graph.incidentNodes(edge);
              }
            });
      }

      @Override
      public int size() {
        return graph.edges().size();
      }

      @Override
      public boolean contains(Object obj) {
        if (!(obj instanceof Endpoints)) {
          return false;
        }
        return containsEndpoints(graph, (Endpoints<?>) obj);
      }
    };
  }

  private static boolean containsEndpoints(Graph<?> graph, Endpoints<?> endpoints) {
    return graph.isDirected() == endpoints.isDirected()
        && graph.nodes().contains(endpoints.nodeA())
        && graph.successors(endpoints.nodeA()).contains(endpoints.nodeB());
  }

  /**
   * Returns an unmodifiable view of edges that are parallel to {@code edge}, i.e. the set of edges
   * that connect the same nodes in the same direction (if any). An edge is not parallel to itself.
   *
   * @throws IllegalArgumentException if {@code edge} is not present in {@code graph}
   */
  public static <N, E> Set<E> parallelEdges(Network<N, E> graph, Object edge) {
    Endpoints<N> endpoints = graph.incidentNodes(edge); // Verifies that edge is in graph
    if (!graph.allowsParallelEdges()) {
      return ImmutableSet.of();
    }
    return Sets.difference(graph.edgesConnecting(endpoints.nodeA(), endpoints.nodeB()),
        ImmutableSet.of(edge)); // An edge is not parallel to itself.
  }

  // Graph mutation methods

  /**
   * Adds {@code edge} to {@code graph} with the specified {@code endpoints}.
   */
  @CanIgnoreReturnValue
  public static <N, E> boolean addEdge(MutableNetwork<N, E> graph, E edge, Endpoints<N> endpoints) {
    checkNotNull(graph, "graph");
    checkNotNull(edge, "edge");
    checkNotNull(endpoints, "endpoints");
    checkArgument(endpoints.isDirected() == graph.isDirected(),
        ENDPOINTS_GRAPH_DIRECTEDNESS, endpoints.isDirected(), graph.isDirected());
    return graph.addEdge(edge, endpoints.nodeA(), endpoints.nodeB());
  }

  // Graph transformation methods

  /**
   * Returns an induced subgraph of {@code graph}. This subgraph is a new graph that contains
   * all of the nodes in {@code nodes}, and all of the edges from {@code graph} for which the
   * edge's incident nodes are both contained by {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N, E> MutableNetwork<N, E> inducedSubgraph(Network<N, E> graph,
      Iterable<? extends N> nodes) {
    NetworkBuilder<N, E> builder = NetworkBuilder.from(graph);
    if (nodes instanceof Collection) {
      builder = builder.expectedNodeCount(((Collection<?>) nodes).size());
    }
    MutableNetwork<N, E> subgraph = builder.build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (E edge : graph.outEdges(node)) {
        N otherNode = graph.incidentNodes(edge).otherNode(node);
        if (subgraph.nodes().contains(otherNode)) {
          subgraph.addEdge(edge, node, otherNode);
        }
      }
    }
    return subgraph;
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same nodes and edges.
   */
  public static <N> MutableGraph<N> copyOf(Graph<N> graph) {
    checkNotNull(graph, "graph");
    // TODO(b/28087289): we can remove this restriction when Graph supports parallel edges
    checkArgument(!allowsParallelEdges(graph), NETWORK_WITH_PARALLEL_EDGE);
    MutableGraph<N> copy = GraphBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .build();

    for (N node : graph.nodes()) {
      checkState(copy.addNode(node));
    }
    for (Endpoints<N> endpoints : endpointsInternal(graph)) {
      checkState(copy.addEdge(endpoints.nodeA(), endpoints.nodeB()));
    }

    return copy;
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same node and edge elements.
   */
  public static <N, E> MutableNetwork<N, E> copyOf(Network<N, E> graph) {
    checkNotNull(graph, "graph");
    MutableNetwork<N, E> copy = NetworkBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .expectedEdgeCount(graph.edges().size())
        .build();

    for (N node : graph.nodes()) {
      checkState(copy.addNode(node));
    }
    for (E edge : graph.edges()) {
      checkState(addEdge(copy, edge, graph.incidentNodes(edge)));
    }

    return copy;
  }

  /**
   * Returns true iff {@code graph1} and {@code graph2} are equal as defined by
   * {@link Graph#equals(Object)}.
   */
  public static boolean equal(@Nullable Graph<?> graph1, @Nullable Graph<?> graph2) {
    // If both graphs are Network instances, use equal(Network, Network) instead
    if (graph1 instanceof Network && graph2 instanceof Network) {
      return equal(castToNetwork(graph1), castToNetwork(graph2));
    }

    // Otherwise, if either graph is a Network (but not both), they can't be equal.
    if (graph1 instanceof Network || graph2 instanceof Network) {
      return false;
    }

    if (graph1 == graph2) {
      return true;
    }

    if (graph1 == null || graph2 == null) {
      return false;
    }

    if (graph1.isDirected() != graph2.isDirected()) {
      return false;
    }

    if (!graph1.nodes().equals(graph2.nodes())) {
      return false;
    }

    for (Object node : graph1.nodes()) {
      if (!graph1.successors(node).equals(graph2.successors(node))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns true iff {@code graph1} and {@code graph2} are equal as defined by
   * {@link Network#equals(Object)}.
   */
  public static boolean equal(@Nullable Network<?, ?> graph1, @Nullable Network<?, ?> graph2) {
    if (graph1 == graph2) {
      return true;
    }

    if (graph1 == null || graph2 == null) {
      return false;
    }

    if (graph1.isDirected() != graph2.isDirected()) {
      return false;
    }

    if (!graph1.nodes().equals(graph2.nodes()) || !graph1.edges().equals(graph2.edges())) {
      return false;
    }

    for (Object edge : graph1.edges()) {
      if (!graph1.incidentNodes(edge).equals(graph2.incidentNodes(edge))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the hash code of {@code graph} as defined by {@link Graph#hashCode()}.
   */
  public static int hashCode(Graph<?> graph) {
    if (graph instanceof Network) {
      return hashCode(castToNetwork(graph));
    }
    return nodeToSuccessorNodes(graph).hashCode();
  }

  /**
   * Returns the hash code of {@code graph} as defined by {@link Network#hashCode()}.
   */
  public static int hashCode(Network<?, ?> graph) {
    return nodeToOutEdges(graph).hashCode();
  }

  /**
   * Returns a string representation of {@code graph}. Encodes edge direction if {@code graph}
   * is directed.
   */
  public static String toString(Graph<?> graph) {
    if (graph instanceof Network) {
      return toString(castToNetwork(graph));
    }
    return String.format(GRAPH_FORMAT,
        getPropertiesString(graph),
        graph.nodes(),
        endpointsString(graph));
  }

  /**
   * Returns a string representation of {@code graph}. Encodes edge direction if {@code graph}
   * is directed.
   */
  public static String toString(Network<?, ?> graph) {
    return String.format(GRAPH_FORMAT,
        getPropertiesString(graph),
        graph.nodes(),
        Maps.asMap(graph.edges(), edgeToEndpointsString(graph)));
  }

  /**
   * Returns a String of the endpoints for {@code graph}.
   */
  private static <N> String endpointsString(final Graph<N> graph) {
    checkNotNull(graph, "graph");
    return String.format("{%s}", Joiner.on(", ").join(endpointsInternal(graph)));
  }

  /**
   * Returns a map that is a live view of {@code graph}, with nodes as keys
   * and the set of outgoing edges as values.
   */
  private static <N, E> Map<N, Set<E>> nodeToOutEdges(final Network<N, E> graph) {
    checkNotNull(graph, "graph");
    return Maps.asMap(graph.nodes(), new Function<N, Set<E>>() {
      @Override
      public Set<E> apply(N node) {
        return graph.outEdges(node);
      }
    });
  }

  /**
   * Returns a map that is a live view of {@code graph}, with nodes as keys
   * and the set of successor nodes as values.
   */
  private static <N> Map<N, Set<N>> nodeToSuccessorNodes(final Graph<N> graph) {
    checkNotNull(graph, "graph");
    return Maps.asMap(graph.nodes(), new Function<N, Set<N>>() {
      @Override
      public Set<N> apply(N node) {
        return graph.successors(node);
      }
    });
  }

  /**
   * Returns a function that transforms an edge into a string representation of its endpoints
   * in {@code graph}. The function's {@code apply} method will throw an
   * {@link IllegalArgumentException} if {@code graph} does not contain {@code edge}.
   */
  private static Function<Object, String> edgeToEndpointsString(final Network<?, ?> graph) {
    checkNotNull(graph, "graph");
    return new Function<Object, String>() {
      @Override
      public String apply(Object edge) {
        return graph.incidentNodes(edge).toString();
      }
    };
 }

  /**
   * Returns a string representation of the properties of {@code graph}.
   */
  // TODO(b/28087289): add allowsParallelEdges() once that's supported
  private static String getPropertiesString(Graph<?> graph) {
    if (graph instanceof Network) {
      return getPropertiesString(castToNetwork(graph));
    }
    return String.format("isDirected: %s, allowsSelfLoops: %s",
        graph.isDirected(), graph.allowsSelfLoops());
  }

  /**
   * Returns a string representation of the properties of {@code graph}.
   */
  private static String getPropertiesString(Network<?, ?> graph) {
    return String.format("isDirected: %s, allowsParallelEdges: %s, allowsSelfLoops: %s",
        graph.isDirected(), graph.allowsParallelEdges(), graph.allowsSelfLoops());
  }

  private static boolean allowsParallelEdges(Graph<?> graph) {
    return (graph instanceof Network) && castToNetwork(graph).allowsParallelEdges();
  }

  @SuppressWarnings("unchecked")
  private static <N> Network<N, ?> castToNetwork(Graph<N> graph) {
    return (Network<N, ?>) graph;
  }

  private abstract static class AbstractEndpointsIterator<N>
      extends AbstractIterator<Endpoints<N>> {
    private final Graph<N> graph;
    private final Iterator<N> nodeIterator;

    N node = null; // null is safe as an initial value because graphs do not allow null nodes
    Iterator<N> successorIterator = ImmutableSet.<N>of().iterator();

    AbstractEndpointsIterator(Graph<N> graph) {
      this.graph = graph;
      this.nodeIterator = graph.nodes().iterator();
    }

    /**
     * Called after {@link #successorIterator} is exhausted. Advances {@link #node} to the next node
     * and updates {@link #successorIterator} to iterate through the successors of {@link #node}.
     */
    final boolean advance() {
      checkState(!successorIterator.hasNext());
      if (!nodeIterator.hasNext()) {
        return false;
      }
      node = nodeIterator.next();
      successorIterator = graph.successors(node).iterator();
      return true;
    }
  }

  /**
   * If the graph is directed, each ordered [source, target] pair will be visited once if there is
   * one or more edge connecting them.
   */
  private static final class DirectedEndpointsIterator<N> extends AbstractEndpointsIterator<N> {
    DirectedEndpointsIterator(Graph<N> graph){
      super(graph);
    }

    @Override
    protected Endpoints<N> computeNext() {
      while (true) {
        if (successorIterator.hasNext()) {
          return Endpoints.ofDirected(node, successorIterator.next());
        }
        if (!advance()) {
          return endOfData();
        }
      }
    }
  }

  /**
   * If the graph is undirected, each unordered [node, otherNode] pair (except self-loops) will be
   * visited twice if there is one or more edge connecting them. To avoid returning duplicate
   * {@link Endpoints}, we keep track of the nodes that we have visited. When processing node pairs,
   * we skip if the "other node" is in the visited set, as shown below:
   *
   * Nodes = {N1, N2, N3, N4}
   *    N2           __
   *   /  \         |  |
   * N1----N3      N4__|
   *
   * Visited Nodes = {}
   * Endpoints [N1, N2] - return
   * Endpoints [N1, N3] - return
   * Visited Nodess = {N1}
   * Endpoints [N2, N1] - skip
   * Endpoints [N2, N3] - return
   * Visited Nodes = {N1, N2}
   * Endpoints [N3, N1] - skip
   * Endpoints [N3, N2] - skip
   * Visited Nodes = {N1, N2, N3}
   * Endpoints [N4, N4] - return
   * Visited Nodes = {N1, N2, N3, N4}
   */
  private static final class UndirectedEndpointsIterator<N> extends AbstractEndpointsIterator<N> {
    private Set<N> visitedNodes;

    UndirectedEndpointsIterator(Graph<N> graph) {
      super(graph);
      this.visitedNodes = Sets.newHashSetWithExpectedSize(graph.nodes().size());
    }

    @Override
    protected Endpoints<N> computeNext() {
      while (true) {
        while (successorIterator.hasNext()) {
          N otherNode = successorIterator.next();
          if (!visitedNodes.contains(otherNode)) {
            return Endpoints.ofUndirected(node, otherNode);
          }
        }
        // Add to visited set *after* processing neighbors so we still include self-loops.
        visitedNodes.add(node);
        if (!advance()) {
          visitedNodes = null;
          return endOfData();
        }
      }
    }
  }
}
