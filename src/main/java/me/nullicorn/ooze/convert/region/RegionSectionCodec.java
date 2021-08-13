package me.nullicorn.ooze.convert.region;

import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.level.Cell;

/**
 * @author Nullicorn
 */
public class RegionSectionCodec extends VersionedCodec {

  public RegionSectionCodec(int dataVersion) {
    super(dataVersion, RegionTag.SECTION_ALTITUDE);
  }

  public NBTCompound encode(Cell section) {
    // TODO: 8/13/21 Implement encode() for sections.
    throw new UnsupportedOperationException("Section encoding is not yet supported");
  }

  public Cell decode(NBTCompound section) {
    // TODO: 8/13/21 Implement decode() for sections.
    throw new UnsupportedOperationException("Section decoding is not yet supported");
  }
}
