/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package javaslang.collection;

import javaslang.JmhRunner;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static javaslang.JmhRunner.create;
import static javaslang.JmhRunner.getRandomValues;
import static javaslang.collection.Collections.areEqual;
import static scala.collection.JavaConverters.asJavaCollection;
import static scala.collection.JavaConverters.asScalaBuffer;

public class ListBenchmark {
    static final Array<Class<?>> CLASSES = Array.of(
            Create.class,
            Head.class,
            Tail.class,
            Get.class,
            Update.class,
            Prepend.class,
            Append.class,
            GroupBy.class,
            Iterate.class
    );

    @Test
    public void testAsserts() {
        JmhRunner.runDebugWithAsserts(CLASSES);
    }

    public static void main(String... args) {
        JmhRunner.runNormalNoAsserts(CLASSES);
    }

    @State(Scope.Benchmark)
    public static class Base {
        @Param({ "10", "100", "1000" })
        public int CONTAINER_SIZE;

        int EXPECTED_AGGREGATE;
        Integer[] ELEMENTS;

        /* Only use these for non-mutating operations */
        java.util.ArrayList<Integer> javaMutable;
        java.util.LinkedList<Integer> javaMutableLinked;
        scala.collection.mutable.MutableList<Integer> scalaMutable;

        fj.data.List<Integer> fjavaPersistent;
        org.pcollections.PStack<Integer> pcollectionsPersistent;
        scala.collection.immutable.List<Integer> scalaPersistent;
        clojure.lang.IPersistentList clojurePersistent;
        javaslang.collection.List<Integer> slangPersistent;

        @Setup
        @SuppressWarnings("unchecked")
        public void setup() {
            ELEMENTS = getRandomValues(CONTAINER_SIZE, 0);
            EXPECTED_AGGREGATE = Iterator.of(ELEMENTS).reduce(JmhRunner::aggregate);

            javaMutable = create(java.util.ArrayList::new, asList(ELEMENTS), v -> areEqual(v, asList(ELEMENTS)));
            javaMutableLinked = create(java.util.LinkedList::new, asList(ELEMENTS), v -> areEqual(v, asList(ELEMENTS)));
            scalaMutable = create(v -> (scala.collection.mutable.MutableList<Integer>) scala.collection.mutable.MutableList$.MODULE$.apply(asScalaBuffer(v)), asList(ELEMENTS), v -> areEqual(asJavaCollection(v), javaMutable));

            scalaPersistent = create(v -> scala.collection.immutable.List$.MODULE$.apply(asScalaBuffer(v)), javaMutable, v -> areEqual(asJavaCollection(v), javaMutable));
            clojurePersistent = create(clojure.lang.PersistentList::create, javaMutable, v -> areEqual((Iterable<?>) v, javaMutable));
            fjavaPersistent = create(v -> fj.data.List.fromIterator(v.iterator()), javaMutable, v -> areEqual(v, javaMutable));
            pcollectionsPersistent = create(org.pcollections.ConsPStack::from, javaMutable, v -> areEqual(v, javaMutable));
            slangPersistent = create(javaslang.collection.List::ofAll, javaMutable, v -> areEqual(v, javaMutable));
        }
    }

    public static class Create extends Base {
        @Benchmark
        public Object java_mutable() {
            final ArrayList<Integer> values = new ArrayList<>(javaMutable);
            assert areEqual(values, javaMutable);
            return values;
        }

        @Benchmark
        public Object scala_persistent() {
            final scala.collection.immutable.List<?> values = scala.collection.immutable.List$.MODULE$.apply(scalaMutable);
            assert Objects.equals(values, scalaPersistent);
            return values;
        }

        @Benchmark
        public Object clojure_persistent() {
            final clojure.lang.IPersistentStack values = clojure.lang.PersistentList.create(javaMutable);
            assert Objects.equals(values, clojurePersistent);
            return values;
        }

        @Benchmark
        public Object fjava_persistent() {
            final fj.data.List<Integer> values = fj.data.List.fromIterator(javaMutable.iterator());
            assert areEqual(values, javaMutable);
            return values;
        }

        @Benchmark
        public Object pcollections_persistent() {
            final org.pcollections.PStack<Integer> values = org.pcollections.ConsPStack.from(javaMutable);
            assert areEqual(values, javaMutable);
            return values;
        }

        @Benchmark
        public Object slang_persistent() {
            final javaslang.collection.List<Integer> values = javaslang.collection.List.ofAll(javaMutable);
            assert areEqual(values, javaMutable);
            return values.head();
        }
    }

    public static class Head extends Base {
        @Benchmark
        public Object java_mutable() {
            final Object head = javaMutable.get(0);
            assert Objects.equals(head, ELEMENTS[0]);
            return head;
        }

        @Benchmark
        public Object scala_persistent() {
            final Object head = scalaPersistent.head();
            assert Objects.equals(head, javaMutable.get(0));
            return head;
        }

        @Benchmark
        public Object clojure_persistent() {
            final Object head = clojurePersistent.peek();
            assert Objects.equals(head, javaMutable.get(0));
            return head;
        }

        @Benchmark
        public Object fjava_persistent() {
            final Object head = fjavaPersistent.head();
            assert Objects.equals(head, javaMutable.get(0));
            return head;
        }

        @Benchmark
        public Object pcollections_persistent() {
            final Object head = pcollectionsPersistent.get(0);
            assert Objects.equals(head, javaMutable.get(0));
            return head;
        }

        @Benchmark
        public Object slang_persistent() {
            final Object head = slangPersistent.head();
            assert Objects.equals(head, javaMutable.get(0));
            return head;
        }
    }

    @SuppressWarnings("Convert2MethodRef")
    public static class Tail extends Base {
        @State(Scope.Thread)
        public static class Initialized {
            final java.util.ArrayList<Integer> javaMutable = new java.util.ArrayList<>();
            final java.util.LinkedList<Integer> javaMutableLinked = new java.util.LinkedList<>();

            @Setup(Level.Invocation)
            public void initializeMutable(Base state) {
                java.util.Collections.addAll(javaMutable, state.ELEMENTS);
                javaMutableLinked.addAll(javaMutable);
                assert areEqual(javaMutable, asList(state.ELEMENTS))
                        && areEqual(javaMutableLinked, javaMutable);
            }

            @TearDown(Level.Invocation)
            public void tearDown() {
                javaMutable.clear();
                javaMutableLinked.clear();
            }
        }

        @Benchmark
        public Object java_mutable(Initialized state) {
            final java.util.ArrayList<Integer> values = state.javaMutable;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values.remove(0);
            }
            assert values.isEmpty();
            return values;
        }

        @Benchmark
        public Object java_linked_mutable(Initialized state) {
            final java.util.LinkedList<Integer> values = state.javaMutableLinked;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values.remove(0);
            }
            assert values.isEmpty();
            return values;
        }

        @Benchmark
        @SuppressWarnings({ "unchecked", "RedundantCast" })
        public Object scala_persistent() {
            scala.collection.immutable.List<Integer> values = scalaPersistent;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values = (scala.collection.immutable.List<Integer>) values.tail();
            }
            assert values.isEmpty();
            return values;
        }

        @Benchmark
        public Object clojure_persistent() {
            clojure.lang.IPersistentStack values = clojurePersistent;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values = values.pop();
            }
            assert Objects.equals(values, clojure.lang.PersistentList.EMPTY);
            return values;
        }

        @Benchmark
        public Object fjava_persistent() {
            fj.data.List<Integer> values = fjavaPersistent;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values = values.tail();
            }
            assert values.isEmpty();
            return values;
        }

        @Benchmark
        public Object pcollections_persistent() {
            org.pcollections.PStack<Integer> values = pcollectionsPersistent;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values = values.minus(0);
            }
            assert values.isEmpty();
            return values;
        }

        @Benchmark
        public Object slang_persistent() {
            javaslang.collection.List<Integer> values = slangPersistent;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values = values.tail();
            }
            assert values.isEmpty();
            return values;
        }
    }

    public static class Get extends Base {
        @Benchmark
        public int java_mutable() {
            int aggregate = 0;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                aggregate ^= javaMutable.get(i);
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int java_linked_mutable() {
            int aggregate = 0;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                aggregate ^= javaMutableLinked.get(i);
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int scala_persistent() {
            int aggregate = 0;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                aggregate ^= scalaPersistent.apply(i);
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int fjava_persistent() {
            int aggregate = 0;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                aggregate ^= fjavaPersistent.index(i);
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int pcollections_persistent() {
            int aggregate = 0;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                aggregate ^= pcollectionsPersistent.get(i);
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int slang_persistent() {
            int aggregate = 0;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                aggregate ^= slangPersistent.get(i);
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }
    }

    public static class Update extends Base {
        @State(Scope.Thread)
        public static class Initialized {
            final java.util.ArrayList<Integer> javaMutable = new java.util.ArrayList<>();
            final java.util.LinkedList<Integer> javaMutableLinked = new java.util.LinkedList<>();
            final scala.collection.mutable.MutableList<Integer> scalaMutable = new scala.collection.mutable.MutableList<>();

            @Setup(Level.Invocation)
            public void initializeMutable(Base state) {
                java.util.Collections.addAll(javaMutable, state.ELEMENTS);
                java.util.Collections.addAll(javaMutableLinked, state.ELEMENTS);
                for (int i = state.CONTAINER_SIZE - 1; i >= 0; i--) {
                    scalaMutable.prependElem(state.ELEMENTS[i]);
                }

                assert areEqual(javaMutable, asList(state.ELEMENTS))
                        && areEqual(javaMutableLinked, javaMutable)
                        && areEqual(asJavaCollection(scalaMutable), javaMutable);
            }

            @TearDown(Level.Invocation)
            public void tearDown() {
                javaMutable.clear();
                javaMutableLinked.clear();
                scalaMutable.clear();
            }
        }

        @Benchmark
        public Object java_mutable(Initialized state) {
            final java.util.ArrayList<Integer> values = state.javaMutable;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values.set(i, 0);
            }
            assert Array.ofAll(values).forAll(e -> e == 0);
            return values;
        }

        @Benchmark
        public Object java_linked_mutable(Initialized state) {
            final java.util.LinkedList<Integer> values = state.javaMutableLinked;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values.set(i, 0);
            }
            assert Array.ofAll(values).forAll(e -> e == 0);
            return values;
        }

        @Benchmark
        public Object scala_mutable(Initialized state) {
            final scala.collection.mutable.MutableList<Integer> values = state.scalaMutable;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values.update(i, 0);
            }
            assert Array.ofAll(asJavaCollection(values)).forAll(e -> e == 0);
            return values;
        }

        @Benchmark
        public Object pcollections_persistent() {
            org.pcollections.PStack<Integer> values = pcollectionsPersistent;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values = values.with(i, 0);
            }
            assert Array.ofAll(values).forAll(e -> e == 0);
            return values;
        }

        @Benchmark
        public Object slang_persistent() {
            javaslang.collection.List<Integer> values = slangPersistent;
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                values = values.update(i, 0);
            }
            assert values.forAll(e -> e == 0);
            return values;
        }
    }

    @SuppressWarnings("ManualArrayToCollectionCopy")
    public static class Prepend extends Base {
        @Benchmark
        public Object java_mutable() {
            final java.util.ArrayList<Integer> values = new java.util.ArrayList<>(CONTAINER_SIZE);
            for (Integer element : ELEMENTS) {
                values.add(0, element);
            }
            assert areEqual(Array.ofAll(values).reverse(), javaMutable);
            return values;
        }

        @Benchmark
        public Object java_linked_mutable() {
            final java.util.LinkedList<Integer> values = new java.util.LinkedList<>();
            for (Integer element : ELEMENTS) {
                values.addFirst(element);
            }
            assert areEqual(Array.ofAll(values).reverse(), javaMutable);
            return values;
        }

        @Benchmark
        public Object scala_mutable() {
            final scala.collection.mutable.MutableList<Integer> values = new scala.collection.mutable.MutableList<>();
            for (Integer element : ELEMENTS) {
                values.prependElem(element);
            }
            assert areEqual(Array.ofAll(asJavaCollection(values)).reverse(), javaMutable);
            return values;
        }

        @Benchmark
        public Object scala_persistent() {
            scala.collection.immutable.List<Integer> values = scala.collection.immutable.List$.MODULE$.empty();
            for (Integer element : ELEMENTS) {
                values = values.$colon$colon(element);
            }
            assert areEqual(Array.ofAll(asJavaCollection(values)).reverse(), javaMutable);
            return values;
        }

        @Benchmark
        public Object fjava_persistent() {
            fj.data.List<Integer> values = fj.data.List.list();
            for (Integer element : ELEMENTS) {
                values = values.cons(element);
            }
            assert areEqual(Array.ofAll(values).reverse(), javaMutable);
            return values;
        }

        @Benchmark
        public Object pcollections_persistent() {
            org.pcollections.PStack<Integer> values = org.pcollections.ConsPStack.empty();
            for (Integer element : ELEMENTS) {
                values = values.plus(element);
            }
            assert areEqual(Array.ofAll(values).reverse(), javaMutable);
            return values;
        }

        @Benchmark
        public Object slang_persistent() {
            javaslang.collection.List<Integer> values = javaslang.collection.List.empty();
            for (Integer element : ELEMENTS) {
                values = values.prepend(element);
            }
            assert areEqual(values.reverse(), javaMutable);
            return values;
        }
    }

    @SuppressWarnings("ManualArrayToCollectionCopy")
    public static class Append extends Base {
        @Benchmark
        public Object java_mutable() {
            final java.util.ArrayList<Integer> values = new java.util.ArrayList<>(CONTAINER_SIZE);
            for (Integer element : ELEMENTS) {
                values.add(element);
            }
            assert areEqual(values, javaMutable);
            return values;
        }

        @Benchmark
        public Object java_linked_mutable() {
            final java.util.LinkedList<Integer> values = new java.util.LinkedList<>();
            for (Integer element : ELEMENTS) {
                values.addLast(element);
            }
            assert values.size() == CONTAINER_SIZE;
            return values;
        }

        @Benchmark
        public Object scala_mutable() {
            final scala.collection.mutable.MutableList<Integer> values = new scala.collection.mutable.MutableList<>();
            for (Integer element : ELEMENTS) {
                values.appendElem(element);
            }
            assert areEqual(asJavaCollection(values), javaMutable);
            return values;
        }

        @Benchmark
        public Object fjava_persistent() {
            fj.data.List<Integer> values = fj.data.List.list();
            for (Integer element : ELEMENTS) {
                values = values.snoc(element);
            }
            assert areEqual(values, javaMutable);
            return values;
        }

        @Benchmark
        public Object pcollections_persistent() {
            org.pcollections.PStack<Integer> values = org.pcollections.ConsPStack.empty();
            for (Integer element : ELEMENTS) {
                values = values.plus(values.size(), element);
            }
            assert areEqual(values, javaMutable);
            return values;
        }

        @Benchmark
        public Object slang_persistent() {
            javaslang.collection.List<Integer> values = javaslang.collection.List.empty();
            for (Integer element : ELEMENTS) {
                values = values.append(element);
            }
            assert areEqual(values, javaMutable);
            return values;
        }
    }

    public static class GroupBy extends Base {
        @Benchmark
        public Object java_mutable() {
            return javaMutable.stream().collect(Collectors.groupingBy(Integer::bitCount));
        }

        @Benchmark
        public Object scala_persistent() {
            return scalaPersistent.groupBy(Integer::bitCount);
        }

        @Benchmark
        public Object fjava_persistent() {
            return fjavaPersistent.groupBy(Integer::bitCount);
        }

        @Benchmark
        public Object slang_persistent() {
            return slangPersistent.groupBy(Integer::bitCount);
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public static class Iterate extends Base {
        @Benchmark
        public int java_mutable() {
            int aggregate = 0;
            for (final java.util.Iterator<Integer> iterator = javaMutable.iterator(); iterator.hasNext(); ) {
                aggregate ^= iterator.next();
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int java_linked_mutable() {
            int aggregate = 0;
            for (final java.util.Iterator<Integer> iterator = javaMutableLinked.iterator(); iterator.hasNext(); ) {
                aggregate ^= iterator.next();
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int scala_mutable() {
            int aggregate = 0;
            for (final scala.collection.Iterator<Integer> iterator = scalaMutable.iterator(); iterator.hasNext(); ) {
                aggregate ^= iterator.next();
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int scala_persistent() {
            int aggregate = 0;
            for (final scala.collection.Iterator<Integer> iterator = scalaPersistent.iterator(); iterator.hasNext(); ) {
                aggregate ^= iterator.next();
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int fjava_persistent() {
            int aggregate = 0;
            for (final java.util.Iterator<Integer> iterator = fjavaPersistent.iterator(); iterator.hasNext(); ) {
                aggregate ^= iterator.next();
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int pcollections_persistent() {
            int aggregate = 0;
            for (final java.util.Iterator<Integer> iterator = pcollectionsPersistent.iterator(); iterator.hasNext(); ) {
                aggregate ^= iterator.next();
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }

        @Benchmark
        public int slang_persistent() {
            int aggregate = 0;
            for (final java.util.Iterator<Integer> iterator = slangPersistent.iterator(); iterator.hasNext(); ) {
                aggregate ^= iterator.next();
            }
            assert aggregate == EXPECTED_AGGREGATE;
            return aggregate;
        }
    }
}