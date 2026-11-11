package app.kolab.manager;
import android.os.ParcelFileDescriptor;

/** Stable read-only protocol v1; no module loading operation is exposed. */
interface IKoLabBridge {
    String getCapabilities();
    String inspectModule(in ParcelFileDescriptor moduleFd, String displayName);
}
