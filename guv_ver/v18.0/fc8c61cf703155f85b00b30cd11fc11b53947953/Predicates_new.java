/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to {@code Predicate} instances.
 *
 * <p>All methods returns serializable predicates as long as they're given
 * serializable parameters.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/FunctionalExplained">the
 * use of {@code Predicate}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
public final class Predicates {
  private Predicates() {}

  // TODO(kevinb): considering having these implement a VisitablePredicate
  // interface which specifies an accept(PredicateVisitor) method.

  /**
   * Returns a predicate that always evaluates to {@code true}.
   */
  @GwtCompatible(serializable = true)
  public static <T> Predicate<T> alwaysTrue() {
    return ObjectPredicate.ALWAYS_TRUE.withNarrowedType();
  }

  /**
   * Returns a predicate that always evaluates to {@code false}.
   */
  @GwtCompatible(serializable = true)
  public static <T> Predicate<T> alwaysFalse() {
    return ObjectPredicate.ALWAYS_FALSE.withNarrowedType();
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the object reference
   * being tested is null.
   */
  @GwtCompatible(serializable = true)
  public static <T> Predicate<T> isNull() {
    return ObjectPredicate.IS_NULL.withNarrowedType();
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the object reference
   * being tested is not null.
   */
  @GwtCompatible(serializable = true)
  public static <T> Predicate<T> notNull() {
    return ObjectPredicate.NOT_NULL.withNarrowedType();
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the given predicate
   * evaluates to {@code false}.
   */
  public static <T> Predicate<T> not(Predicate<T> predicate) {
    return new NotPredicate<T>(predicate);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if each of its
   * components evaluates to {@code true}. The components are evaluated in
   * order, and evaluation will be "short-circuited" as soon as a false
   * predicate is found. It defensively copies the iterable passed in, so future
   * changes to it won't alter the behavior of this predicate. If {@code
   * components} is empty, the returned predicate will always evaluate to {@code
   * true}.
   */
  public static <T> Predicate<T> and(
      Iterable<? extends Predicate<? super T>> components) {
    return new AndPredicate<T>(defensiveCopy(components));
  }

  /**
   * Returns a predicate that evaluates to {@code true} if each of its
   * components evaluates to {@code true}. The components are evaluated in
   * order, and evaluation will be "short-circuited" as soon as a false
   * predicate is found. It defensively copies the array passed in, so future
   * changes to it won't alter the behavior of this predicate. If {@code
   * components} is empty, the returned predicate will always evaluate to {@code
   * true}.
   */
  public static <T> Predicate<T> and(Predicate<? super T>... components) {
    return new AndPredicate<T>(defensiveCopy(components));
  }

  /**
   * Returns a predicate that evaluates to {@code true} if both of its
   * components evaluate to {@code true}. The components are evaluated in
   * order, and evaluation will be "short-circuited" as soon as a false
   * predicate is found.
   */
  public static <T> Predicate<T> and(Predicate<? super T> first,
      Predicate<? super T> second) {
    return new AndPredicate<T>(Predicates.<T>asList(
        checkNotNull(first), checkNotNull(second)));
  }

  /**
   * Returns a predicate that evaluates to {@code true} if any one of its
   * components evaluates to {@code true}. The components are evaluated in
   * order, and evaluation will be "short-circuited" as soon as a
   * true predicate is found. It defensively copies the iterable passed in, so
   * future changes to it won't alter the behavior of this predicate. If {@code
   * components} is empty, the returned predicate will always evaluate to {@code
   * false}.
   */
  public static <T> Predicate<T> or(
      Iterable<? extends Predicate<? super T>> components) {
    return new OrPredicate<T>(defensiveCopy(components));
  }

  /**
   * Returns a predicate that evaluates to {@code true} if any one of its
   * components evaluates to {@code true}. The components are evaluated in
   * order, and evaluation will be "short-circuited" as soon as a
   * true predicate is found. It defensively copies the array passed in, so
   * future changes to it won't alter the behavior of this predicate. If {@code
   * components} is empty, the returned predicate will always evaluate to {@code
   * false}.
   */
  public static <T> Predicate<T> or(Predicate<? super T>... components) {
    return new OrPredicate<T>(defensiveCopy(components));
  }

  /**
   * Returns a predicate that evaluates to {@code true} if either of its
   * components evaluates to {@code true}. The components are evaluated in
   * order, and evaluation will be "short-circuited" as soon as a
   * true predicate is found.
   */
  public static <T> Predicate<T> or(
      Predicate<? super T> first, Predicate<? super T> second) {
    return new OrPredicate<T>(Predicates.<T>asList(
        checkNotNull(first), checkNotNull(second)));
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the object being
   * tested {@code equals()} the given target or both are null.
   */
  public static <T> Predicate<T> equalTo(@Nullable T target) {
    return (target == null)
        ? Predicates.<T>isNull()
        : new IsEqualToPredicate<T>(target);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the object reference
   * being tested is a member of the given collection. It does not defensively
   * copy the collection passed in, so future changes to it will alter the
   * behavior of the predicate.
   *
   * <p>This method can technically accept any {@code Collection<?>}, but using
   * a typed collection helps prevent bugs. This approach doesn't block any
   * potential users since it is always possible to use {@code
   * Predicates.<Object>in()}.
   *
   * @param target the collection that may contain the function input
   */
  public static <T> Predicate<T> in(Collection<? extends T> target) {
    return new InPredicate<T>(target);
  }

  /**
   * Returns the composition of a function and a predicate. For every {@code x},
   * the generated predicate returns {@code predicate(function(x))}.
   *
   * @return the composition of the provided function and predicate
   */
  public static <A, B> Predicate<A> compose(
      Predicate<B> predicate, Function<A, ? extends B> function) {
    return new CompositionPredicate<A, B>(predicate, function);
  }

  // End public API, begin private implementation classes.

  // Package private for GWT serialization.
  enum ObjectPredicate implements Predicate<Object> {
    ALWAYS_TRUE {
      @Override public boolean apply(@Nullable Object o) {
        return true;
      }
    },
    ALWAYS_FALSE {
      @Override public boolean apply(@Nullable Object o) {
        return false;
      }
    },
    IS_NULL {
      @Override public boolean apply(@Nullable Object o) {
        return o == null;
      }
    },
    NOT_NULL {
      @Override public boolean apply(@Nullable Object o) {
        return o != null;
      }
    };

    @SuppressWarnings("unchecked") // safe contravariant cast
    <T> Predicate<T> withNarrowedType() {
      return (Predicate<T>) this;
    }
  }

  /** @see Predicates#not(Predicate) */
  private static class NotPredicate<T> implements Predicate<T>, Serializable {
    final Predicate<T> predicate;

    NotPredicate(Predicate<T> predicate) {
      this.predicate = checkNotNull(predicate);
    }
    @Override
    public boolean apply(@Nullable T t) {
      return !predicate.apply(t);
    }
    @Override public int hashCode() {
      return ~predicate.hashCode();
    }
    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof NotPredicate) {
        NotPredicate<?> that = (NotPredicate<?>) obj;
        return predicate.equals(that.predicate);
      }
      return false;
    }
    @Override public String toString() {
      return "Not(" + predicate.toString() + ")";
    }
    private static final long serialVersionUID = 0;
  }

  private static final Joiner COMMA_JOINER = Joiner.on(",");

  /** @see Predicates#and(Iterable) */
  private static class AndPredicate<T> implements Predicate<T>, Serializable {
    private final List<? extends Predicate<? super T>> components;

    private AndPredicate(List<? extends Predicate<? super T>> components) {
      this.components = components;
    }
    @Override
    public boolean apply(@Nullable T t) {
      // Avoid using the Iterator to avoid generating garbage (issue 820).
      for (int i = 0; i < components.size(); i++) {
        if (!components.get(i).apply(t)) {
          return false;
        }
      }
      return true;
    }
    @Override public int hashCode() {
      // add a random number to avoid collisions with OrPredicate
      return components.hashCode() + 0x12472c2c;
    }
    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof AndPredicate) {
        AndPredicate<?> that = (AndPredicate<?>) obj;
        return components.equals(that.components);
      }
      return false;
    }
    @Override public String toString() {
      return "And(" + COMMA_JOINER.join(components) + ")";
    }
    private static final long serialVersionUID = 0;
  }

  /** @see Predicates#or(Iterable) */
  private static class OrPredicate<T> implements Predicate<T>, Serializable {
    private final List<? extends Predicate<? super T>> components;

    private OrPredicate(List<? extends Predicate<? super T>> components) {
      this.components = components;
    }
    @Override
    public boolean apply(@Nullable T t) {
      // Avoid using the Iterator to avoid generating garbage (issue 820).
      for (int i = 0; i < components.size(); i++) {
        if (components.get(i).apply(t)) {
          return true;
        }
      }
      return false;
    }
    @Override public int hashCode() {
      // add a random number to avoid collisions with AndPredicate
      return components.hashCode() + 0x053c91cf;
    }
    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof OrPredicate) {
        OrPredicate<?> that = (OrPredicate<?>) obj;
        return components.equals(that.components);
      }
      return false;
    }
    @Override public String toString() {
      return "Or(" + COMMA_JOINER.join(components) + ")";
    }
    private static final long serialVersionUID = 0;
  }

  /** @see Predicates#equalTo(Object) */
  private static class IsEqualToPredicate<T>
      implements Predicate<T>, Serializable {
    private final T target;

    private IsEqualToPredicate(T target) {
      this.target = target;
    }
    @Override
    public boolean apply(T t) {
      return target.equals(t);
    }
    @Override public int hashCode() {
      return target.hashCode();
    }
    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof IsEqualToPredicate) {
        IsEqualToPredicate<?> that = (IsEqualToPredicate<?>) obj;
        return target.equals(that.target);
      }
      return false;
    }
    @Override public String toString() {
      return "IsEqualTo(" + target + ")";
    }
    private static final long serialVersionUID = 0;
  }

  /** @see Predicates#in(Collection) */
  private static class InPredicate<T> implements Predicate<T>, Serializable {
    private final Collection<?> target;

    private InPredicate(Collection<?> target) {
      this.target = checkNotNull(target);
    }

    @Override
    public boolean apply(@Nullable T t) {
      try {
        return target.contains(t);
      } catch (NullPointerException e) {
        return false;
      } catch (ClassCastException e) {
        return false;
      }
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof InPredicate) {
        InPredicate<?> that = (InPredicate<?>) obj;
        return target.equals(that.target);
      }
      return false;
    }

    @Override public int hashCode() {
      return target.hashCode();
    }

    @Override public String toString() {
      return "In(" + target + ")";
    }
    private static final long serialVersionUID = 0;
  }

  /** @see Predicates#compose(Predicate, Function) */
  private static class CompositionPredicate<A, B>
      implements Predicate<A>, Serializable {
    final Predicate<B> p;
    final Function<A, ? extends B> f;

    private CompositionPredicate(Predicate<B> p, Function<A, ? extends B> f) {
      this.p = checkNotNull(p);
      this.f = checkNotNull(f);
    }

    @Override
    public boolean apply(@Nullable A a) {
      return p.apply(f.apply(a));
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof CompositionPredicate) {
        CompositionPredicate<?, ?> that = (CompositionPredicate<?, ?>) obj;
        return f.equals(that.f) && p.equals(that.p);
      }
      return false;
    }

    @Override public int hashCode() {
      return f.hashCode() ^ p.hashCode();
    }

    @Override public String toString() {
      return p.toString() + "(" + f.toString() + ")";
    }

    private static final long serialVersionUID = 0;
  }

  private static <T> List<Predicate<? super T>> asList(
      Predicate<? super T> first, Predicate<? super T> second) {
    // TODO(kevinb): understand why we still get a warning despite @SafeVarargs!
    return Arrays.<Predicate<? super T>>asList(first, second);
  }

  private static <T> List<T> defensiveCopy(T... array) {
    return defensiveCopy(Arrays.asList(array));
  }

  static <T> List<T> defensiveCopy(Iterable<T> iterable) {
    ArrayList<T> list = new ArrayList<T>();
    for (T element : iterable) {
      list.add(checkNotNull(element));
    }
    return list;
  }
}

