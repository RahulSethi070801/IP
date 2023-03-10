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

import static com.google.common.graph.TestUtil.sanityCheckCollection;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract base class for testing implementations of {@link BasicGraph} interface. Graph
 * instances created for testing should have Integer node and String edge objects.
 *
 * <p>Test cases that should be handled similarly in any graph implementation are
 * included in this class. For example, testing that {@code nodes()} method returns
 * the set of the nodes in the graph. The following test cases are left for the subclasses
 * to handle:
 * <ul>
 * <li>Test cases related to whether the graph is directed, undirected, mutable,
 *     or immutable.
 * <li>Test cases related to the specific implementation of the {@link BasicGraph} interface.
 * </ul>
 *
 * TODO(user): Make this class generic (using <N, E>) for all node and edge types.
 * TODO(user): Differentiate between directed and undirected edge strings.
 */
public abstract class AbstractGraphTest {
  MutableBasicGraph<Integer> graph;
  static final Integer N1 = 1;
  static final Integer N2 = 2;
  static final Integer N3 = 3;
  static final Integer N4 = 4;
  static final Integer N5 = 5;
  static final Integer NODE_NOT_IN_GRAPH = 1000;

  // TODO(user): Consider separating Strings that we've defined here to capture
  // identifiable substrings of expected error messages, from Strings that we've defined
  // here to provide error messages.
  // TODO(user): Some Strings used in the subclasses can be added as static Strings
  // here too.
  static final String ERROR_ELEMENT_NOT_IN_GRAPH = "not an element of this graph";
  static final String NODE_STRING = "Node";
  static final String ERROR_MODIFIABLE_SET = "Set returned is unexpectedly modifiable";
  static final String ERROR_SELF_LOOP = "self-loops are not allowed";
  static final String ERROR_NODE_NOT_IN_GRAPH =
      "Should not be allowed to pass a node that is not an element of the graph.";
  static final String ERROR_ADDED_SELF_LOOP = "Should not be allowed to add a self-loop edge.";

  /**
   * Creates and returns an instance of the graph to be tested.
   */
  public abstract MutableBasicGraph<Integer> createGraph();

  /**
   * A proxy method that adds the node {@code n} to the graph being tested.
   * In case of Immutable graph implementations, this method should add {@code n} to the graph
   * builder and build a new graph with the current builder state.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * TODO(user): Consider changing access modifier to be protected.
   */
  @CanIgnoreReturnValue
  boolean addNode(Integer n) {
    return graph.addNode(n);
  }

  /**
   * A proxy method that adds the edge {@code e} to the graph
   * being tested. In case of Immutable graph implementations, this method
   * should add {@code e} to the graph builder and build a new graph with the current
   * builder state.
   *
   * <p>This method should be used in tests of specific implementations if you want to
   * ensure uniform behavior (including side effects) with how edges are added elsewhere
   * in the tests.  For example, the existing implementations of this method explicitly
   * add the supplied nodes to the graph, and then call {@code graph.addEdge()} to connect
   * the edge to the nodes; this is not part of the contract of {@code graph.addEdge()}
   * and is done for convenience.  In cases where you want to avoid such side effects
   * (e.g., if you're testing what happens in your implementation if you add an edge
   * whose end-points don't already exist in the graph), you should <b>not</b> use this
   * method.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * TODO(user): Consider changing access modifier to be protected.
   */
  @CanIgnoreReturnValue
  boolean addEdge(Integer n1, Integer n2) {
    graph.addNode(n1);
    graph.addNode(n2);
    return graph.putEdge(n1, n2);
  }

  @Before
  public void init() {
    graph = createGraph();
  }

  @After
  public void validateGraphState() {
    validateGraph(graph);
  }

  static <N> void validateGraph(ValueGraph<N, ?> graph) {
    if (graph instanceof BasicGraph) {
      @SuppressWarnings("unchecked")
      BasicGraph<N> basicGraph = (BasicGraph<N>) graph;
      new EqualsTester().addEqualityGroup(
          basicGraph,
          Graphs.copyOf(basicGraph),
          ImmutableBasicGraph.copyOf(basicGraph)).testEquals();
    }

    String graphString = graph.toString();
    assertThat(graphString).contains("isDirected: " + graph.isDirected());
    assertThat(graphString).contains("allowsSelfLoops: " + graph.allowsSelfLoops());

    int nodeStart = graphString.indexOf("nodes:");
    int edgeStart = graphString.indexOf("edges:");
    String nodeString = graphString.substring(nodeStart, edgeStart);

    sanityCheckCollection(graph.nodes());
    sanityCheckCollection(graph.edges());

    Set<Endpoints<N>> allEndpoints = new HashSet<Endpoints<N>>();

    for (N node : graph.nodes()) {
      assertThat(nodeString).contains(node.toString());

      sanityCheckCollection(graph.adjacentNodes(node));
      sanityCheckCollection(graph.predecessors(node));
      sanityCheckCollection(graph.successors(node));

      for (N adjacentNode : graph.adjacentNodes(node)) {
        assertThat(graph.predecessors(node).contains(adjacentNode)
            || graph.successors(node).contains(adjacentNode)).isTrue();
      }

      for (N predecessor : graph.predecessors(node)) {
        assertThat(graph.successors(predecessor)).contains(node);
      }

      for (N successor : graph.successors(node)) {
        allEndpoints.add(Endpoints.of(graph, node, successor));
        assertThat(graph.predecessors(successor)).contains(node);
      }
    }

    assertThat(graph.edges()).isEqualTo(allEndpoints);
  }

  /**
   * Verifies that the {@code Set} returned by {@code nodes} has the expected mutability property
   * (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void nodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentNodes} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void adjacentNodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code predecessors} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void predecessors_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code successors} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void successors_checkReturnedSetMutability();

  @Test
  public void nodes_oneNode() {
    addNode(N1);
    assertThat(graph.nodes()).containsExactly(N1);
  }

  @Test
  public void nodes_noNodes() {
    assertThat(graph.nodes()).isEmpty();
  }

  @Test
  public void adjacentNodes_oneEdge() {
    addEdge(N1, N2);
    assertThat(graph.adjacentNodes(N1)).containsExactly(N2);
    assertThat(graph.adjacentNodes(N2)).containsExactly(N1);
  }

  @Test
  public void adjacentNodes_noAdjacentNodes() {
    addNode(N1);
    assertThat(graph.adjacentNodes(N1)).isEmpty();
  }

  @Test
  public void adjacentNodes_nodeNotInGraph() {
    try {
      graph.adjacentNodes(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void predecessors_noPredecessors() {
    addNode(N1);
    assertThat(graph.predecessors(N1)).isEmpty();
  }

  @Test
  public void predecessors_nodeNotInGraph() {
    try {
      graph.predecessors(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void successors_noSuccessors() {
    addNode(N1);
    assertThat(graph.successors(N1)).isEmpty();
  }

  @Test
  public void successors_nodeNotInGraph() {
    try {
      graph.successors(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void addNode_newNode() {
    assertThat(addNode(N1)).isTrue();
    assertThat(graph.nodes()).contains(N1);
  }

  @Test
  public void addNode_existingNode() {
    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(graph.nodes());
    assertThat(addNode(N1)).isFalse();
    assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_existingNode() {
    addEdge(N1, N2);
    addEdge(N4, N1);
    assertThat(graph.removeNode(N1)).isTrue();
    assertThat(graph.removeNode(N1)).isFalse();
    assertThat(graph.nodes()).containsExactly(N2, N4);
    assertThat(graph.adjacentNodes(N2)).isEmpty();
    assertThat(graph.adjacentNodes(N4)).isEmpty();
  }

  @Test
  public void removeNode_antiparallelEdges() {
    addEdge(N1, N2);
    addEdge(N2, N1);

    assertThat(graph.removeNode(N1)).isTrue();
    assertThat(graph.nodes()).containsExactly(N2);
    assertThat(graph.edges()).isEmpty();

    assertThat(graph.removeNode(N2)).isTrue();
    assertThat(graph.nodes()).isEmpty();
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  public void removeNode_nodeNotPresent() {
    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(graph.nodes());
    assertThat(graph.removeNode(NODE_NOT_IN_GRAPH)).isFalse();
    assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_queryAfterRemoval() {
    addNode(N1);
    Set<Integer> unused = graph.adjacentNodes(N1); // ensure cache (if any) is populated
    assertThat(graph.removeNode(N1)).isTrue();
    try {
      graph.adjacentNodes(N1);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void removeEdge_existingEdge() {
    addEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N2);
    assertThat(graph.predecessors(N2)).containsExactly(N1);
    assertThat(graph.removeEdge(N1, N2)).isTrue();
    assertThat(graph.removeEdge(N1, N2)).isFalse();
    assertThat(graph.successors(N1)).isEmpty();
    assertThat(graph.predecessors(N2)).isEmpty();
  }

  @Test
  public void removeEdge_oneOfMany() {
    addEdge(N1, N2);
    addEdge(N1, N3);
    addEdge(N1, N4);
    assertThat(graph.removeEdge(N1, N3)).isTrue();
    assertThat(graph.adjacentNodes(N1)).containsExactly(N2, N4);
  }

  @Test
  public void removeEdge_nodeNotPresent() {
    addEdge(N1, N2);
    assertThat(graph.removeEdge(N1, NODE_NOT_IN_GRAPH)).isFalse();
    assertThat(graph.successors(N1)).contains(N2);
  }

  @Test
  public void removeEdge_edgeNotPresent() {
    addEdge(N1, N2);
    addNode(N3);
    assertThat(graph.removeEdge(N1, N3)).isFalse();
    assertThat(graph.successors(N1)).contains(N2);
  }

  static void assertNodeNotInGraphErrorMessage(Throwable throwable) {
    assertThat(throwable.getMessage()).startsWith(NODE_STRING);
    assertThat(throwable.getMessage()).contains(ERROR_ELEMENT_NOT_IN_GRAPH);
  }
}
