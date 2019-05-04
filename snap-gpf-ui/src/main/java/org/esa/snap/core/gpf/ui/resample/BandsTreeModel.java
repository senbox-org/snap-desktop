package org.esa.snap.core.gpf.ui.resample;

import org.esa.snap.core.datamodel.Product;

import javax.swing.tree.TreeModel;
import java.util.ArrayList;

/**
 * Created by obarrile on 22/04/2019.
 */
public class BandsTreeModel implements TreeModel {
    private String[] productBandNames;
    ArrayList<String> listGroups = new ArrayList<>();
    ArrayList<String> bandsWithoutGroup = new ArrayList<>();
    private int totalRows;
    private String[] rows;


    public BandsTreeModel(Product product) {
        this.productBandNames = product.getBandNames().clone();
        if(product.getAutoGrouping()!= null) {
            for (int i = 0 ; i < product.getAutoGrouping().size() ; i++) {
                for(String bandName : product.getBandNames()) {
                    if (bandName.startsWith(product.getAutoGrouping().get(i)[0])) {
                        if(!listGroups.contains(product.getAutoGrouping().get(i)[0])) {
                            listGroups.add(product.getAutoGrouping().get(i)[0]);
                        }
                    }
                }
            }

            for(String bandName : product.getBandNames()) {
                boolean inGroup = false;
                for (int i = 0 ; i < listGroups.size() ; i++) {
                    if (bandName.startsWith(listGroups.get(i))) {
                        inGroup = true;
                        break;
                    }
                }
                if(!inGroup) {
                    bandsWithoutGroup.add(bandName);
                }
            }

        } else {
            for(String bandName : product.getBandNames()) {
                bandsWithoutGroup.add(bandName);
            }
        }

        totalRows = 1 + listGroups.size() + product.getNumBands();
        rows = new String[totalRows];
        rows[0]=new String ("Bands");
        for(int i = 0 ; i < listGroups.size() ; i++) {
            rows[1+i] = new String(listGroups.get(i));
        }
        for(int i = 0 ; i < product.getNumBands() ; i++) {
            rows[1+listGroups.size()+i] = new String(product.getBandAt(i).getName());
        }
    }

    public int getTotalRows() {
        return totalRows;
    }

    public String[] getRows() {
        return rows;
    }


    @Override
    public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        //do nothing
    }

    @Override
    public Object getChild(Object parent, int index) {
        String parentString = (String) parent;
        ArrayList<String> autogroupingBands = new ArrayList<>();
        if(parentString.equals("Bands")) {
            if (index < listGroups.size()) {
                return listGroups.get(index);
            } else {
                return bandsWithoutGroup.get(index-listGroups.size());
            }
        }

        for (int i = 0 ; i < listGroups.size() ; i++) {
            if(listGroups.get(i).equals(parentString)) {
                String[] bandNames = productBandNames;
                ArrayList<String> autoGroupingBands = new ArrayList<>();
                for(String bandName : bandNames) {
                    if(bandName.startsWith(parentString)) {
                        autoGroupingBands.add(bandName);
                    }
                }
                return autoGroupingBands.get(index);
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        String parentString = (String) parent;
        if(parentString.equals("Bands")) {
            if(listGroups.size() == 0) {
                return productBandNames.length;
            } else {
                return (bandsWithoutGroup.size() + listGroups.size());
            }
        }

        for (int i = 0 ; i < listGroups.size() ; i++) {
            if(listGroups.get(i).equals(parentString)) {
                String[] bandNames = productBandNames;
                ArrayList<String> autoGroupingBands = new ArrayList<>();
                for(String bandName : bandNames) {
                    if(bandName.startsWith(parentString)) {
                        autoGroupingBands.add(bandName);
                    }
                }
                return autoGroupingBands.size();
            }
        }

        return 0;
    }
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        String parentString = (String) parent;
        String childString = (String) child;
        if(parentString.equals("Bands")) {
            if(listGroups.size() == 0) {
                String[] bands = productBandNames;
                for(int i = 0 ; i < bands.length ; i++) {
                    if(bands[i].equals(childString)) {
                        return i;
                    }
                }
            } else {
                for (int i = 0 ; i < listGroups.size() ; i++) {
                    if(listGroups.get(i).equals(child)) {
                        return i;
                    }
                }
                String[] bands = productBandNames;
                for(int i = 0 ; i < bands.length ; i++) {
                    if(bands[i].equals(childString)) {
                        return (i+listGroups.size());
                    }
                }
            }
        }

        for (int i = 0 ; i < listGroups.size() ; i++) {
            if(listGroups.get(i).equals(parentString)) {
                String[] bandNames = productBandNames;
                ArrayList<String> autoGroupingBands = new ArrayList<>();
                for(String bandName : bandNames) {
                    if(bandName.startsWith(parentString)) {
                        autoGroupingBands.add(bandName);
                    }
                }
                return autoGroupingBands.indexOf(child);
            }
        }

        return -1;
    }
    @Override
    public Object getRoot() {
        return "Bands";
    }
    @Override
    public boolean isLeaf(Object node) {
        String stringNode = (String) node;
        String[] bandNames = productBandNames;
        for(String bandName : bandNames) {
            if(bandName.equals(stringNode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        //do nothing
    }
    @Override
    public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
        //do nothing
    }
}
