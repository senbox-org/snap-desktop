package org.esa.snap.rcp.actions.vector;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.crs.CrsSelectionPanel;
import org.esa.snap.ui.crs.CustomCrsForm;
import org.esa.snap.ui.crs.PredefinedCrsForm;
import org.esa.snap.ui.crs.ProductCrsForm;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Insets;

/**
 * Dialog for selection of a feature CRS in CSV import
 *
 * @author olafd
 */
public class FeatureCrsDialog extends ModalDialog {

    private CrsSelectionPanel crsSelectionPanel;
    private Product product;
    private String title;

    public FeatureCrsDialog(Product product, String title) {
        super(SnapApp.getDefault().getMainFrame(), title, ModalDialog.ID_OK_CANCEL_HELP, "importCSV");
        this.product = product;
        this.title = title;
        createUI();
    }

    private void createUI() {
        final ProductCrsForm productCrsForm = new ProductCrsForm(SnapApp.getDefault().getAppContext(), product);
        final CustomCrsForm customCrsForm = new CustomCrsForm(SnapApp.getDefault().getAppContext());
        final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(SnapApp.getDefault().getAppContext());

        crsSelectionPanel = new CrsSelectionPanel(productCrsForm, customCrsForm, predefinedCrsForm);
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellPadding(0, 0, new Insets(4, 10, 4, 4));
        final JPanel contentPanel = new JPanel(tableLayout);
        final JLabel label = new JLabel();
        label.setText("<html><b>" +
                              "The vector data are not associated with a coordinate reference system (CRS).<br/>" +
                              "Please specify a CRS so that coordinates can be interpreted correctly.</b>");

        contentPanel.add(label);
        contentPanel.add(crsSelectionPanel);
        setContent(contentPanel);
    }

    public CoordinateReferenceSystem getFeatureCrs() {
        CoordinateReferenceSystem crs = null;
        try {
            crs = crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(product));
        } catch (FactoryException e) {
            SnapDialogs.showError(title,
                                  "Cannot create coordinate reference system.\n" + e.getMessage());
        }
        return crs;
    }

    @Override
    protected void onOK() {
        super.onOK();
        getParent().setVisible(true);    // todo: Visat main window disappears otherwise, find better solution
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        getParent().setVisible(true);   // todo: Visat main window disappears otherwise, find better solution
    }

}
