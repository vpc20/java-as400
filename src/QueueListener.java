import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;


public class QueueListener {

    public static void main(String[] args) throws Exception {

        AS400 system = new AS400("PUB400.COM", "MYUSER", "MYPASSWORD");
        // Force connection immediately so you know it's connected
        system.connectService(AS400.DATAQUEUE);
        DataQueue queue = new DataQueue(system, "/QSYS.LIB/VPCRZKH1.LIB/SAMPLEDQ.DTAQ");

        System.out.println("Listening for messages...");

        while (true) {
            // Block indefinitely until a message arrives
            DataQueueEntry entry = queue.read(-1);

            if (entry != null) {
                String msg = new String(entry.getData(), "IBM037").trim();
                System.out.println("Received: " + msg);
                processMessage(msg);
            }
        }
    }

    static void processMessage(String msg) {
        // Your business logic here
        System.out.println("Processing: " + msg);
    }
}