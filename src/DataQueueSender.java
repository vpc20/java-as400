import com.ibm.as400.access.*;
import com.ibm.as400.access.Record;

import java.math.BigDecimal;

public class DataQueueSender {

    public static void main(String[] args) throws Exception {

        // Define the SAME layout as the receiver
        RecordFormat format = new RecordFormat();
        format.addFieldDescription(new CharacterFieldDescription(new AS400Text(10), "MSG_TEXT"));
        format.addFieldDescription(new ZonedDecimalFieldDescription(new AS400ZonedDecimal(7, 2), "ZONED_POS"));
        format.addFieldDescription(new ZonedDecimalFieldDescription(new AS400ZonedDecimal(7, 2), "ZONED_NEG"));
        format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(7, 2), "PACKED_POS"));
        format.addFieldDescription(new PackedDecimalFieldDescription(new AS400PackedDecimal(7, 2), "PACKED_NEG"));
        format.addFieldDescription(new BinaryFieldDescription(new AS400Bin4(), "BINARY_POS"));
        format.addFieldDescription(new BinaryFieldDescription(new AS400Bin4(), "BINARY_NEG"));

        // 1. Connect to IBM i
        AS400 system = new AS400("PUB400.COM", "user", "password");
        system.connectService(AS400.DATAQUEUE);
        System.out.println("Connected to IBM i.");

        // 2. Reference the data queue
        DataQueue queue = new DataQueue(system, "/QSYS.LIB/VPCRZKH1.LIB/SAMPLEDQ.DTAQ");

        // 3. Build a Record with test data
        Record rec = format.getNewRecord();

        // Text field  (10 chars – padded automatically by AS400Text)
        rec.setField("MSG_TEXT", "HELLO     ");

        // Zoned decimal  7 digits, 2 decimal places
        rec.setField("ZONED_POS", new BigDecimal("12345.67"));
        rec.setField("ZONED_NEG", new BigDecimal("-23456.78"));

        // Packed decimal  7 digits, 2 decimal places
        rec.setField("PACKED_POS", new BigDecimal("34567.89"));
        rec.setField("PACKED_NEG", new BigDecimal("-45678.90"));

        // 4-byte signed binary
        rec.setField("BINARY_POS", Integer.valueOf(123));
        rec.setField("BINARY_NEG", Integer.valueOf(-456));

        // 4. Serialize the record to EBCDIC bytes
        byte[] rawData = rec.getContents();

        // 5. Write to the data queue
        queue.write(rawData);
        System.out.println("Data written to queue successfully.");

        // Echo what was sent
        System.out.println("--- Sent values ---");
        System.out.println("MSG_TEXT  : " + rec.getField("MSG_TEXT"));
        System.out.println("ZONED_POS : " + rec.getField("ZONED_POS"));
        System.out.println("ZONED_NEG : " + rec.getField("ZONED_NEG"));
        System.out.println("PACKED_POS: " + rec.getField("PACKED_POS"));
        System.out.println("PACKED_NEG: " + rec.getField("PACKED_NEG"));
        System.out.println("BINARY_POS: " + rec.getField("BINARY_POS"));
        System.out.println("BINARY_NEG: " + rec.getField("BINARY_NEG"));

        // 6. Disconnect
        system.disconnectAllServices();
        System.out.println("Disconnected.");
    }
}