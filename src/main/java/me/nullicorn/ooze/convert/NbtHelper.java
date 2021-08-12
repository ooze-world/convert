package me.nullicorn.ooze.convert;

import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import me.nullicorn.nedit.NBTInputStream;
import me.nullicorn.nedit.NBTOutputStream;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;

/**
 * Helper methods for processing NBT data.
 *
 * @author Nullicorn
 */
public final class NbtHelper {

  /**
   * Copies the contents of a {@code compound} into a new compound that cannot be modified,
   * disregarding reflection or similar methods.
   *
   * @throws IllegalArgumentException if the provided {@code compound} is {@code null}.
   */
  public static NBTCompound copyToImmutable(NBTCompound compound) {
    return new ImmutableCompound(compound);
  }

  /**
   * Serializes the contents of an NBT compound.
   *
   * @return a string of bytes, representing an NBT-encoded compound. This includes the type and
   * name of each child, finally terminated by a {@code TAG_End}.
   * @throws IOException              if the compound could not be serialized.
   * @throws IllegalArgumentException if the {@code compound} is {@code null}.
   */
  public static ByteString encodeToBytes(NBTCompound compound) throws IOException {
    if (compound == null) {
      throw new IllegalArgumentException("null compound cannot be converted to proto");
    } else if (compound.isEmpty()) {
      // Return zero bytes if the compound is empty.
      return ByteString.EMPTY;
    }

    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();

    // Serialize the contents of the compound.
    new NBTOutputStream(bytesOut, false).writeCompound(compound);
    return ByteString.copyFrom(bytesOut.toByteArray());
  }

  /**
   * Builds a compound object from an NBT-encoded string of bytes.
   *
   * @param proto a string of bytes representing an NBT-encoded compound. This includes each tag's
   *              type, name, and value. As specified by the NBT format, the last byte must be a
   *              {@code TAG_End}, even if there are no other tags.
   * @return the compound represented by the bytes.
   * @throws IOException              if the byte string was not an NBT-encoded compound.
   * @throws IllegalArgumentException if the {@code proto} is {@code null}.
   */
  public static NBTCompound decodeFromBytes(ByteString proto) throws IOException {
    if (proto == null) {
      throw new IllegalArgumentException("null proto cannot be converted to compound");
    } else if (proto.isEmpty()) {
      // Return an empty compound if no bytes were supplied.
      return new NBTCompound();
    }

    // Open a new stream for the bytes. String interning is enabled for both keys and values.
    try (NBTInputStream in = new NBTInputStream(proto.newInput(), true, true)) {
      return in.readCompound();
    }
  }

  /**
   * Recursively copies the contents of an NBT tag. Used internally to copy tags into {@link
   * ImmutableCompound immutable compounds}.
   *
   * @param nbt The tag to copy.
   * @param <T> The runtime class of the NBT type.
   * @throws IllegalArgumentException if the {@code nbt} value is {@code null}, or if it's class has
   *                                  no corresponding NBT type.
   */
  // Suppressed so we can copy differently based on class, including arrays with an unknown type.
  @SuppressWarnings({"SuspiciousSystemArraycopy", "unchecked"})
  static <T> T deepCopy(T nbt) {
    if (nbt == null) {
      throw new IllegalArgumentException("null tag cannot be copied");
    }

    Object copy;

    if (nbt instanceof NBTCompound) {
      NBTCompound compound = (NBTCompound) nbt;
      NBTCompound compoundCopy = new NBTCompound();

      // Perform a deep copy on each value, then add
      // them to the new compound using the same name.
      compound.forEach((name, value) -> compoundCopy.put(name, deepCopy(value)));
      copy = compoundCopy;

    } else if (nbt instanceof NBTList) {
      NBTList list = (NBTList) nbt;
      NBTList listCopy = new NBTList(list.getContentType());

      // Perform a deep copy on each element, then
      // add them to the new list at the same index.
      list.forEach(element -> listCopy.add(deepCopy(element)));
      copy = listCopy;

    } else if (nbt instanceof byte[] || nbt instanceof int[] || nbt instanceof long[]) {
      Class<?> arrayType = nbt.getClass();
      int arrayLength = Array.getLength(nbt);

      // Copy the array without knowing its type.
      copy = Array.newInstance(arrayType.getComponentType(), arrayLength);
      System.arraycopy(nbt, 0, copy, 0, arrayLength);

    } else if (nbt instanceof String || nbt instanceof Byte || nbt instanceof Short
               || nbt instanceof Integer || nbt instanceof Long || nbt instanceof Float
               || nbt instanceof Double) {
      // Already immutable, no need to copy.
      copy = nbt;

    } else {
      throw new IllegalArgumentException("Unable to copy NBT value: " + nbt);
    }

    return (T) copy;
  }

  private NbtHelper() {
    throw new UnsupportedOperationException(getClass() + " should not be instantiated");
  }
}
