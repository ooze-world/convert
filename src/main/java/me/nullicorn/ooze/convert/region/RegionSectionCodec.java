package me.nullicorn.ooze.convert.region;

import java.io.IOException;
import java.util.Optional;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.convert.VersionedTag;
import me.nullicorn.ooze.level.Cell;
import me.nullicorn.ooze.level.PackedUIntArray;
import me.nullicorn.ooze.level.Palette;

/**
 * @author Nullicorn
 */
public class RegionSectionCodec extends VersionedCodec {

  private static final VersionedTag ALTITUDE_TAG = RegionTag.SECTION_ALTITUDE;
  private static final VersionedTag PALETTE_TAG  = RegionTag.PALETTE;
  private static final VersionedTag BLOCKS_TAG   = RegionTag.BLOCKS;

  private final RegionCompactArrayCodec blockArrayCodec;
  private final RegionPaletteCodec      paletteCodec;
  private final boolean                 useModernCodec;

  public RegionSectionCodec(int dataVersion) {
    super(dataVersion, ALTITUDE_TAG);

    blockArrayCodec = new RegionCompactArrayCodec(dataVersion);
    paletteCodec = new RegionPaletteCodec(dataVersion);

    useModernCodec = PALETTE_TAG.isSupported(dataVersion);
  }

  public NBTCompound encode(Cell section) {
    NBTCompound encoded = new NBTCompound();

    if (useModernCodec) {
      // Make sure the section has its own independent palette,
      // just like in vanilla.
      section = section.isolatedCopy();

      Palette palette = section.getPalette();
      PackedUIntArray blocks = section.getBlocks();

      // Encode the palette & block array, then add them to
      // the output NBT,
      PALETTE_TAG.setValueIn(encoded, paletteCodec.encode(palette));
      BLOCKS_TAG.setValueIn(encoded, blockArrayCodec.encode(blocks));
    } else {
      // TODO: 8/16/21 Implement encode() for legacy sections.
      throw new UnsupportedOperationException("Legacy section encoding is not yet supported");
    }

    return encoded;
  }

  public Cell decode(NBTCompound section) throws IOException {
    Palette palette;
    PackedUIntArray blocks;

    if (useModernCodec) {
      // Read the NBT tags from the section.
      Optional<long[]> nbtBlocks = BLOCKS_TAG.valueIn(section, long[].class);
      Optional<NBTList> nbtPalette = PALETTE_TAG.valueIn(section, NBTList.class);

      if (!nbtBlocks.isPresent()) {
        // Sections might not have a block array in vanilla,
        // so we silently return here instead of throwing.
        return Cell.empty();

      } else if (!nbtPalette.isPresent()) {
        // Vanilla sections should never have blocks without
        // a palette, so we can throw here.
        throw new IOException("Populated section is missing a palette");
      }

      // Decode the block array & palette.
      palette = paletteCodec.decode(nbtPalette.get());
      blocks = blockArrayCodec.decode(nbtBlocks.get(), palette.magnitude());
    } else {
      // TODO: 8/16/21 Implement decode() for legacy sections.
      throw new UnsupportedOperationException("Legacy section decoding is not yet supported");
    }

    return new Cell(palette, blocks);
  }
}
