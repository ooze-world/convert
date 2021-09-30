package me.nullicorn.ooze.convert.region;

import java.io.IOException;
import java.util.Optional;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.ooze.convert.MalformedInputException;
import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.convert.VersionedTag;
import me.nullicorn.ooze.convert.region.legacy.RegionLegacySectionCodec;
import me.nullicorn.ooze.convert.region.storage.RegionUIntArray;
import me.nullicorn.ooze.level.Cell;
import me.nullicorn.ooze.level.PackedUIntArray;
import me.nullicorn.ooze.level.Palette;

/**
 * Provides serialization to and from NBT chunk sections.
 *
 * @author Nullicorn
 */
public class RegionSectionCodec extends VersionedCodec<Cell, NBTCompound> {

  /**
   * The number of total blocks that a cell (aka chunk section) contains. Equal to {@code pow(16,
   * 3)}.
   */
  private static final int BLOCKS_PER_CELL = 4096;

  // Chunk section NBT tags (not static imports for readability).
  private static final VersionedTag BLOCKS_TAG   = RegionTag.BLOCKS;
  private static final VersionedTag PALETTE_TAG  = RegionTag.PALETTE;
  private static final VersionedTag ALTITUDE_TAG = RegionTag.SECTION_ALTITUDE;

  // Sub-codecs.
  private final RegionPaletteCodec    paletteCodec;
  private final RegionBlockArrayCodec blockArrayCodec;

  // Fall-back codec for older versions.
  private final boolean                  doUseLegacyCodec;
  private final RegionLegacySectionCodec legacySectionCodec;

  public RegionSectionCodec(int dataVersion) {
    super(dataVersion, ALTITUDE_TAG);

    boolean doUseModernCodec = (PALETTE_TAG.isSupported(dataVersion));

    if (doUseModernCodec) {
      paletteCodec = new RegionPaletteCodec(dataVersion);
      blockArrayCodec = new RegionBlockArrayCodec(dataVersion);
      legacySectionCodec = null;
    } else {
      paletteCodec = null;
      blockArrayCodec = null;
      legacySectionCodec = new RegionLegacySectionCodec(dataVersion);
    }
    doUseLegacyCodec = !doUseModernCodec;
  }

  @Override
  public NBTCompound encode(Cell cell) throws IOException {
    if (doUseLegacyCodec) {
      return legacySectionCodec.encode(cell);
    } else if (cell == null) {
      throw new IllegalArgumentException("null cannot be encoded as a chunk section");
    }
    NBTCompound section = new NBTCompound();

    // Make sure the section has its own independent palette, just like in vanilla.
    cell = cell.isolatedCopy();

    Palette palette = cell.getPalette();
    PackedUIntArray blocks = cell.getBlocks();

    // Encode the palette & block array, then add them to the output NBT,
    setTagValue(PALETTE_TAG, paletteCodec.encode(palette), section);
    setTagValue(BLOCKS_TAG, blockArrayCodec.encode(blocks), section);

    return section;
  }

  @Override
  public Cell decode(NBTCompound section) throws IOException {
    if (doUseLegacyCodec) {
      return legacySectionCodec.decode(section);
    } else if (section == null) {
      throw new IllegalArgumentException("null cannot be decoded as a cell");
    }

    // Read the NBT tags from the section.
    Optional<long[]> nbtBlocks = getTagValue(BLOCKS_TAG, section);
    Optional<NBTList> nbtPalette = getTagValue(PALETTE_TAG, section);

    if (!nbtBlocks.isPresent() && !nbtPalette.isPresent()) {
      // It's normal for there to be no blocks and palette. It just means the section is empty.
      return Cell.empty();

    } else if (!nbtBlocks.isPresent()) {
      // Palette w/o blocks is probably bad.
      throw new MalformedInputException("chunk section", "has palette, but no blocks");

    } else if (!nbtPalette.isPresent()) {
      // Blocks w/o palette is definitely bad.
      throw new MalformedInputException("chunk section", "has blocks, but no palette");
    }

    // Decode the palette.
    Palette palette = paletteCodec.decode(nbtPalette.get());

    // Decode the block array.
    RegionUIntArray regionBlockArray = RegionUIntArray.from(
        BLOCKS_PER_CELL,
        palette.magnitude(),
        nbtBlocks.get(),
        dataVersion);
    PackedUIntArray blocks = blockArrayCodec.decode(regionBlockArray);

    return new Cell(palette, blocks);
  }
}
