import com.ibm.as400.access.AS400;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.KeyedDataQueueEntry;

public class KeyedDataQueueExample {

    public static void main(String[] args) throws Exception {

        AS400 system = new AS400("MY_IBMI_HOST", "MYUSER", "MYPASSWORD");
        // Force connection immediately so you know it's connected
        system.connectService(AS400.DATAQUEUE);
        KeyedDataQueue queue = new KeyedDataQueue(system, "/QSYS.LIB/MYLIB.LIB/MYKEYDTAQ.DTAQ");

        // --- SEND with key ---
        String key     = "ORDER001";
        String message = "Process order 001";

        queue.write(
                key.getBytes("IBM037"),
                message.getBytes("IBM037")
        );
        System.out.println("Sent with key: " + key);

        // --- RECEIVE by key ---
        KeyedDataQueueEntry entry = queue.read(
                key.getBytes("IBM037"),
                10,   // wait seconds
                "EQ"  // key order: EQ, NE, LT, LE, GT, GE
        );

        if (entry != null) {
            String received = new String(entry.getData(), "IBM037").trim();
            System.out.println("Received: " + received);
        }

        system.disconnectAllServices();
    }
}