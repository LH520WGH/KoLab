package app.kolab.manager;
import android.os.ParcelFileDescriptor;
interface IKoLabBridge {
 String getCapabilities();
 String inspectModule(in ParcelFileDescriptor moduleFd, String displayName);
}
