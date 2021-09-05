package me.nullicorn.ooze.convert.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.TagType;
import me.nullicorn.ooze.convert.VersionedCodecTests;
import me.nullicorn.ooze.level.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * @author Nullicorn
 */
class RegionBlockStateCodecTests extends VersionedCodecTests {

  // Data-version when the block-state compound was introduced.
  private static final int EARLIEST_VERSION = 1451;

  // Info about a block state's "Name" tag.
  private static final String  NAME_TAG_NAME = "Name";
  private static final TagType NAME_TAG_TYPE = TagType.STRING;

  // Info about a block state's "Properties" tag.
  private static final String  PROPERTIES_TAG_NAME = "Properties";
  private static final TagType PROPERTIES_TAG_TYPE = TagType.COMPOUND;

  private static RegionBlockStateCodec testCodec;

  @Override
  protected IntConsumer getVersionedConstructor() {
    return RegionBlockStateCodec::new;
  }

  @Override
  protected int[] getAcceptableVersionRange() {
    return new int[]{EARLIEST_VERSION, Integer.MAX_VALUE};
  }

  @BeforeAll
  static void setUp() {
    testCodec = new RegionBlockStateCodec(EARLIEST_VERSION);
  }

  @ParameterizedTest
  @NullSource
  void encode_shouldRejectNullStates(BlockState state) {
    assertThrows(IllegalArgumentException.class, () -> testCodec.encode(state));
  }

  @ParameterizedTest
  @MethodSource("provideBlockStates")
  void encode_shouldOutputHaveName(BlockState state) {
    NBTCompound encoded = testCodec.encode(state);

    assertTrue(encoded.containsTag(NAME_TAG_NAME, NAME_TAG_TYPE));
    assertEquals(state.getName(), encoded.get(NAME_TAG_NAME));
  }

  @ParameterizedTest
  @MethodSource("provideBlockStates")
  void encode_shouldOutputHavePropertiesWhenInputDoesToo(BlockState state) {
    NBTCompound encoded = testCodec.encode(state);

    boolean expectedToHaveProperties = state.hasProperties();
    boolean actuallyHasProperties = encoded.containsTag(PROPERTIES_TAG_NAME, PROPERTIES_TAG_TYPE);

    assertEquals(expectedToHaveProperties, actuallyHasProperties);
    if (expectedToHaveProperties) {
      assertEquals(state.getProperties(), encoded.get(PROPERTIES_TAG_NAME));
    }
  }

  @ParameterizedTest
  @NullSource
  void decode_shouldRejectNullCompounds(NBTCompound state) {
    assertThrows(IllegalArgumentException.class, () -> testCodec.decode(state));
  }

  @ParameterizedTest
  @MethodSource("provideInvalidEncodedBlockStates")
  void decode_shouldRejectCompoundsWithoutName(NBTCompound state) {
    assertThrows(IOException.class, () -> testCodec.decode(state));
  }

  @ParameterizedTest
  @MethodSource("provideEncodedBlockStates")
  void decode_shouldOutputHaveSameValuesAsInput(String name, NBTCompound properties, NBTCompound state) throws IOException {
    BlockState decoded = testCodec.decode(state);

    assertEquals(name, decoded.getName());
    assertEquals(properties, decoded.getProperties());
  }

  /**
   * Provides a single non-null block state for use in parameterized tests. The included states may
   * or may not have properties.
   */
  static Stream<Arguments> provideBlockStates() {
    return Stream.concat(
        // No properties.
        Stream.of(
            arguments(new BlockState("test_state_without_properties"))
        ),

        // With properties (possible empty ones).
        provideValidProperties()
            .map(properties -> new BlockState("test_state_with_properties", properties))
            .map(Arguments::of)
    );
  }

  /**
   * Provides NBT-encoded block states that should be accepted by the coded. The included states
   * will always have a "Name" and "Properties" tag, although there may not be any properties
   * present.
   */
  static Stream<Arguments> provideEncodedBlockStates() {
    String nameWithProperties = "test_state_with_properties";

    String nameWithoutProperties = "test_state_without_properties";
    Stream<Arguments> argsWithoutProperties = Stream.of(arguments(
        nameWithoutProperties,
        new NBTCompound(),
        new NBTCompound() {{
          put(NAME_TAG_NAME, nameWithoutProperties);
        }}
    ));

    return Stream.concat(
        // No properties provided ("Properties" tag isn't there at all).
        argsWithoutProperties,

        // With properties provided.
        provideValidProperties()
            .map(properties -> {
              NBTCompound encoded = new NBTCompound() {{
                put(NAME_TAG_NAME, nameWithProperties);
                put(PROPERTIES_TAG_NAME, properties);
              }};
              return arguments(nameWithProperties, properties, encoded);
            })
    );
  }

  /**
   * Provides NBT-encoded block states that are non-null, but have no "Name" tag, making them
   * invalid.
   */
  static Stream<Arguments> provideInvalidEncodedBlockStates() {
    return Stream.concat(
        // With no valid tags.
        Stream.of(
            // Completely empty.
            arguments(new NBTCompound()),

            // With random invalid tag.
            arguments(new NBTCompound() {{
              put("i_dont_exist", "its true!");
            }})
        ),

        // With valid properties, but no name tag.
        provideValidProperties()
            .map(properties -> new NBTCompound() {{
              put(PROPERTIES_TAG_NAME, properties);
            }})
            .map(Arguments::of)
    );
  }

  /**
   * Provides a block state's "Properties" tag for use in parameterized tests. The included
   * compounds may be empty, but are never null.
   */
  static Stream<NBTCompound> provideValidProperties() {
    return Stream.of(
        // Empty.
        new NBTCompound(),

        // One property.
        new NBTCompound() {{
          put("test_property", "5");
        }},

        // Two properties.
        new NBTCompound() {{
          put("test_property", "5");
          put("test_another_property", "some_value");
        }}
    );
  }
}
