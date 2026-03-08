import com.ibm.as400.access.*;
import com.ibm.as400.access.AS400FileRecordDescription;
import java.io.IOException;

public class AS400RecordDesc {
    static void main() throws AS400SecurityException, AS400Exception, IOException, InterruptedException {
        AS400 system = new AS400("PUB400.COM", "VPCRZKH", "vpcrzkh41");
        AS400FileRecordDescription desc = new AS400FileRecordDescription(system, "/QSYS.LIB/VPCRZKH1.LIB/DATATYPES.FILE");
            desc.createRecordFormatSource("/tmp", "com.mycompany");
        // Writes /tmp/MYFILE.java — ready to compile and use
    }
}
