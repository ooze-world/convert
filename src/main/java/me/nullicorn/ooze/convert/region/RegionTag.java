package me.nullicorn.ooze.convert.region;

import java.util.Optional;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import me.nullicorn.ooze.convert.VersionedTag;

/**
 * An assortment of NBT tags used by Minecraft for storing chunk information.
 *
 * @author Nullicorn
 */
enum RegionTag implements VersionedTag {

  /*
   * Tags within the root compounds of the `region` directory.
   */

  /**
   * The wrapper compound around a chunk's data.
   * <p><br>
   * Located at the root of a compound in an anvil world file.
   */
  CHUNK("Level", TagType.COMPOUND),

  /**
   * The version that a chunk was last saved in.
   * <p><br>
   * Located at the root of a compound in an anvil world file.
   */
  DATA_VERSION("DataVersion", TagType.INT, 100),

  /*
   * Tags within the "Level" compounds of the `region` directory.
   */

  /**
   * The chunk's location along the X axis, measured in 16-block units.
   * <p><br>
   * Located in the {@link #CHUNK chunk tag}.
   */
  CHUNK_POS_X("xPos", TagType.INT),

  /**
   * The chunk's location along the Z axis, measured in 16-block units.
   * <p><br>
   * Located in the {@link #CHUNK chunk tag}.
   */
  CHUNK_POS_Z("zPos", TagType.INT),

  /**
   * A list of 16-block tall cubes that store portions of the chunk's blocks.
   * <p><br>
   * Located in the {@link #CHUNK chunk tag}.
   */
  CHUNK_SECTIONS("Sections", TagType.LIST, TagType.COMPOUND),

  /**
   * A list compounds containing information about each mob/object inside the chunk's horizontal
   * boundaries.
   * <p><br>
   * Only valid within a world's {@code region} directory. As of data version {@code 2679} (1.17.x),
   * the tag is still supported, but {@link #ENTITY_STORAGE_LIST} and {@link #ENTITY_STORAGE_POS}
   * are preferred.
   */
  CHUNK_ENTITIES("Entities", TagType.LIST, TagType.COMPOUND),

  /**
   * A list of compounds containing information about each block in the chunk with metadata
   * (containers, moving pistons, etc).
   */
  CHUNK_BLOCK_ENTITIES("TileEntities", TagType.LIST, TagType.COMPOUND),

  /**
   * The chunk's generation state.
   * <p><br>
   * Data versions prior to {@code 1466} (1.13.x) should use {@link #IS_LIGHT_GENERATED} and {@link
   * #IS_TERRAIN_GENERATED} instead.
   */
  CHUNK_STATUS("Status", TagType.STRING, 1466),

  /**
   * Whether or not the game has calculated light levels for the chunk yet.
   * <p><br>
   * As of data version {@code 1466} (1.13.x), this tag is deprecated in favor of {@link
   * #CHUNK_STATUS}.
   */
  IS_LIGHT_GENERATED("LightPopulated", TagType.BYTE, 99, 1465),

  /**
   * Whether or not the game has calculated light levels for the chunk yet.
   * <p><br>
   * As of data version {@code 1466} (1.13.x), this tag is deprecated in favor of {@link
   * #CHUNK_STATUS}.
   */
  IS_TERRAIN_GENERATED("TerrainPopulated", TagType.BYTE, 99, 1465),

  /*
   * Tags within the compound elements of a chunk's "Sections".
   */

  /**
   * The section's vertical distance from {@code y=0}, measured in 16-block units.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_SECTIONS section list}.
   */
  SECTION_ALTITUDE("Y", TagType.INT),

  /**
   * A list of all block states that can be used in the section.
   * <p><br>
   * Each block state is a compound containing a {@link #BLOCK_NAME name}, and optionally {@link
   * #BLOCK_PROPERTIES extra properties}.
   * <p><br>
   * Data versions prior to {@code 1451} (1.13.x) should use {@link #BLOCKS_LEGACY}, {@link
   * #BLOCK_STATES_LEGACY}, and {@link #BLOCK_EXTENSIONS_LEGACY} instead.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_SECTIONS section list}.
   */
  PALETTE("Palette", TagType.LIST, TagType.COMPOUND, 1451),

  /**
   * A {@link RegionCompactArrayCodec compact array} of 4096 integers (e.g. multiple values in a
   * single longs; the number of longs in the array is always less).
   * <p><br>
   * Each compacted value in the array is an index pointing to a state in the section's {@link
   * #PALETTE palette}. The array's {@code magnitude} is the number of bits needed to hold the
   * palette's last index.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_SECTIONS section list}.
   */
  BLOCKS("BlockStates", TagType.LONG_ARRAY, 1451),

  /**
   * An array of 4096 block IDs for the section. Blocks appear in YZX order, meaning all blocks with
   * the same Z and Y positions will be adjacent in the array.
   * <p><br>
   * "Block ID" refers to the unique numeric identifiers assigned to blocks and items in older
   * versions of Minecraft. As of data version {@code 1451} (1.13.x), this tag is deprecated in
   * favor of {@link #BLOCKS} and {@link #PALETTE}.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_SECTIONS section list}.
   */
  BLOCKS_LEGACY("Blocks", TagType.BYTE_ARRAY, 99, 1450),

  /**
   * An array of 2048 octets, each containing two of the data/damage values for corresponding blocks
   * in the {@link #BLOCKS_LEGACY block array}.
   * <p><br>
   * Each octet contains two data/damage values. Given an index in the block array, the
   * corresponding data value can be found in the octet at {@code index / 2}. Even-number indices
   * use the octet's lowest four bits, and odd-number indices use the highest 4 bits.
   * <p><br>
   * As of data version {@code 1451} (1.13.x), this tag is deprecated in favor of {@link #BLOCKS}
   * and {@link #PALETTE}.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_SECTIONS section list}.
   */
  BLOCK_STATES_LEGACY("Data", TagType.BYTE_ARRAY, 99, 1450),

  /**
   * An optional array of 2048 octets, each containing a pair of 4-bit values that can be used to
   * extend the {@link #BLOCKS_LEGACY block array}.
   * <p><br>
   * Extension allows block IDs to use up to 12 bits, rather than the usual 8-bit cap imposed by the
   * block array. Extensions are applied by taking the corresponding 4 bits from this array,
   * left-shifting them 8 bits, and then adding the result to the corresponding block ID. The 4-bit
   * groups of this array are indexed exactly the same as the {@link #BLOCK_STATES_LEGACY data
   * array}.
   * <p><br>
   * As of data version {@code 1451} (1.13.x), this tag is deprecated in favor of {@link #BLOCKS}
   * and {@link #PALETTE}.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_SECTIONS section list}.
   */
  BLOCK_EXTENSIONS_LEGACY("Add", TagType.BYTE_ARRAY, 99, 1450),

  /*
   * Tags that make up entries of a section's "Palette".
   */

  /**
   * The block's main identifier, with or without a namespace.
   * <p><br>
   * e.g. {@code minecraft:stone}, {@code air}, {@code namespace:value}, etc.
   * <p><br>
   * Located in the compounds of a section's {@link #PALETTE palette}.
   */
  BLOCK_NAME("Name", TagType.STRING, 1451),

  /**
   * An optional compound that defines extra information about a block's state. This includes things
   * like orientation, power, level, etc.
   * <p><br>
   * Located in the compounds of a section's {@link #PALETTE palette}.
   */
  BLOCK_PROPERTIES("Properties", TagType.COMPOUND, 1451),

  /*
   * Tags relating to the positions of blocks-entities and entities.
   */

  /**
   * A list of 3 doubles indicating an entity's absolute X, Y, and Z positions in the world, in that
   * order.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_ENTITIES entity list}, or in an
   * entity-chunk's {@link #ENTITY_STORAGE_LIST entries} (data versions {@code 2679}+ only).
   */
  ENTITY_POS("Pos", TagType.LIST, TagType.DOUBLE),

  /**
   * An integer indicating a block-entity's absolute X coordinate in the world.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_BLOCK_ENTITIES block-entity list}.
   *
   * @see #BLOCK_ENTITY_POS_Y
   * @see #BLOCK_ENTITY_POS_Z
   */
  BLOCK_ENTITY_POS_X("x", TagType.INT),

  /**
   * An integer indicating a block-entity's absolute Y coordinate in the world.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_BLOCK_ENTITIES block-entity list}.
   *
   * @see #BLOCK_ENTITY_POS_X
   * @see #BLOCK_ENTITY_POS_Z
   */
  BLOCK_ENTITY_POS_Y("y", TagType.INT),

  /**
   * An integer indicating a block-entity's absolute Z coordinate in the world.
   * <p><br>
   * Located in the compounds of a chunk's {@link #CHUNK_BLOCK_ENTITIES block-entity list}.
   *
   * @see #BLOCK_ENTITY_POS_X
   * @see #BLOCK_ENTITY_POS_Y
   */
  BLOCK_ENTITY_POS_Z("z", TagType.INT),

  /*
   * Tags within the root compounds of the `entities` directory.
   */

  /**
   * A list compounds containing information about each mob/object inside the chunk's horizontal
   * boundaries.
   * <p><br>
   * Only valid within the {@code entities} directory of a world, which was added in data version
   * {@code 2679} (1.17.x). For older versions, see {@link #CHUNK_ENTITIES}.
   * <p><br>
   * Located at the root of a compound in an anvil world file.
   */
  ENTITY_STORAGE_LIST("Entities", TagType.LIST, TagType.COMPOUND, 2679),

  /**
   * A list of two integers, indicating the X and Y coordinates of the chunk respectively.
   * <p><br>
   * Only valid within the {@code entities} directory of a world, which was added in data version
   * {@code 2679} (1.17.x). For older versions, see {@link #CHUNK_ENTITIES}.
   * <p><br>
   * Located at the root of a compound in an anvil world file.
   */
  ENTITY_STORAGE_POS("Position", TagType.INT_ARRAY, 2679);

  private final String  tagName;
  private final TagType tagType;
  private final TagType listType;
  private final int     minVersion;
  private final int     maxVersion;

  RegionTag(String tagName, TagType tagType) {
    this(tagName, tagType, 99);
  }

  RegionTag(String tagName, TagType tagType, int since) {
    this(tagName, tagType, null, since, Integer.MAX_VALUE);
  }

  RegionTag(String tagName, TagType tagType, int since, int until) {
    this(tagName, tagType, null, since, until);
  }

  RegionTag(String tagName, TagType tagType, TagType listType) {
    this(tagName, tagType, listType, 99);
  }

  RegionTag(String tagName, TagType tagType, TagType listType, int since) {
    this(tagName, tagType, listType, since, Integer.MAX_VALUE);
  }

  RegionTag(String tagName, TagType tagType, TagType listType, int since, int until) {
    if (tagName == null) {
      throw new IllegalArgumentException("null is not a valid tagName");
    } else if (tagType == null || tagType == TagType.END) {
      throw new IllegalArgumentException("tagType is invalid: " + tagType);
    } else if (listType != null && tagType != TagType.LIST) {
      throw new IllegalArgumentException("listType is only valid for lists, not " + tagType);
    } else if (since > until) {
      throw new IllegalArgumentException("First version exceeds last: " + since + " > " + until);
    }

    this.tagName = tagName;
    this.tagType = tagType;
    this.listType = listType;
    this.minVersion = since;
    this.maxVersion = until;
  }

  @Override
  public String tagName() {
    return tagName;
  }

  @Override
  public TagType tagType() {
    return tagType;
  }

  @Override
  public boolean isSupported(int dataVersion) {
    return dataVersion >= minVersion && dataVersion <= maxVersion;
  }

  @Override
  public <T> Optional<T> valueIn(NBTCompound compound, Class<T> runtimeType) {
    if (compound == null) {
      throw new IllegalArgumentException("Cannot get value of tag " + this + " from null compound");
    }

    // Make sure the runtime type matches that of the
    // NBT type.
    Class<?> expectedClass = tagType.getRuntimeType();
    if (runtimeType != expectedClass) {
      throw new IllegalArgumentException("Runtime type should be " + expectedClass +
                                         ", not " + runtimeType);
    }

    // Get the value and make sure it is the correct
    // type.
    Object value = compound.get(tagName());
    if (value == null || !tagType.getRuntimeType().isAssignableFrom(value.getClass())) {
      return Optional.empty();
    }

    // Suppressed because class is checked above.
    // noinspection unchecked
    return (Optional<T>) Optional.of(value);
  }

  @Override
  public void setValueIn(NBTCompound compound, Object value) {
    if (compound == null) {
      throw new IllegalArgumentException("Cannot set tag " + this + " for null compound");
    } else if (value == null) {
      throw new IllegalArgumentException("null is not a valid value for tag " + this);
    } else if (tagType != TagType.fromObject(value)) {
      // The value cannot be converted to the correct NBT type.
      throw new IllegalArgumentException("Invalid value for tag " + this + ": " + value);
    }

    // If the tag is a list, make sure the content type is correct.
    if (value instanceof NBTList && listType != null) {
      NBTList list = (NBTList) value;
      TagType actualListType = list.getContentType();

      if (listType != actualListType) {
        // The value is a list, but it's element-type doesn't match the expected one.
        throw new IllegalArgumentException("Tag " + this + " expects a list of " + list +
                                           ", but got a list of " + actualListType +
                                           ": " + value);
      }
    }

    compound.put(tagName, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(tagName)
        .append('(')
        .append(tagType);

    if (listType != null) {
      sb.append(':').append(listType);
    }

    // Format: name(TYPE) or name(TYPE:ELEMENT_TYPE)
    return sb.append(')').toString();
  }
}
