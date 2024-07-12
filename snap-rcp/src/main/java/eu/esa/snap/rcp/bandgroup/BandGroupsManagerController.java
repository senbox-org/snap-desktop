package eu.esa.snap.rcp.bandgroup;

import eu.esa.snap.core.datamodel.group.BandGroup;
import eu.esa.snap.core.datamodel.group.BandGroupImpl;
import eu.esa.snap.core.datamodel.group.BandGroupsManager;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;

import java.io.IOException;

public class BandGroupsManagerController {

    private final BandGroupsManager bandGroupsManager;

    public BandGroupsManagerController() throws IOException {
        bandGroupsManager = BandGroupsManager.getInstance();
    }

    // only for testing tb 2024-07-12
    BandGroupsManagerController(BandGroupsManager bandGroupsManager) {
        this.bandGroupsManager = bandGroupsManager;
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

    BandGroupImpl getGroup(String groupName) {
        final BandGroup[] bandGroups = bandGroupsManager.get();
        for (final BandGroup bandGroup : bandGroups) {
            if (bandGroup.getName().equals(groupName)) {
                return (BandGroupImpl) bandGroup;
            }
        }

        return null;
    }

    void storeGroup(String groupName, String[] bandNames) {
        if (groupExists(groupName)) {
            bandGroupsManager.remove(groupName);
        }

        final BandGroupImpl bandGroup = new BandGroupImpl(groupName, bandNames);
        bandGroupsManager.add(bandGroup);
    }

    public boolean groupExists(String groupName) {
        final BandGroup[] bandGroups = bandGroupsManager.get();
        for (final BandGroup bandGroup : bandGroups) {
            if (bandGroup.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    public void saveBandGroups() throws IOException {
        bandGroupsManager.save();
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
