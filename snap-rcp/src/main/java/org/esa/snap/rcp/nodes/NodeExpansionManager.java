package org.esa.snap.rcp.nodes;

import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.windows.ProductExplorerTopComponent;
import org.openide.nodes.Node;

/**
 * @author Tonio Fincke
 */
public class NodeExpansionManager {

    public static boolean isNodeExpanded(Node node) {
        final ProductExplorerTopComponent topComponent =
                WindowUtilities.getOpened(ProductExplorerTopComponent.class).findFirst().orElse(null);
        if (topComponent != null) {
            return topComponent.isNodeExpanded(node);
        }
        return false;
    }

    public static void expandNode(Node node) {
        final ProductExplorerTopComponent topComponent =
                WindowUtilities.getOpened(ProductExplorerTopComponent.class).findFirst().orElse(null);
        if (topComponent != null) {
            topComponent.expandNode(node);
        }
    }

}
