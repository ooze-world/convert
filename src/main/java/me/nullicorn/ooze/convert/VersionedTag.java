package me.nullicorn.ooze.convert;

import java.util.Optional;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.TagType;

/**
 * An NBT tag with a known name, type, and version range.
 *
 * @author Nullicorn
 */
public interface VersionedTag {

  /**
   * @return the tag's expected name.
   */
  String tagName();

  /**
   * @return the NBT data-type used by the tag.
   */
  TagType tagType();

  /**
   * Checks the tag's compatibility with a given Minecraft world version (aka data-version).
   *
   * @return {@code true} if the tag is supported in the {@code dataVersion} specified. Otherwise
   * {@code false}.
   */
  boolean isSupported(int dataVersion);

  /**
   * Gets the value of the tag as a compound's direct child.
   *
   * @param compound    The compound to get the tag's value from.
   * @param runtimeType The tag's expected class. Must be the same as the NBT type's {@link
   *                    TagType#getRuntimeType() class}.
   * @param <T>         The same class as {@code runtimeType}.
   * @return the tag's value in the compound, wrapped in a nullable optional in case it doesn't have
   * a value or is the wrong type.
   * @throws IllegalArgumentException if the input compound is null, or if the {@code runtimeType}
   *                                  is not valid for the tag's {@link #tagType() type}. Also
   *                                  thrown if {@code runtimeType} does not match the NBT type's
   *                                  class.
   */
  <T> Optional<T> valueIn(NBTCompound compound, Class<T> runtimeType);

  /**
   * Assigns the tag a value {@code value} inside the given {@code compound}
   *
   * @param compound The compound to set the tag's value in.
   * @param value    The value to set the tag to.
   * @throws ClassCastException            if the value's class cannot be cast to the tag's {@link
   *                                       TagType#getRuntimeType() runtime type}.
   * @throws IllegalArgumentException      if the compound or value are {@code null}.
   * @throws UnsupportedOperationException if the compound is immutable.
   */
  void setValueIn(NBTCompound compound, Object value);
}
