package me.nullicorn.ooze.convert.region;

import static me.nullicorn.ooze.convert.region.RegionBlockStateCodecTests.NAME_TAG_NAME;
import static me.nullicorn.ooze.convert.region.RegionBlockStateCodecTests.PROPERTIES_TAG_NAME;
import static me.nullicorn.ooze.convert.region.RegionBlockStateCodecTests.provideBlockStates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import me.nullicorn.ooze.convert.VersionedCodecTests;
import me.nullicorn.ooze.level.BlockState;
import me.nullicorn.ooze.level.Palette;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * @author Nullicorn
 */
class RegionPaletteCodecTests extends VersionedCodecTests {

  // Data-version when block palettes were introduced.
  private static final int EARLIEST_VERSION = 1451;

  private static RegionPaletteCodec testCodec;

  @BeforeAll
  static void beforeAll() {
    testCodec = new RegionPaletteCodec(EARLIEST_VERSION);
  }

  @Override
  protected IntConsumer getVersionedConstructor() {
    return RegionPaletteCodec::new;
  }

  @Override
  protected int[] getAcceptableVersionRange() {
    return new int[]{EARLIEST_VERSION, Integer.MAX_VALUE};
  }

  @ParameterizedTest
  @NullSource
  void encode_shouldRejectNullPalettes(Palette palette) {
    assertThrows(IllegalArgumentException.class, () -> testCodec.encode(palette));
  }

  @ParameterizedTest
  @MethodSource("providePalettes")
  void encode_shouldOutputUseCorrectTagType(Palette palette) {
    NBTList encoded = testCodec.encode(palette);
    assertEquals(TagType.COMPOUND, encoded.getContentType());
  }

  @ParameterizedTest
  @MethodSource("providePalettes")
  void encode_shouldOutputBeSameSize(Palette palette) {
    NBTList encoded = testCodec.encode(palette);
    assertEquals(palette.size(), encoded.size());
  }

  @ParameterizedTest
  @MethodSource("providePalettes")
  void encode_shouldOrderBePreserved(Palette palette) {
    NBTList encoded = testCodec.encode(palette);

    // Make sure each state matches its encoded counterpart.
    for (int i = 0; i < palette.size(); i++) {
      BlockState state = palette.get(i);
      NBTCompound stateEncoded = encoded.getCompound(i);

      // Properties will only be included in output if non-empty.
      NBTCompound expectedProperties = state.getProperties();
      if (expectedProperties.isEmpty()) {
        expectedProperties = null;
      }

      assertEquals(state.getName(), stateEncoded.get(NAME_TAG_NAME));
      assertEquals(expectedProperties, stateEncoded.get(PROPERTIES_TAG_NAME));
    }
  }

  /**
   * Provides valid block palettes for use in parameterized tests.
   */
  static Stream<Arguments> providePalettes() {
    // Borrow states from the block codec test.
    List<BlockState> states = provideBlockStates()
        .collect(Collectors.toList()).stream()
        .map(arguments -> (BlockState) arguments.get()[0])
        .collect(Collectors.toList());

    return Stream.of(
        arguments(new Palette("empty", EARLIEST_VERSION, Collections.emptyList())),
        arguments(new Palette("filled", EARLIEST_VERSION, states))
    );
  }
}
