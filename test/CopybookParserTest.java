import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopybookParserTest {

    @Test
    void parseCopybook() {
        System.out.println("Test 1");
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

        System.out.println("Test 2");
        cb1 = """
                       01  WS-ORDER.
                           05  WS-ORDER-ID        PIC 9(8).
                           05  WS-LINE-AMT        PIC S9(7)V99 COMP-3
                                                  OCCURS 5 TIMES.
                           05  WS-LINE-CODE       PIC X(3)
                                                  OCCURS 5 TIMES.
                """;
        myList = List.of(
                "format.addFieldDescription(new ZonedDecimalFieldDescription(new AS400ZonedDecimal(8, 0), \"ORDER_ID\"));",
                "// LINE_AMT  OCCURS 5 (effective dimensions: [5])",
                "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"LINE_AMT_1\"));",
                "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"LINE_AMT_2\"));",
                "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"LINE_AMT_3\"));",
                "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"LINE_AMT_4\"));",
                "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"LINE_AMT_5\"));",
                "// LINE_CODE  OCCURS 5 (effective dimensions: [5])",
                "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(3), \"LINE_CODE_1\"));",
                "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(3), \"LINE_CODE_2\"));",
                "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(3), \"LINE_CODE_3\"));",
                "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(3), \"LINE_CODE_4\"));",
                "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(3), \"LINE_CODE_5\"));"
        );
        assertEquals(myList, CopybookParser.parseCopybook(cb1));

        System.out.println("Test 3");
        cb1 = """
                       01  WS-INVOICE.
                           05  WS-INV-NUMBER      PIC 9(8).
                           05  WS-INV-LINE        OCCURS 3 TIMES.
                               10  WS-ITEM-CODE   PIC X(5).
                               10  WS-ITEM-QTY    PIC S9(5)      COMP-4.
                               10  WS-ITEM-PRICE  PIC S9(7)V99   COMP-3.
                           05  WS-INV-TOTAL       PIC S9(9)V99   COMP-3.
                """;
        myList = List.of(
        "format.addFieldDescription(new ZonedDecimalFieldDescription(new AS400ZonedDecimal(8, 0), \"INV_NUMBER\"));",
        "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(5), \"ITEM_CODE_1\"));",
        "format.addFieldDescription(new BinaryFieldDescription(new AS400Bin4(), \"ITEM_QTY_1\"));",
        "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"ITEM_PRICE_1\"));",
        "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(5), \"ITEM_CODE_2\"));",
        "format.addFieldDescription(new BinaryFieldDescription(new AS400Bin4(), \"ITEM_QTY_2\"));",
        "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"ITEM_PRICE_2\"));",
        "format.addFieldDescription(new CharacterFieldDescription(new AS400Text(5), \"ITEM_CODE_3\"));",
        "format.addFieldDescription(new BinaryFieldDescription(new AS400Bin4(), \"ITEM_QTY_3\"));",
        "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(9, 2), \"ITEM_PRICE_3\"));",
        "format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(11, 2), \"INV_TOTAL\"));"
        );
        assertEquals(myList, CopybookParser.parseCopybook(cb1));

    }
}