import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CopybookParserTest {

    @Test
    void parseCopybook() {
        String cb1 = """
                       01  WS-DTAQ-MESSAGE.
                           05  WS-MSG-TEXT        PIC X(10).
                           05  WS-MSG-ZONED_POS   PIC S9(5)V99.
                           05  WS-MSG-ZONED_NEG   PIC S9(5)V99.
                           05  WS-MSG-PACKED_POS  PIC S9(5)V99   COMP-3.
                           05  WS-MSG-PACKED_NEG  PIC S9(5)V99   COMP-3.
                           05  WS-MSG-BINARY_POS  PIC S9(5)      COMP-4.
                           05  WS-MSG-BINARY_NEG  PIC S9(5)      COMP-4.
                """;
        List<String> myList = List.of(
                "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(10), \"TEXT\"));",
                "format.addFieldDescription(new ZonedDecimalFieldDescription(new AS400ZonedDecimal(7, 2), \"ZONED_POS\"));",
                "format.addFieldDescription(new ZonedDecimalFieldDescription(new AS400ZonedDecimal(7, 2), \"ZONED_NEG\"));",
                "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(7, 2), \"PACKED_POS\"));",
                "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(7, 2), \"PACKED_NEG\"));",
                "format.addFieldDescription(new BinaryFieldDescription(new AS400Bin4(), \"BINARY_POS\"));",
                "format.addFieldDescription(new BinaryFieldDescription(new AS400Bin4(), \"BINARY_NEG\"));"
                );
        assertEquals(myList, CopybookParser.parseCopybook(cb1));
    }
}