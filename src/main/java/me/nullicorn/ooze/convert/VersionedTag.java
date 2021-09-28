package me.nullicorn.ooze.convert;

import me.nullicorn.nedit.type.NBTList;
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
  String getName();

  /**
   * @return the NBT data-type used by the tag.
   */
  TagType getType();

  /**
   * @return the expected {@link NBTList#getContentType() content-type} for lists using this tag, or
   * {@link TagType#END TAG_End} if any type is acceptable.
   * @throws UnsupportedOperationException If the tag's {@link #getType() type} is not {@link
   *                                       TagType#LIST TAG_List}.
   */
  TagType getContentType();

  /**
   * Checks the tag's compatibility with a given Minecraft world version (aka data-version).
   *
   * @return {@code true} if the tag is supported in the {@code dataVersion} specified. Otherwise
   * {@code false}.
   */
  boolean isSupported(int dataVersion);
}
