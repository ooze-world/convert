package me.nullicorn.ooze.convert;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.nullicorn.nedit.type.NBTCompound;

/**
 * An immutable wrapper around the NBTCompound class.
 *
 * @author Nullicorn
 * @implNote Created externally via {@link NbtHelper#copyToImmutable(NBTCompound) NbtHelper}.
 */
class ImmutableCompound extends NBTCompound {

  /**
   * Helper function that throws an {@link UnsupportedOperationException}. Should be called by all
   * methods that could otherwise modify the compound's state.
   *
   * @param <T> Ignored. Allows the function to be called from {@code return}.
   */
  private static <T> T throwUnsupported() {
    throw new UnsupportedOperationException("Cannot modify immutable compound");
  }

  /**
   * @throws IllegalArgumentException if the {@code source} compound is {@code null}.
   */
  ImmutableCompound(NBTCompound source) {
    if (source == null) {
      throw new IllegalArgumentException("source compound cannot be null");
    }
    source.forEach((name, value) -> super.put(name, NbtHelper.deepCopy(value)));
  }

  @Override
  public Object put(String name, Object value) {
    return throwUnsupported();
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    throwUnsupported();
  }

  @Override
  public Object putIfAbsent(String name, Object value) {
    return throwUnsupported();
  }

  @Override
  public Object compute(String key,
      BiFunction<? super String, ? super Object, ?> remappingFunction) {
    return throwUnsupported();
  }

  @Override
  public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
    return throwUnsupported();
  }

  @Override
  public Object computeIfPresent(String key,
      BiFunction<? super String, ? super Object, ?> remappingFunction) {
    return throwUnsupported();
  }

  @Override
  public Object remove(Object name) {
    return throwUnsupported();
  }

  @Override
  public boolean remove(Object name, Object value) {
    return throwUnsupported();
  }

  @Override
  public boolean replace(String key, Object oldValue, Object newValue) {
    return throwUnsupported();
  }

  @Override
  public Object replace(String key, Object value) {
    return throwUnsupported();
  }

  @Override
  public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
    throwUnsupported();
  }

  @Override
  public Object merge(String key, Object value,
      BiFunction<? super Object, ? super Object, ?> remappingFunction) {
    return throwUnsupported();
  }

  @Override
  public void clear() {
    throwUnsupported();
  }

  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(super.keySet());
  }

  @Override
  public Collection<Object> values() {
    return Collections.unmodifiableCollection(super.values());
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    // Make the entries themselves immutable.
    Set<Entry<String, Object>> immutableEntries = super.entrySet().stream().map(
        entry -> new Entry<String, Object>() {

          @Override
          public String getKey() {
            return entry.getKey();
          }

          @Override
          public Object getValue() {
            return entry.getValue();
          }

          @Override
          public Object setValue(Object value) {
            return throwUnsupported();
          }
        }).collect(Collectors.toSet());

    // Make the set itself immutable.
    return Collections.unmodifiableSet(immutableEntries);
  }
}
