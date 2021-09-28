package me.nullicorn.ooze.convert.region.legacy;

import java.io.IOException;
import java.util.Optional;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.TagType;
import me.nullicorn.ooze.convert.Codec;
import me.nullicorn.ooze.convert.MalformedInputException;
import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.convert.VersionedTag;
import me.nullicorn.ooze.convert.region.RegionSectionCodec;
import me.nullicorn.ooze.convert.region.storage.NibbleArray;
import me.nullicorn.ooze.level.BlockState;
import me.nullicorn.ooze.level.Cell;
import me.nullicorn.ooze.level.PackedUIntArray;
import me.nullicorn.ooze.level.Palette;

/**
 * Provides serialization to and from NBT chunk sections, specifically for versions of Minecraft
 * before the 1.13.x "flattening" update (data version 1451).
 * <p>
 * This codec is not compatible with newer versions of the game. For that, see {@link
 * RegionSectionCodec}, which supports both newer and older versions of the game.
 *
 * @author Nullicorn
 */
public class RegionLegacySectionCodec extends VersionedCodec<Cell, NBTCompound> {

  /**
   * The number of total blocks that a cell (aka chunk section) contains. Equal to {@code pow(16,
   * 3)}.
   */
  private static final int BLOCKS_PER_CELL = 4096;

  // Chunk section NBT tags (not static imports for readability).
  private static final VersionedTag BLOCK_TYPES          = LegacyTag.BLOCK_TYPES;
  private static final VersionedTag BLOCK_TYPES_EXTENDED = LegacyTag.BLOCK_TYPES_EXTENDED;
  private static final VersionedTag BLOCK_VARIANTS       = LegacyTag.BLOCK_VARIANTS;

  private final Codec<BlockState, NumericBlockState> stateCodec;

  /**
   * Creates a codec compatible with a specific Minecraft {@code dataVersion}.
   *
   * @throws IllegalArgumentException if the supplied {@code dataVersion} is not between {@code 99}
   *                                  and {@code 1450} inclusive.
   */
  public RegionLegacySectionCodec(int dataVersion) {
    super(dataVersion, BLOCK_TYPES, BLOCK_TYPES_EXTENDED, BLOCK_VARIANTS);
    this.stateCodec = new NumericBlockStateCodec(dataVersion);
  }

  // TODO: 9/28/21 Document encoding process.

  @Override
  public NBTCompound encode(Cell cell) throws IOException {
    // Pull out the section's palette & blocks so we don't repeatedly get them.
    Palette srcPalette = cell.getPalette();
    PackedUIntArray srcBlocks = cell.getBlocks();

    // Create the tags that will end up being returned in the NBT compound. Extensions and Variants
    // are created lazily (as-needed).
    byte[] types = new byte[srcBlocks.size()];
    NibbleArray extensions = null;
    NibbleArray variants = null;

    // Acts as a mirror of the srcPalette, but using NumericBlockStates instead of standard
    // BlockStates. States are only added to this array as-needed, so that they can be re-used by
    // subsequent blocks with the same state.
    NumericBlockState[] legacyPalette = new NumericBlockState[srcPalette.size()];

    for (int i = 0; i < types.length; i++) {
      NumericBlockState state;
      int paletteIndex = srcBlocks.get(i);

      if (paletteIndex < 0 || paletteIndex >= srcPalette.size()) {
        throw new IOException("Palette index is out of bound (i=" + i + "): " + paletteIndex);
      }

      // If we've already converted the state to legacy, use that version.
      state = legacyPalette[paletteIndex];
      if (state == null) {
        // Otherwise do the conversion now, then store it in `legacyPalette` for later.
        state = stateCodec.encode(srcPalette.get(paletteIndex));
        legacyPalette[paletteIndex] = state;
      }

      // Convert the block's XZY index (ooze) -> YZX index (region).
      int encodedIndex = encodeBlockIndex(i);

      // Write the lower 8 bits of the block's type.
      types[encodedIndex] = (byte) (state.getType() & 0xff);

      // Write the upper 4 bits of the block's type (if necessary).
      if (state.isTypeExtended()) {
        if (extensions == null) {
          extensions = new NibbleArray(4096);
        }
        extensions.set(encodedIndex, state.getTypeExtension());
      }

      // Write the block's variant value (if necessary).
      if (state.isVariant()) {
        if (variants == null) {
          variants = new NibbleArray(4096);
        }
        variants.set(encodedIndex, state.getVariant());
      }
    }

    // Assemble & return the section as an NBT compound.
    NBTCompound section = new NBTCompound();
    setTagValue(BLOCK_TYPES, types, section);
    if (extensions != null) {
      setTagValue(BLOCK_TYPES_EXTENDED, extensions.toByteArray(), section);
    }
    if (variants != null) {
      setTagValue(BLOCK_TYPES_EXTENDED, variants.toByteArray(), section);
    }
    return section;
  }

  @Override
  public Cell decode(NBTCompound section) throws IOException {
    // Get the original section's tags.
    byte[] types = getTagValue(BLOCK_TYPES, section).map(byte[].class::cast).orElse(null);
    Optional<NibbleArray> extensions = getNibbles(BLOCK_TYPES_EXTENDED, section);
    Optional<NibbleArray> variants = getNibbles(BLOCK_VARIANTS, section);

    if (types == null) {
      // Make sure there aren't ONLY extensions or variants.
      if (extensions.isPresent()) {
        throw new MalformedInputException("chunk section", "solitary extension array");
      } else if (variants.isPresent()) {
        throw new MalformedInputException("chunk section", "solitary variants array");
      }
      // Short-circuit if the sections is empty (has no type array).
      return Cell.empty();

    } else if (types.length != BLOCKS_PER_CELL) {
      throw new MalformedInputException("block array", "length=" + types.length);
    }

    int[] cellBlocks = new int[BLOCKS_PER_CELL];
    PaletteBuilder cellPalette = new PaletteBuilder(dataVersion, stateCodec);

    for (int i = 0; i < types.length; i++) {
      int oldIndex = i; // Re-declared so it can be used in lambdas below.

      int type = types[oldIndex];
      int extension = extensions.map(array -> array.get(oldIndex)).orElse(0);
      int variant = variants.map(array -> array.get(oldIndex)).orElse(0);

      // Calculate the block's index and value in the new array.
      int newIndex = decodeBlockIndex(oldIndex);
      int newState = cellPalette.add(type, extension, variant);
      cellBlocks[newIndex] = newState;
    }

    return new Cell(cellPalette.build(), new PackedUIntArray(cellBlocks));
  }

  /**
   * Attempts to get the value of a NibbleArray from an NBT {@code tag} inside the supplied {@code
   * parent} compound.
   * <p><br>
   * Since NBT does not natively support nibble arrays, the {@code tag}'s type must be {@link
   * TagType#BYTE_ARRAY TAG_Byte_Array} so that the value can be passed to {@code NibbleArray's}
   * {@link NibbleArray#NibbleArray(int, byte...) constructor}. The {@code length} is always equal
   * to {@link #BLOCKS_PER_CELL}.
   */
  private Optional<NibbleArray> getNibbles(VersionedTag tag, NBTCompound parent) throws IOException {
    TagType type = tag.getType();
    if (type != TagType.BYTE) {
      throw new IllegalArgumentException("Cannot read nibble array as " + type);
    }

    byte[] words = getTagValue(tag, parent).map(byte[].class::cast).orElse(null);
    int expectedWordCount = (int) Math.ceil(BLOCKS_PER_CELL / 2d);

    if (words == null) {
      return Optional.empty();
    } else if (words.length != expectedWordCount) {
      throw new MalformedInputException("nibble array", "length=" + words.length);
    }

    return Optional.of(new NibbleArray(BLOCKS_PER_CELL, words));
  }

  /**
   * Maps the index of a block in an ooze file's block array to its corresponding index in a vanilla
   * (region/anvil) block array.
   * <p><br>
   * This is necessary because ooze uses XZY order for blocks, whereas region stores blocks in YZX
   * order.
   */
  private static int encodeBlockIndex(int oozeIndex) {
    // Ooze blocks are ordered XZY.
    int x = oozeIndex >>> 8 & 0xf;
    int z = oozeIndex >>> 4 & 0xf;
    int y = oozeIndex & 0xf;

    // Region blocks are ordered YZX
    return (y << 8) | (z << 4) | x;
  }

  /**
   * Performs the opposite conversion of {@link #encodeBlockIndex(int)}.
   */
  private static int decodeBlockIndex(int regionIndex) {
    int y = regionIndex >>> 8 & 0xf;
    int z = regionIndex >>> 4 & 0xf;
    int x = regionIndex & 0xf;

    return (x << 8) | (z << 4) | y;
  }
}
