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

import static com.google.common.graph.GraphConstants.GRAPH_STRING_FORMAT;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link Graph}. It is recommended to extend this
 * class rather than implement {@link Graph} directly, to ensure consistent {@link #equals(Object)}
 * and {@link #hashCode()} results across different graph implementations.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractGraph<N> implements Graph<N> {

  @Override
  public int degree(Object node) {
    // TODO(b/28087289): only works for non-multigraphs; multigraphs not yet supported
    return adjacentNodes(node).size();
  }

  @Override
  public int inDegree(Object node) {
    // TODO(b/28087289): only works for non-multigraphs; multigraphs not yet supported
    return predecessors(node).size();
  }

  @Override
  public int outDegree(Object node) {
    // TODO(b/28087289): only works for non-multigraphs; multigraphs not yet supported
    return successors(node).size();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Graph)) {
      return false;
    }
    Graph<?> other = (Graph<?>) obj;

    // Needed to enforce a symmetric equality relationship.
    if (other instanceof Network) {
      return false;
    }

    if (isDirected() != other.isDirected()) {
      return false;
    }

    if (!nodes().equals(other.nodes())) {
      return false;
    }

    for (Object node : nodes()) {
      if (!successors(node).equals(other.successors(node))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Maps.asMap(nodes(), new Function<N, Set<N>>() {
      @Override
      public Set<N> apply(N node) {
        return successors(node);
      }
    }).hashCode();
  }

  /**
   * Returns a string representation of this graph. Encodes edge direction if any.
   */
  @Override
  public String toString() {
    return String.format(GRAPH_STRING_FORMAT,
        getPropertiesString(),
        nodes(),
        endpointsString());
  }

  // TODO(b/28087289): add allowsParallelEdges() once that's supported
  private String getPropertiesString() {
    return String.format("isDirected: %s, allowsSelfLoops: %s", isDirected(), allowsSelfLoops());
  }

  private String endpointsString() {
    return String.format("{%s}", Joiner.on(", ").join(Graphs.endpointsInternal(this)));
  }
}
