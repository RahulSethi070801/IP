/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import static java.util.Arrays.asList;

import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

/**
 * Tests for {@link EvictingQueue}.
 *
 * @author Kurt Alfred Kluever
 */
public class EvictingQueueTest extends TestCase {

  public void testCreateWithNegativeSize() throws Exception {
    try {
      EvictingQueue.create(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateWithZeroSize() throws Exception {
    try {
      EvictingQueue.create(0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testEvictingAfterOne() throws Exception {
    EvictingQueue<String> queue = EvictingQueue.create(1);
    assertEquals(0, queue.size());

    assertTrue(queue.add("hi"));
    assertEquals("hi", queue.element());
    assertEquals("hi", queue.peek());
    assertEquals(1, queue.size());

    assertTrue(queue.add("there"));
    assertEquals("there", queue.element());
    assertEquals("there", queue.peek());
    assertEquals(1, queue.size());

    assertEquals("there", queue.remove());
    assertEquals(0, queue.size());
  }

  public void testEvictingAfterThree() throws Exception {
    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertEquals(0, queue.size());

    assertTrue(queue.add("one"));
    assertTrue(queue.add("two"));
    assertTrue(queue.add("three"));
    assertEquals("one", queue.element());
    assertEquals("one", queue.peek());
    assertEquals(3, queue.size());

    assertTrue(queue.add("four"));
    assertEquals("two", queue.element());
    assertEquals("two", queue.peek());
    assertEquals(3, queue.size());

    assertEquals("two", queue.remove());
    assertEquals(2, queue.size());
  }

  public void testAddAll() throws Exception {
    EvictingQueue<String> queue = EvictingQueue.create(3);
    assertEquals(0, queue.size());

    assertTrue(queue.addAll(asList("one", "two", "three")));
    assertEquals("one", queue.element());
    assertEquals("one", queue.peek());
    assertEquals(3, queue.size());

    assertTrue(queue.addAll(asList("four")));
    assertEquals("two", queue.element());
    assertEquals("two", queue.peek());
    assertEquals(3, queue.size());

    assertEquals("two", queue.remove());
    assertEquals(2, queue.size());
  }

  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(EvictingQueue.class);
    tester.testAllPublicConstructors(EvictingQueue.class);
    EvictingQueue<String> queue = EvictingQueue.create(5);
    // The queue must be non-empty so it throws a NPE correctly
    queue.add("one");
    tester.testAllPublicInstanceMethods(queue);
  }
}
