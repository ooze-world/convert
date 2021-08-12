package me.nullicorn.ooze.convert;

/**
 * A structure for associating unique integer keys with unsigned integer values.
 *
 * @author Nullicorn
 */
public class ArrayUIntMap {

  /**
   * The number of entries currently in the map.
   */
  private int size;

  /**
   * The number of entries that the {@link #flatMap} can currently hold.
   */
  private int capacity;

  /**
   * All keys and values in the map. Keys are at even indices, and their respective values are at
   * the next odd indices.
   */
  private int[] flatMap;

  /**
   * Creates a new map with an arbitrary initial capacity.
   *
   * @see #ArrayUIntMap(int)
   */
  public ArrayUIntMap() {
    this(16);
  }

  /**
   * Creates a new map that can hold up to a certain number of entries before growing internally.
   * <p><br>
   * This does not change the map's behavior, but allows memory to be saved if the required number
   * of entries is known beforehand.
   */
  public ArrayUIntMap(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("initialCapacity cannot be negative: " + initialCapacity);
    }

    capacity = initialCapacity;
    flatMap = new int[initialCapacity * 2];
  }

  /**
   * @return the number of key/value pairs in the map.
   */
  public int size() {
    return size;
  }

  /**
   * Retrieves the value associated with a {@code key}, if it has one.
   *
   * @param key The key whose respective value should be returned.
   * @return the value for the key, or {@code -1} if none is set.
   * @see #set(int, int)
   */
  public int get(int key) {
    int keyIndex = indexOfKey(key);
    return keyIndex == -1
        ? -1
        : flatMap[keyIndex + 1];
  }

  /**
   * Associates a value with a unique key, replacing the existing value for that key if applicable.
   *
   * @param key   A unique integer that can be used to retrieve the value later.
   * @param value An integer value to associate with the key.
   * @return the replaced value, or an {@code -1} if the key was not already in the map.
   * @throws IllegalArgumentException if the {@code value} is negative.
   * @see #get(int)
   */
  public int set(int key, int value) {
    if (value < 0) {
      throw new IllegalArgumentException(value + " is not allowed in an unsigned map");
    }

    int oldValue;

    int keyIndex = indexOfKey(key);
    if (keyIndex == -1) {
      oldValue = -1;

      // We need to add the key/value pair.
      // Makes sure there's space in the array.
      if (size == capacity) {
        upscale(1 + (int) (capacity * 1.5d));
      }

      keyIndex = 2 * (size - 1);
      size++;

    } else {
      oldValue = flatMap[keyIndex + 1];
    }

    flatMap[keyIndex + 1] = value;
    return oldValue;
  }

  /**
   * @return the index in {@link #flatMap} where the {@code key} is held, or {@code -1} if the map
   * does not contain that key.
   */
  private int indexOfKey(int key) {
    for (int i = 0; i < size * 2; i += 2) {
      if (flatMap[i] == key) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Helper method for increasing the internal {@link #capacity}.
   */
  private void upscale(int newCapacity) {
    capacity = newCapacity;

    int[] tempFlatMap = flatMap;
    flatMap = new int[capacity * 2];

    System.arraycopy(tempFlatMap, 0, flatMap, 0, tempFlatMap.length);
  }
}
