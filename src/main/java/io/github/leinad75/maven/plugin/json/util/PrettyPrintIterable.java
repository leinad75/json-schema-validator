package io.github.leinad75.maven.plugin.json.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PrettyPrintIterable<T> implements Iterable<T>{

  private final Iterable<T> iterable;

  public PrettyPrintIterable(Iterable<T> iterable) {
    this.iterable = iterable;
  }
  @Override
  public Iterator<T> iterator() {
    return iterable.iterator();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    Iterable.super.forEach(action);
  }

  @Override
  public Spliterator<T> spliterator() {
    return Iterable.super.spliterator();
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('[');
    Iterator<T> iterator = iterator();
    if (iterator.hasNext()) {
      builder.append(iterator.next().toString());
    }
    while (iterator.hasNext()) {
      builder.append(System.lineSeparator());
      builder.append(iterator.next().toString());
    }
    builder.append(']');
    return builder.toString();
  }
}
