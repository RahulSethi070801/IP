/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.MapMakerInternalMap.Strength.SOFT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.MapMakerInternalMap.Strength;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

/**
 * <p>A builder of {@link ConcurrentMap} instances having any combination of the following features:
 *
 * <ul>
 * <li>keys or values automatically wrapped in {@linkplain WeakReference weak} references
 * </ul>
 *
 * <p>Usage example: <pre>   {@code
 *
 *   ConcurrentMap<Request, Stopwatch> timers = new MapMaker()
 *       .concurrencyLevel(4)
 *       .weakKeys()
 *       .makeMap();}</pre>
 *
 * <p>These features are all optional; {@code new MapMaker().makeMap()} returns a valid concurrent
 * map that behaves similarly to a {@link ConcurrentHashMap}.
 *
 * <p>The returned map is implemented as a hash table with similar performance characteristics to
 * {@link ConcurrentHashMap}. It supports all optional operations of the {@code ConcurrentMap}
 * interface. It does not permit null keys or values.
 *
 * <p><b>Note:</b> by default, the returned map uses equality comparisons (the {@link Object#equals
 * equals} method) to determine equality for keys or values. However, if {@link #weakKeys} was
 * specified, the map uses identity ({@code ==}) comparisons instead for keys. Likewise, if
 * {@link #weakValues}
 * was specified, the map uses identity comparisons for values.
 *
 * <p>The view collections of the returned map have <i>weakly consistent iterators</i>. This means
 * that they are safe for concurrent use, but if other threads modify the map after the iterator is
 * created, it is undefined which of these changes, if any, are reflected in that iterator. These
 * iterators never throw {@link ConcurrentModificationException}.
 *
 * <p>If {@link #weakKeys}/{@link #weakValues}
 * are requested, it is possible for a key or value present in the map to be reclaimed by the
 * garbage collector. Entries with reclaimed keys or values may be removed from the map on each map
 * modification or on occasional map accesses; such entries may be counted by {@link Map#size}, but
 * will never be visible to read or write operations. A partially-reclaimed entry is never exposed
 * to the user. Any {@link java.util.Map.Entry} instance retrieved from the map's
 * {@linkplain Map#entrySet entry set} is a snapshot of that entry's state at the time of retrieval;
 * such entries do, however, support {@link java.util.Map.Entry#setValue}, which simply calls
 * {@link Map#put} on the entry's key.
 *
 * <p>The maps produced by {@code MapMaker} are serializable, and the deserialized maps retain all
 * the configuration properties of the original map. During deserialization, if the original map had
 * used weak
 * references, the entries are reconstructed as they were, but it's not unlikely they'll be quickly
 * garbage-collected before they are ever accessed.
 *
 * <p>{@code new MapMaker().weakKeys().makeMap()} is a recommended replacement for
 * {@link java.util.WeakHashMap}, but note that it compares keys using object identity whereas
 * {@code WeakHashMap} uses {@link Object#equals}.
 *
 * @author Bob Lee
 * @author Charles Fry
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class MapMaker extends GenericMapMaker<Object, Object> {
  private static final int DEFAULT_INITIAL_CAPACITY = 16;
  private static final int DEFAULT_CONCURRENCY_LEVEL = 4;

  static final int UNSET_INT = -1;

  // TODO(kevinb): dispense with this after benchmarking
  boolean useCustomMap;

  int initialCapacity = UNSET_INT;
  int concurrencyLevel = UNSET_INT;

  Strength keyStrength;
  Strength valueStrength;

  boolean evictImmediately = false;

  Equivalence<Object> keyEquivalence;

  /**
   * Constructs a new {@code MapMaker} instance with default settings, including strong keys, strong
   * values, and no automatic eviction of any kind.
   */
  public MapMaker() {}

  /**
   * Sets a custom {@code Equivalence} strategy for comparing keys.
   *
   * <p>By default, the map uses {@link Equivalence#identity} to determine key equality when
   * {@link #weakKeys} is specified, and {@link Equivalence#equals()} otherwise. The only place this
   * is used is in {@link Interners.WeakInterner}.
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // To be supported
  @Override
  MapMaker keyEquivalence(Equivalence<Object> equivalence) {
    checkState(keyEquivalence == null, "key equivalence was already set to %s", keyEquivalence);
    keyEquivalence = checkNotNull(equivalence);
    this.useCustomMap = true;
    return this;
  }

  Equivalence<Object> getKeyEquivalence() {
    return MoreObjects.firstNonNull(keyEquivalence, getKeyStrength().defaultEquivalence());
  }

  /**
   * Sets the minimum total size for the internal hash tables. For example, if the initial capacity
   * is {@code 60}, and the concurrency level is {@code 8}, then eight segments are created, each
   * having a hash table of size eight. Providing a large enough estimate at construction time
   * avoids the need for expensive resizing operations later, but setting this value unnecessarily
   * high wastes memory.
   *
   * @throws IllegalArgumentException if {@code initialCapacity} is negative
   * @throws IllegalStateException if an initial capacity was already set
   */
  @CanIgnoreReturnValue
  @Override
  public MapMaker initialCapacity(int initialCapacity) {
    checkState(
        this.initialCapacity == UNSET_INT,
        "initial capacity was already set to %s",
        this.initialCapacity);
    checkArgument(initialCapacity >= 0);
    this.initialCapacity = initialCapacity;
    return this;
  }

  int getInitialCapacity() {
    return (initialCapacity == UNSET_INT) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
  }

  /**
   * Guides the allowed concurrency among update operations. Used as a hint for internal sizing. The
   * table is internally partitioned to try to permit the indicated number of concurrent updates
   * without contention. Because assignment of entries to these partitions is not necessarily
   * uniform, the actual concurrency observed may vary. Ideally, you should choose a value to
   * accommodate as many threads as will ever concurrently modify the table. Using a significantly
   * higher value than you need can waste space and time, and a significantly lower value can lead
   * to thread contention. But overestimates and underestimates within an order of magnitude do not
   * usually have much noticeable impact. A value of one permits only one thread to modify the map
   * at a time, but since read operations can proceed concurrently, this still yields higher
   * concurrency than full synchronization. Defaults to 4.
   *
   * <p><b>Note:</b> Prior to Guava release 9.0, the default was 16. It is possible the default will
   * change again in the future. If you care about this value, you should always choose it
   * explicitly.
   *
   * @throws IllegalArgumentException if {@code concurrencyLevel} is nonpositive
   * @throws IllegalStateException if a concurrency level was already set
   */
  @CanIgnoreReturnValue
  @Override
  public MapMaker concurrencyLevel(int concurrencyLevel) {
    checkState(
        this.concurrencyLevel == UNSET_INT,
        "concurrency level was already set to %s",
        this.concurrencyLevel);
    checkArgument(concurrencyLevel > 0);
    this.concurrencyLevel = concurrencyLevel;
    return this;
  }

  int getConcurrencyLevel() {
    return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
  }

  /**
   * Specifies that each key (not value) stored in the map should be wrapped in a
   * {@link WeakReference} (by default, strong references are used).
   *
   * <p><b>Warning:</b> when this method is used, the resulting map will use identity ({@code ==})
   * comparison to determine equality of keys, which is a technical violation of the {@link Map}
   * specification, and may not be what you expect.
   *
   * @throws IllegalStateException if the key strength was already set
   * @see WeakReference
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // java.lang.ref.WeakReference
  @Override
  public MapMaker weakKeys() {
    return setKeyStrength(Strength.WEAK);
  }

  MapMaker setKeyStrength(Strength strength) {
    checkState(keyStrength == null, "Key strength was already set to %s", keyStrength);
    keyStrength = checkNotNull(strength);
    checkArgument(keyStrength != SOFT, "Soft keys are not supported");
    if (strength != Strength.STRONG) {
      // STRONG could be used during deserialization.
      useCustomMap = true;
    }
    return this;
  }

  Strength getKeyStrength() {
    return MoreObjects.firstNonNull(keyStrength, Strength.STRONG);
  }

  /**
   * Specifies that each value (not key) stored in the map should be wrapped in a
   * {@link WeakReference} (by default, strong references are used).
   *
   * <p>Weak values will be garbage collected once they are weakly reachable. This makes them a poor
   * candidate for caching.
   *
   * <p><b>Warning:</b> when this method is used, the resulting map will use identity ({@code ==})
   * comparison to determine equality of values. This technically violates the specifications of the
   * methods {@link Map#containsValue containsValue}, {@link ConcurrentMap#remove(Object, Object)
   * remove(Object, Object)} and {@link ConcurrentMap#replace(Object, Object, Object) replace(K, V,
   * V)}, and may not be what you expect.
   *
   * @throws IllegalStateException if the value strength was already set
   * @see WeakReference
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // java.lang.ref.WeakReference
  @Override
  public MapMaker weakValues() {
    return setValueStrength(Strength.WEAK);
  }

  /**
   * Specifies that each value (not key) stored in the map should be wrapped in a
   * {@link SoftReference} (by default, strong references are used). Softly-referenced objects will
   * be garbage-collected in a <i>globally</i> least-recently-used manner, in response to memory
   * demand.
   *
   * <p><b>Warning:</b> you should only use this method if you are well familiar with the practical
   * consequences of soft references.
   *
   * <p><b>Warning:</b> when this method is used, the resulting map will use identity ({@code ==})
   * comparison to determine equality of values. This technically violates the specifications of the
   * methods {@link Map#containsValue containsValue}, {@link ConcurrentMap#remove(Object, Object)
   * remove(Object, Object)} and {@link ConcurrentMap#replace(Object, Object, Object) replace(K, V,
   * V)}, and may not be what you expect.
   *
   * @throws IllegalStateException if the value strength was already set
   * @deprecated Caching functionality in {@code MapMaker} has been moved to
   *     {@link com.google.common.cache.CacheBuilder}, with {@link #softValues} being replaced by
   *     {@link com.google.common.cache.CacheBuilder#softValues}. Note that {@code CacheBuilder} is
   *     simply an enhanced API for an implementation which was branched from {@code MapMaker}.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @GwtIncompatible // java.lang.ref.SoftReference
  @Override
  MapMaker softValues() {
    return setValueStrength(Strength.SOFT);
  }

  MapMaker setValueStrength(Strength strength) {
    checkState(valueStrength == null, "Value strength was already set to %s", valueStrength);
    valueStrength = checkNotNull(strength);
    if (strength != Strength.STRONG) {
      // STRONG could be used during deserialization.
      useCustomMap = true;
    }
    return this;
  }

  Strength getValueStrength() {
    return MoreObjects.firstNonNull(valueStrength, Strength.STRONG);
  }

  /**
   * Builds a thread-safe map. This method does not alter the state of this {@code MapMaker}
   * instance, so it can be invoked again to create multiple independent maps.
   *
   * <p>The bulk operations {@code putAll}, {@code equals}, and {@code clear} are not guaranteed to
   * be performed atomically on the returned map. Additionally, {@code size} and
   * {@code containsValue} are implemented as bulk read operations, and thus may fail to observe
   * concurrent writes.
   *
   * @return a serializable concurrent map having the requested features
   */
  @Override
  public <K, V> ConcurrentMap<K, V> makeMap() {
    if (!useCustomMap) {
      return new ConcurrentHashMap<K, V>(getInitialCapacity(), 0.75f, getConcurrencyLevel());
    }
    return evictImmediately ? new NullConcurrentMap<K, V>() : new MapMakerInternalMap<K, V>(this);
  }

  /**
   * Returns a MapMakerInternalMap for the benefit of internal callers that use features of that
   * class not exposed through ConcurrentMap.
   */
  @Override
  @GwtIncompatible // MapMakerInternalMap
  <K, V> MapMakerInternalMap<K, V> makeCustomMap() {
    return new MapMakerInternalMap<K, V>(this);
  }

  /**
   * Builds a map that supports atomic, on-demand computation of values. {@link Map#get} either
   * returns an already-computed value for the given key, atomically computes it using the supplied
   * function, or, if another thread is currently computing the value for this key, simply waits for
   * that thread to finish and returns its computed value. Note that the function may be executed
   * concurrently by multiple threads, but only for distinct keys.
   *
   * <p>New code should use {@link com.google.common.cache.CacheBuilder}, which supports
   * {@linkplain com.google.common.cache.CacheStats statistics} collection, introduces the
   * {@link com.google.common.cache.CacheLoader} interface for loading entries into the cache
   * (allowing checked exceptions to be thrown in the process), and more cleanly separates
   * computation from the cache's {@code Map} view.
   *
   * <p>If an entry's value has not finished computing yet, query methods besides {@code get} return
   * immediately as if an entry doesn't exist. In other words, an entry isn't externally visible
   * until the value's computation completes.
   *
   * <p>{@link Map#get} on the returned map will never return {@code null}. It may throw:
   *
   * <ul>
   * <li>{@link NullPointerException} if the key is null or the computing function returns a null
   * result
   * <li>{@link ComputationException} if an exception was thrown by the computing function. If that
   * exception is already of type {@link ComputationException} it is propagated directly; otherwise
   * it is wrapped.
   * </ul>
   *
   * <p><b>Note:</b> Callers of {@code get} <i>must</i> ensure that the key argument is of type
   * {@code K}. The {@code get} method accepts {@code Object}, so the key type is not checked at
   * compile time. Passing an object of a type other than {@code K} can result in that object being
   * unsafely passed to the computing function as type {@code K}, and unsafely stored in the map.
   *
   * <p>If {@link Map#put} is called before a computation completes, other threads waiting on the
   * computation will wake up and return the stored value.
   *
   * <p>This method does not alter the state of this {@code MapMaker} instance, so it can be invoked
   * again to create multiple independent maps.
   *
   * <p>Insertion, removal, update, and access operations on the returned map safely execute
   * concurrently by multiple threads. Iterators on the returned map are weakly consistent,
   * returning elements reflecting the state of the map at some point at or since the creation of
   * the iterator. They do not throw {@link ConcurrentModificationException}, and may proceed
   * concurrently with other operations.
   *
   * <p>The bulk operations {@code putAll}, {@code equals}, and {@code clear} are not guaranteed to
   * be performed atomically on the returned map. Additionally, {@code size} and
   * {@code containsValue} are implemented as bulk read operations, and thus may fail to observe
   * concurrent writes.
   *
   * @param computingFunction the function used to compute new values
   * @return a serializable concurrent map having the requested features
   * @deprecated Caching functionality in {@code MapMaker} has been moved to
   *     {@link com.google.common.cache.CacheBuilder}, with {@link #makeComputingMap} being replaced
   *     by {@link com.google.common.cache.CacheBuilder#build}. See the
   *     <a href="https://github.com/google/guava/wiki/MapMakerMigration">MapMaker Migration
   *     Guide</a> for more details.
   */
  @Deprecated
  @Override
  <K, V> ConcurrentMap<K, V> makeComputingMap(Function<? super K, ? extends V> computingFunction) {
    return evictImmediately
        ? new NullComputingConcurrentMap<K, V>(computingFunction)
        : new MapMaker.ComputingMapAdapter<K, V>(this, computingFunction);
  }

  /**
   * Returns a string representation for this MapMaker instance. The exact form of the returned
   * string is not specificed.
   */
  @Override
  public String toString() {
    MoreObjects.ToStringHelper s = MoreObjects.toStringHelper(this);
    if (initialCapacity != UNSET_INT) {
      s.add("initialCapacity", initialCapacity);
    }
    if (concurrencyLevel != UNSET_INT) {
      s.add("concurrencyLevel", concurrencyLevel);
    }
    if (keyStrength != null) {
      s.add("keyStrength", Ascii.toLowerCase(keyStrength.toString()));
    }
    if (valueStrength != null) {
      s.add("valueStrength", Ascii.toLowerCase(valueStrength.toString()));
    }
    if (keyEquivalence != null) {
      s.addValue("keyEquivalence");
    }
    return s.toString();
  }

  /** A map that is always empty and evicts on insertion. */
  static class NullConcurrentMap<K, V> extends AbstractMap<K, V>
      implements ConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 0;

    // implements ConcurrentMap

    @Override
    public boolean containsKey(@Nullable Object key) {
      return false;
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
      return false;
    }

    @Override
    public V get(@Nullable Object key) {
      return null;
    }

    @Override
    public V put(K key, V value) {
      checkNotNull(key);
      checkNotNull(value);
      return null;
    }

    @Override
    public V putIfAbsent(K key, V value) {
      return put(key, value);
    }

    @Override
    public V remove(@Nullable Object key) {
      return null;
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
      return false;
    }

    @Override
    public V replace(K key, V value) {
      checkNotNull(key);
      checkNotNull(value);
      return null;
    }

    @Override
    public boolean replace(K key, @Nullable V oldValue, V newValue) {
      checkNotNull(key);
      checkNotNull(newValue);
      return false;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return Collections.emptySet();
    }
  }

  /** Computes on retrieval and evicts the result. */
  static final class NullComputingConcurrentMap<K, V> extends NullConcurrentMap<K, V> {
    private static final long serialVersionUID = 0;

    final Function<? super K, ? extends V> computingFunction;

    NullComputingConcurrentMap(Function<? super K, ? extends V> computingFunction) {
      this.computingFunction = checkNotNull(computingFunction);
    }

    @SuppressWarnings("unchecked") // unsafe, which is why Cache is preferred
    @Override
    public V get(Object k) {
      K key = (K) k;
      V value = compute(key);
      checkNotNull(value, "%s returned null for key %s.", computingFunction, key);
      return value;
    }

    private V compute(K key) {
      checkNotNull(key);
      try {
        return computingFunction.apply(key);
      } catch (ComputationException e) {
        throw e;
      } catch (Throwable t) {
        throw new ComputationException(t);
      }
    }
  }

  /**
   * Overrides get() to compute on demand. Also throws an exception when {@code null} is returned
   * from a computation.
   */
  /*
   * This might make more sense in ComputingConcurrentHashMap, but it causes a javac crash in some
   * cases there: http://code.google.com/p/guava-libraries/issues/detail?id=950
   */
  static final class ComputingMapAdapter<K, V> extends ComputingConcurrentHashMap<K, V>
      implements Serializable {
    private static final long serialVersionUID = 0;

    ComputingMapAdapter(MapMaker mapMaker, Function<? super K, ? extends V> computingFunction) {
      super(mapMaker, computingFunction);
    }

    @SuppressWarnings("unchecked") // unsafe, which is one advantage of Cache over Map
    @Override
    public V get(Object key) {
      V value;
      try {
        value = getOrCompute((K) key);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        Throwables.propagateIfInstanceOf(cause, ComputationException.class);
        throw new ComputationException(cause);
      }

      if (value == null) {
        throw new NullPointerException(computingFunction + " returned null for key " + key + ".");
      }
      return value;
    }
  }
}
