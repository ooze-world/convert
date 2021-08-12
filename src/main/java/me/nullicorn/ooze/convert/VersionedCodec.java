package me.nullicorn.ooze.convert;

import java.util.HashSet;
import java.util.Set;

/**
 * A codec intended to be compatible with a specific Minecraft world version (aka data-version).
 *
 * @author Nullicorn
 */
public abstract class VersionedCodec {

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
}
