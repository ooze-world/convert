package me.nullicorn.ooze.convert;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;

/**
 * A codec intended to be compatible with a specific Minecraft world version (aka data-version).
 *
 * @author Nullicorn
 */
public abstract class VersionedCodec<T, E> implements Codec<T, E> {

  /**
   * @throws IllegalArgumentException if the {@code tags} array is {@code null}, or if any of its
   *                                  values are not {@link VersionedTag#isSupported(int) supported}
   *                                  in the {@code dataVersion} specified.
   */
  private static void ensureTagsAreAvailable(int dataVersion, VersionedTag[] tags) {
    if (tags == null) {
      throw new IllegalArgumentException("tags array cannot be null");
    }

    // Find any tags that aren't supported by the dataVersion.
    Set<VersionedTag> unavailableTags = new HashSet<>(tags.length);
    for (VersionedTag tag : tags) {
      if (!tag.isSupported(dataVersion)) {
        unavailableTags.add(tag);
      }
    }

    // If any of the tags are not available, throw an exception.
    // Otherwise, return quietly.
    if (!unavailableTags.isEmpty()) {
      // Convert the set to a comma-separated list (with brackets removed - [...]).
      String unavailableStr = unavailableTags.toString();
      unavailableStr = unavailableStr.substring(1, unavailableStr.length() - 1);

      throw new IllegalArgumentException("Unusable tags in version " + dataVersion + ": " +
                                         unavailableStr);
    }
  }

  /**
   * The codec's {@link #getCompatibility() compatible world version}.
   */
  protected final int dataVersion;

  /**
   * Creates a new codec intended for a specific world version.
   *
   * @param dataVersion  The version the codec should be compatible with. See {@link #dataVersion}.
   * @param requiredTags Any tags that the codec must be able to use when encoding & decoding.
   * @throws IllegalArgumentException if any of the {@code requiredTags} are {@link
   *                                  VersionedTag#isSupported(int) incompatible} with the {@code
   *                                  dataVersion}.
   */
  protected VersionedCodec(int dataVersion, VersionedTag... requiredTags) {
    this.dataVersion = dataVersion;

    if (requiredTags != null && requiredTags.length > 0) {
      ensureTagsAreAvailable(dataVersion, requiredTags);
    }
  }

  /**
   * @return the Minecraft world version that the codec is compatible with.
   */
  public final int getCompatibility() {
    return dataVersion;
  }

  /**
   * Gets the value of a certain NBT {@code tag} within a {@code parent} compound.
   *
   * @param tag    The tag to get the value of.
   * @param parent The compound to get the tag's value from.
   * @return the tag's value in the compound, or an empty optional if the value is missing, or if it
   * is the wrong type of tag.
   * @throws IllegalArgumentException if the {@code parent} compound is null.
   */
  protected <V> Optional<V> getTagValue(VersionedTag tag, NBTCompound parent) {
    if (tag == null) {
      throw new IllegalArgumentException("tag cannot be null");
    } else if (parent == null) {
      throw new IllegalArgumentException("parent cannot be null");
    }

    Object value = parent.get(tag.getName());
    if (value == null) {
      return Optional.empty();
    }

    TagType actualType = TagType.fromObject(value);
    TagType expectedType = tag.getType();
    if (actualType != expectedType) {
      return Optional.empty();
    }

    // Make sure the list's content-type matches if necessary.
    if (expectedType == TagType.LIST) {
      TagType expectedContentType = tag.getContentType();
      TagType actualContentType = ((NBTList) value).getContentType();

      // (END means any type is allowed)
      if (expectedContentType != TagType.END && actualContentType != expectedContentType) {
        return Optional.empty();
      }
    }

    // Suppressed because class is checked above.
    // noinspection unchecked
    return (Optional<V>) Optional.of(value);
  }

  /**
   * Assigns a {@code value} for an NBT {@code tag} within the supplied {@code parent} compound.
   *
   * @param parent The compound to set the tag's value in.
   * @param value  The value to set the tag to.
   * @throws ClassCastException            if the value's class cannot be cast to the tag's {@link
   *                                       TagType#getRuntimeType() runtime type}.
   * @throws IllegalArgumentException      if the tag, compound, or value are {@code null}.
   * @throws UnsupportedOperationException if the compound is immutable.
   */
  protected void setTagValue(VersionedTag tag, Object value, NBTCompound parent) {
    if (tag == null) {
      throw new IllegalArgumentException("tag cannot be null");
    } else if (value == null) {
      throw new IllegalArgumentException("value cannot be null");
    } else if (parent == null) {
      throw new IllegalArgumentException("parent cannot be null");
    }

    TagType actualType = TagType.fromObject(value);
    TagType expectedType = tag.getType();
    if (actualType != expectedType) {
      throw new ClassCastException("Invalid value for type " + expectedType + ": " + value);
    }

    // Make sure the list's content-type matches if necessary.
    if (expectedType == TagType.LIST) {
      TagType expectedContentType = tag.getContentType();
      TagType actualContentType = ((NBTList) value).getContentType();

      // (END means any type is allowed)
      if (expectedContentType != TagType.END && actualContentType != expectedContentType) {
        throw new ClassCastException("Invalid content type for " + tag + ": " + actualContentType);
      }
    }

    parent.put(tag.getName(), value);
  }
}
