package org.esa.snap.core.io;

/*
import java.util.Set;
import org.openide.modules.OnStart;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.lookup.Lookups;

@OnStart
public class Installer implements Runnable, LookupListener {
    @Override
    public void run() {
        Lookup lookup = Lookups.forPath("Snap/ProductReaders");
        Lookup.Result result = lookup.lookupResult(ProductReaderSpi.class);
        result.addLookupListener(this);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        Lookup.Result result = (Lookup.Result) ev.getSource();
        Set<Class<ProductReaderSpi>> types = result.allClasses();
        for (Class<ProductReaderSpi> type : types) {
            System.out.println(">>> ProductReaderSpi: " + type);
        }
    } 
}*/
public class Installer {

}
