package org.esa.snap.rcp.imgfilter;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.FilterSet;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.ModalDialog;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * The dialog that lets users select existing or define new image filters.
 *
 * @author Norman
 */
// JAN2019 - Daniel Knowles - Added link to new Straylight filters

public class CreateFilteredBandDialog extends ModalDialog implements FilterSetForm.Listener {

    public static final String TITLE = "Create Filtered Band"; /*I18N*/
    private final Product product;
    private final FilterSetsForm filterSetsForm;
    private final FilterSetFileStore filterSetStore;
    private List<FilterSet> userFilterSets;

    public CreateFilteredBandDialog(Product product, String sourceBandName, String helpId) {
        super(SnapApp.getDefault().getMainFrame(),
              TITLE,
              ModalDialog.ID_OK_CANCEL_HELP,
              helpId);
        this.product = product;

        FilterSet systemFilterSet = new FilterSet("System", false);
        systemFilterSet.addFilter("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
        systemFilterSet.addFilter("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
        systemFilterSet.addFilter("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
        systemFilterSet.addFilter("Straylight", StandardFilters.STRAYLIGHT_FILTERS);
        systemFilterSet.addFilter("Sharpen", StandardFilters.SHARPENING_FILTERS);
        systemFilterSet.addFilter("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
        systemFilterSet.addFilter("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
        systemFilterSet.addFilter("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);

        filterSetStore = new FilterSetFileStore(getFiltersDir());
        try {
            userFilterSets = filterSetStore.loadFilterSetModels();
        } catch (IOException e) {
            userFilterSets = new ArrayList<>();
            Dialogs.showError(TITLE, "Failed to load filter sets:\n" + e.getMessage());
            SystemUtils.LOG.log(Level.WARNING, "Failed to load filter sets", e);
        }

        ArrayList<FilterSet> filterSets = new ArrayList<>();
        filterSets.add(systemFilterSet);
        if (userFilterSets.isEmpty()) {
            userFilterSets.add(new FilterSet("User", true));
        }
        filterSets.addAll(userFilterSets);

        filterSetsForm = new FilterSetsForm(sourceBandName,
                                            this,
                                            filterSetStore, new FilterWindow(getJDialog()),
                                            filterSets.toArray(new FilterSet[filterSets.size()]));

        setContent(filterSetsForm);
    }

    @Override
    protected void onOK() {
        super.onOK();
        for (FilterSet userFilterSet : userFilterSets) {
            userFilterSet.setEditable(true);
            try {
                filterSetStore.storeFilterSetModel(userFilterSet);
            } catch (IOException e) {
                Dialogs.showError(TITLE, "Failed to store filter sets:\n" + e.getMessage());
            }
        }
    }

    public DialogData getDialogData() {
        return new DialogData(filterSetsForm.getSelectedFilter(), filterSetsForm.getTargetBandName(), filterSetsForm.getIterationCount());
    }

    @Override
    protected boolean verifyUserInput() {
        String message = null;
        final String targetBandName = filterSetsForm.getTargetBandName();
        if (targetBandName.equals("")) {
            message = "Please enter a name for the new filtered band."; /*I18N*/
        } else if (!ProductNode.isValidNodeName(targetBandName)) {
            message = MessageFormat.format("The band name ''{0}'' appears not to be valid.\n" +
                                                   "Please choose a different band name.", targetBandName); /*I18N*/
        } else if (product.containsBand(targetBandName)) {
            message = MessageFormat.format("The selected product already contains a band named ''{0}''.\n" +
                                                   "Please choose a different band name.", targetBandName); /*I18N*/
        } else if (filterSetsForm.getSelectedFilter() == null) {
            message = "Please select an image filter.";    /*I18N*/
        }
        if (message != null) {
            Dialogs.showError(TITLE, message);
            return false;
        }
        return true;
    }

    @Override
    public void filterSelected(FilterSet filterSet, Filter filter) {
        //System.out.println("filterModelSelected: filterModel = " + filter);
    }

    @Override
    public void filterAdded(FilterSet filterSet, Filter filter) {
        //System.out.println("filterModelAdded: filterModel = " + filter);
    }

    @Override
    public void filterRemoved(FilterSet filterSet, Filter filter) {
        //System.out.println("filterModelRemoved: filterModel = " + filter);
    }

    @Override
    public void filterChanged(FilterSet filterSet, Filter filter, String propertyName) {
        //System.out.println("filterModelChanged: filterModel = " + filter + ", propertyName = \"" + propertyName + "\"");
    }


    private File getFiltersDir() {
        return new File(SystemUtils.getAuxDataPath().toFile(), "image_filters");
    }

    public static class DialogData {

        private final Filter filter;
        private final String bandName;
        private final int iterationCount;

        private DialogData(Filter filter, String bandName, int iterationCount) {
            this.filter = filter;
            this.bandName = bandName;
            this.iterationCount = iterationCount;
        }

        public String getBandName() {
            return bandName;
        }

        public Filter getFilter() {
            return filter;
        }

        public int getIterationCount() {
            return iterationCount;
        }
    }


}
