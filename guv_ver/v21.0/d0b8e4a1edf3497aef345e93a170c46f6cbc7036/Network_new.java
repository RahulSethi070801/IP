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
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An interface for <a href="https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)">graph</a>
 * data structures. A graph is composed of a set of nodes (sometimes called vertices) and a set of
 * edges connecting pairs of nodes. Graphs are useful for modeling many kinds of relations. If the
 * relation to be modeled is symmetric (such as "distance between cities"), that can be represented
 * with an undirected graph, where an edge that connects node A to node B also connects node B to
 * node A. If the relation to be modeled is asymmetric (such as "employees managed"), that can be
 * represented with a directed graph, where edges are strictly one-way.
 *
 * <p>There are three main interfaces provided to represent graphs. In order of increasing
 * complexity they are: {@link BasicGraph}, {@link Graph}, and {@link Network}. You should generally
 * prefer the simplest interface that satisfies your use case.
 *
 * <ol>
 * <li>Do you have data (objects) that you wish to associate with edges?
 *     <p>Yes: Go to question 2. No: Use {@link BasicGraph}.
 * <li>Are the objects you wish to associate with edges unique within the scope of a graph? That is,
 *     no two objects would be {@link Object#equals(Object)} to each other. A common example where
 *     this would <i>not</i> be the case is with weighted graphs.
 *     <p>Yes: Go to question 3. No: Use {@link Graph}.
 * <li>Do you need to be able to query the graph for an edge associated with a particular object
 *     (not just the edge connecting a given pair of nodes)?
 *     <p>Yes: Use {@link Network}. No: Go to question 4.
 * <li>Do you need explicit support for parallel edges? Do you need to be able to remove one edge
 *     connecting a pair of nodes while leaving other edges connecting those same nodes?
 *     <p>Yes: Use {@link Network}. No: Use {@link Graph}.
 * </ol>
 *
 * <p>Although {@link Graph}s and {@link Network}s both require users to provide objects when adding
 * edges to the graph, the differentiating factor is that in {@link Graph}s, these objects can be
 * any arbitrary data. Like the values in a {@link Map}, they do not have to be unique, and can be
 * mutated while in the graph. In a {@link Network}, these objects serve as keys into internal data
 * structures. Like the keys in a {@link Map}, they must be unique, and cannot be mutated in a way
 * that affects their equals/hashcode or the data structure will become corrupted.
 *
 * <p>In all three interfaces, nodes have all the same requirements as keys in a {@link Map}.
 *
 * <p>All mutation methods live on the subinterface {@link MutableNetwork}. If you do not need to
 * mutate a network (e.g. if you write a method than runs a read-only algorithm on the network), you
 * should prefer the non-mutating {@link Network} interface.
 *
 * <p>We provide an efficient implementation of this interface via {@link NetworkBuilder}. When
 * using the implementation provided, all {@link Set}-returning methods provide live, unmodifiable
 * views of the network. In other words, you cannot add an element to the {@link Set}, but if an
 * element is added to the {@link Network} that would affect the result of that set, it will be
 * updated automatically. This also means that you cannot modify a {@link Network} in a way that
 * would affect a {#link Set} while iterating over that set. For example, you cannot remove the
 * nodes from a {@link Network} while iterating over {@link #nodes} (unless you first make a copy of
 * the nodes), just as you could not remove the keys from a {@Map} while iterating over its {@link
 * Map#keySet()}. This will either throw a {@link ConcurrentModificationException} or risk undefined
 * behavior.
 *
 * <p>Example of use:
 *
 * <pre><code>
 * MutableNetwork<String, String> roadNetwork = NetworkBuilder.undirected().build();
 * roadNetwork.addEdge("Springfield", "Shelbyville", "Monorail");
 * roadNetwork.addEdge("New York", "New New York", "Applied Cryogenics");
 * roadNetwork.addEdge("Springfield", "New New York", "Secret Wormhole");
 * String roadToQuery = "Secret Wormhole";
 * if (roadNetwork.edges().contains(roadToQuery)) {
 *   Endpoints<String> cities = roadNetwork.incidentNodes(roadToQuery);
 *   System.out.format("%s and %s connected via %s", cities.nodeA(), cities.nodeB(), roadToQuery);
 * }
 * </code></pre>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public interface Network<N, E> {
  //
  // Network-level accessors
  //

  /** Returns all nodes in this graph, in the order specified by {@link #nodeOrder()}. */
  Set<N> nodes();

  /** Returns all edges in this network, in the order specified by {@link #edgeOrder()}. */
  Set<E> edges();

  /**
   * Returns a live view of this network as a {@link Graph}. The resulting {@link Graph} will have
   * an edge connecting node A to node B iff this {@link Network} has an edge connecting A to B.
   *
   * <p>{@link Graph#edgeValue(Object, Object)} will return the set of edges connecting node A to
   * node B if the set is non-empty, otherwise, it will throw {@link IllegalArgumentException}.
   *
   * <p>If this network {@link #allowsParallelEdges()}, parallel edges will treated as if collapsed
   * into a single edge. For example, the {@link #degree(Object)} of a node in the {@link Graph}
   * view may be less than the degree of the same node in this {@link Network}.
   */
  Graph<N, Set<E>> asGraph();

  //
  // Network properties
  //

  /**
   * Returns true if the edges in this graph have a direction associated with them.
   *
   * <p>A directed edge is an {@linkplain #outEdges(Object) outgoing edge} of its {@linkplain
   * Endpoints#source() source}, and an {@linkplain #inEdges(Object) incoming edge} of its
   * {@linkplain Endpoints#target() target}. An undirected edge connects its {@linkplain
   * #incidentNodes(Object) incident nodes} to each other, and is both an {@linkplain
   * #outEdges(Object) outgoing edge} and {@linkplain #inEdges(Object) incoming edge} of each
   * incident node.
   */
  boolean isDirected();

  /**
   * Returns true if this graph allows self-loops (edges that connect a node to itself). Attempting
   * to add a self-loop to a graph that does not allow them will throw an {@link
   * UnsupportedOperationException}.
   */
  boolean allowsSelfLoops();

  /**
   * Returns true if this graph allows parallel edges. Attempting to add a parallel edge to a graph
   * that does not allow them will throw an {@link UnsupportedOperationException}.
   */
  boolean allowsParallelEdges();

  /** Returns the order of iteration for the elements of {@link #nodes()}. */
  ElementOrder<N> nodeOrder();

  /** Returns the order of iteration for the elements of {@link #edges()}. */
  ElementOrder<E> edgeOrder();

  //
  // Element-level accessors
  //

  /**
   * Returns the nodes which have an incident edge in common with {@code node} in this graph.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> adjacentNodes(Object node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s incoming edges <i>against</i> the direction (if any) of the edge.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> predecessors(Object node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s outgoing edges in the direction (if any) of the edge.
   *
   * <p>This is <i>not</i> the same as "all nodes reachable from {@code node} by following outgoing
   * edges" (also known as {@code node}'s transitive closure).
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> successors(Object node);

  /**
   * Returns the edges whose endpoints in this graph include {@code node}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<E> incidentEdges(Object node);

  /**
   * Returns all edges in this graph which can be traversed in the direction (if any) of the edge to
   * end at {@code node}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<E> inEdges(Object node);

  /**
   * Returns all edges in this graph which can be traversed in the direction (if any) of the edge
   * starting from {@code node}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<E> outEdges(Object node);

  /**
   * Returns the count of {@code node}'s {@link #incidentEdges(Object) incident edges}, counting
   * self-loops twice (equivalently, the number of times an edge touches {@code node}).
   *
   * <p>For directed graphs, this is equivalent to {@code inDegree(node) + outDegree(node)}.
   *
   * <p>For undirected graphs, this is equivalent to {@code incidentEdges(node).size()} + (number
   * of self-loops incident to {@code node}).
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int degree(Object node);

  /**
   * Returns the count of {@code node}'s {@link #inEdges(Object) incoming edges} in a directed
   * graph. In an undirected graph, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int inDegree(Object node);

  /**
   * Returns the count of {@code node}'s {@link #outEdges(Object) outgoing edges} in a directed
   * graph. In an undirected graph, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int outDegree(Object node);

  /**
   * Returns the nodes which are the endpoints of {@code edge} in this graph as {@link Endpoints}.
   *
   * @throws IllegalArgumentException if {@code edge} is not an element of this graph
   */
  Endpoints<N> incidentNodes(Object edge);

  /**
   * Returns the set of edges that connect {@code nodeA} to {@code nodeB}.
   *
   * <p>This set is the intersection of {@code outEdges(nodeA)} and {@code inEdges(nodeB)}. If
   * {@code nodeA} is equal to {@code nodeB}, then it is the set of self-loop edges for that node.
   *
   * @throws IllegalArgumentException if {@code nodeA} or {@code nodeB} is not an element of this
   *     graph
   */
  Set<E> edgesConnecting(Object nodeA, Object nodeB);

  //
  // Network identity
  //

  /**
   * Returns {@code true} iff {@code object} is a {@link Network} that has the same elements and the
   * same structural relationships as those in this network.
   *
   * <p>Thus, two networks A and B are equal if <b>all</b> of the following are true:
   *
   * <ul>
   * <li>A and B have equal {@link #isDirected() directedness}.
   * <li>A and B have equal {@link #nodes() node sets}.
   * <li>A and B have equal {@link #edges() edge sets}.
   * <li>Every edge in A and B connects the same nodes in the same direction (if any).
   * </ul>
   *
   * <p>Network properties besides {@link #isDirected() directedness} do <b>not</b> affect equality.
   * For example, two networks may be considered equal even if one allows parallel edges and the
   * other doesn't. Additionally, the order in which nodes or edges are added to the network, and
   * the order in which they are iterated over, are irrelevant.
   *
   * <p>A reference implementation of this is provided by {@link AbstractNetwork#equals(Object)}.
   */
  @Override
  boolean equals(@Nullable Object object);

  /**
   * Returns the hash code for this network. The hash code of a network is defined as the hash code
   * of a map from each of its {@link #edges() edges} to their {@link #incidentNodes(Object)
   * incident nodes}.
   *
   * <p>A reference implementation of this is provided by {@link AbstractNetwork#hashCode()}.
   */
  @Override
  int hashCode();
}
