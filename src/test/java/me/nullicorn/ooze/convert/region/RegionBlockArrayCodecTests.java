package me.nullicorn.ooze.convert.region;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.IntConsumer;
import java.util.stream.Stream;
import me.nullicorn.ooze.convert.VersionedCodecTests;
import me.nullicorn.ooze.convert.region.storage.RegionUIntArray;
import me.nullicorn.ooze.convert.region.storage.RegionUIntArrayTestHelper;
import me.nullicorn.ooze.level.PackedUIntArray;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * @author Nullicorn
 */
class RegionBlockArrayCodecTests extends VersionedCodecTests {

  // Data-version when the compact block-array was introduced.
  private static final int EARLIEST_VERSION = 1451;

  // Shared codec instance to test on.
  // Initialized in beforeAll().
  private static RegionBlockArrayCodec testCodec;

  @BeforeAll
  static void beforeAll() {
    testCodec = new RegionBlockArrayCodec(EARLIEST_VERSION);
  }

  @Override
  protected IntConsumer getVersionedConstructor() {
    return RegionBlockArrayCodec::new;
  }

  @Override
  protected int[] getAcceptableVersionRange() {
    return new int[]{EARLIEST_VERSION, Integer.MAX_VALUE};
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void encode_shouldOutputLengthMatchInput(int length, int magnitude) {
    PackedUIntArray expected = new PackedUIntArray(provider_arrayValues(length, magnitude));
    RegionUIntArray actual = testCodec.encode(expected);

    assertEquals(expected.size(), actual.length(), "Encoded array has wrong length");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void encode_shouldOutputMagnitudeMatchInput(int length, int magnitude) {
    PackedUIntArray expected = new PackedUIntArray(provider_arrayValues(length, magnitude));
    RegionUIntArray actual = testCodec.encode(expected);

    assertEquals(expected.magnitude(), actual.magnitude(), "Encoded array has wrong magnitude");
  }

  @ParameterizedTest
  @NullSource
  void encode_shouldThrowIfInputIsNull(PackedUIntArray input) {
    assertThrows(IllegalArgumentException.class, () -> testCodec.encode(input));
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void encode_shouldOutputHaveSameValuesAsInput(int length, int magnitude) {
    PackedUIntArray expected = new PackedUIntArray(provider_arrayValues(length, magnitude));
    RegionUIntArray actual = testCodec.encode(expected);

    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), actual.get(i), "disagreement at i=" + i);
    }
  }

  @ParameterizedTest
  @NullSource
  void decode_shouldThrowIfInputIsNull(RegionUIntArray arrayThatIsNull) {
    assertThrows(IllegalArgumentException.class, () -> testCodec.decode(arrayThatIsNull));
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void decode_shouldOutputHaveSameValuesAsInput(int length, int magnitude) {
    RegionUIntArray expected = RegionUIntArray.from(length, magnitude, EARLIEST_VERSION);
    PackedUIntArray actual = testCodec.decode(expected);

    for (int i = 0; i < expected.length(); i++) {
      assertEquals(expected.get(i), actual.get(i), "disagreement at i=" + i);
    }
  }

  /**
   * @see RegionUIntArrayTestHelper#provider_arrayValues(int, int)
   */
  static int[] provider_arrayValues(int length, int magnitude) {
    return RegionUIntArrayTestHelper.provider_arrayValues(length, magnitude);
  }

  /**
   * @see RegionUIntArrayTestHelper#provider_lengthsAndMagnitudes()
   */
  static Stream<Arguments> provider_lengthsAndMagnitudes() {
    return RegionUIntArrayTestHelper.provider_lengthsAndMagnitudes();
  }
}
