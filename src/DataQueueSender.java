import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;

public class DataQueueSender {

    public static void main(String[] args) throws Exception {

        // 1. Connect to IBM i
        AS400 system = new AS400("PUB400.COM", "user", "password");

        // Force connection immediately so you know it's connected
        system.connectService(AS400.DATAQUEUE);

        // 2. Reference the data queue
        DataQueue queue = new DataQueue(system, "/QSYS.LIB/VPCRZKH1.LIB/SAMPLEDQ.DTAQ");

        // 3. Prepare the message/data to send
        String message = "Hello from Java!";
        byte[] data = message.getBytes("IBM037"); // EBCDIC encoding for IBM i

        // 4. Write data to the queue
        queue.write(data);

        System.out.println("Data sent: " + message);

        // 5. Disconnect
        system.disconnectAllServices();
    }
}