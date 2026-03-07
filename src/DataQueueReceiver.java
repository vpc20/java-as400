import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;

public class DataQueueReceiver {

    public static void main(String[] args) throws Exception {

        // 1. Connect to IBM i
        AS400 system = new AS400("PUB400.COM", "VPCRZKH", "vpcrzkh41");

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
        } else {
            System.out.println("No data in queue.");
        }

        // 5. Disconnect
        system.disconnectAllServices();
    }
}