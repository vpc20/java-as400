import com.ibm.as400.access.*;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.Record;
import java.math.BigDecimal;

public class DataQueueReceiver {

    public static void main(String[] args) throws Exception {
// Define the layout once
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

        // Force connection immediately so you know it's connected
        system.connectService(AS400.DATAQUEUE);

        // 2. Reference the data queue
        DataQueue queue = new DataQueue(system, "/QSYS.LIB/VPCRZKH1.LIB/SAMPLEDQ.DTAQ");

        // 3. Read from the queue (wait up to 10 seconds)
        int waitSeconds = 10;  // -1 = wait forever, 0 = no wait
        DataQueueEntry entry = queue.read(waitSeconds);

        if (entry != null) {
            // 4. Decode the data (EBCDIC → Java String)
            byte[] rawData = entry.getData();
            String message = new String(rawData, "IBM037").trim();
            System.out.println("Received: " + message);
            // Map bytes to fields automatically
            Record rec = format.getNewRecord(rawData);
            // Access converted Java types directly
            String msgText = (String) rec.getField("MSG_TEXT");
            BigDecimal zonedPos = (BigDecimal) rec.getField("ZONED_POS");
            BigDecimal zonedNeg = (BigDecimal) rec.getField("ZONED_NEG");
            BigDecimal packedPos = (BigDecimal) rec.getField("PACKED_POS");
            BigDecimal packedNeg = (BigDecimal) rec.getField("PACKED_NEG");
            int binaryPos        = (Integer) rec.getField("BINARY_POS");
            int binaryNeg        = (Integer) rec.getField("BINARY_NEG");

            System.out.println(msgText);
            System.out.println(zonedPos);
            System.out.println(zonedNeg);
            System.out.println(packedPos);
            System.out.println(packedNeg);
            System.out.println(binaryPos);
            System.out.println(binaryNeg);
        } else {
            System.out.println("No data in queue.");
        }

        // 5. Disconnect
        system.disconnectAllServices();
    }
}