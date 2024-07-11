package eu.esa.snap.rcp.bandgroup;

import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;

import java.io.IOException;

public class BandGroupsManagerController {

    private final BandGroupsManager bandGroupsManager;

    public BandGroupsManagerController() throws IOException {
        bandGroupsManager = BandGroupsManager.getInstance();
    }

    public void setSelectedProduct(Product product) {
        bandGroupsManager.addGroupsOfProduct(product);
    }

    public void deselectProduct() {
        bandGroupsManager.removeGroupsOfProduct();
    }

    public String[] getBandGroupNames() {
        final BandGroup[] bandGroups = bandGroupsManager.get();
        return getBandGroupNames(bandGroups);
    }

    static String[] getBandGroupNames(BandGroup[] bandGroups) {
        final String[] groupNames = new String[bandGroups.length];

        int i = 0;
        for (final BandGroup bandGroup : bandGroups) {
            String name = bandGroup.getName();
            if (StringUtils.isNullOrEmpty(name)) {
                final String[] strings = bandGroup.get(0);
                groupNames[i] = "<unnamed>(" + strings[0] + " ...)";
            } else {
                groupNames[i] = name;
            }
            ++i;
        }

        return groupNames;
    }
}
