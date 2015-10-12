package org.esa.snap.rcp.placemark;

import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.Product;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class UndoablePlacemarkActionFactory {

    static UndoablePlacemarkAction createUndoablePlacemarkRemoval(Product product, List<Placemark> placemarks,
                                                                  PlacemarkDescriptor placemarkDescriptor) {
        return new UndoablePlacemarkAction(new UndoablePlacemarkRemovalStrategy(product, placemarks, placemarkDescriptor));
    }

    static UndoablePlacemarkAction createUndoablePlacemarkInsertion(Product product, Placemark placemark,
                                                                    PlacemarkDescriptor placemarkDescriptor) {
        return new UndoablePlacemarkAction(new UndoablePlacemarkInsertionStrategy(product, placemark, placemarkDescriptor));
    }

    static UndoablePlacemarkAction createUndoablePlacemarkCopying(Product product, Placemark placemark,
                                                                  PlacemarkDescriptor placemarkDescriptor) {
        return new UndoablePlacemarkAction(new UndoablePlacemarkCopyingStrategy(product, placemark, placemarkDescriptor));
    }

    static UndoablePlacemarkAction createUndoablePlacemarkEditing(Product product, Placemark oldPlacemark, Placemark newPlacemark,
                                                                  PlacemarkDescriptor placemarkDescriptor) {
        return new UndoablePlacemarkAction(new UndoablePlacemarkEditingStrategy(product, oldPlacemark, newPlacemark, placemarkDescriptor));
    }

    private static class UndoablePlacemarkAction extends AbstractUndoableEdit {

        private UndoablePlacemarkActionStrategy strategy;

        public UndoablePlacemarkAction(UndoablePlacemarkActionStrategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public void undo() {
            super.undo();
            strategy.undo();
        }

        @Override
        public void redo() {
            super.redo();
            strategy.redo();
        }

        @Override
        public String getPresentationName() {
            return strategy.getPresentationName();
        }

        @Override
        public void die() {
            super.die();
            strategy = null;
        }

    }

    private interface UndoablePlacemarkActionStrategy {

        void undo();

        void redo();

        String getPresentationName();

    }

    private static class UndoablePlacemarkInsertionStrategy implements UndoablePlacemarkActionStrategy {

        private Product product;
        private Placemark newPlacemark;
        private PlacemarkDescriptor placemarkDescriptor;

        UndoablePlacemarkInsertionStrategy(Product product, Placemark newPlacemark, PlacemarkDescriptor placemarkDescriptor) {
            this.product = product;
            this.newPlacemark = newPlacemark;
            this.placemarkDescriptor = placemarkDescriptor;
        }

        @Override
        public void undo() {
            placemarkDescriptor.getPlacemarkGroup(product).remove(newPlacemark);
        }

        @Override
        public void redo() {
            placemarkDescriptor.getPlacemarkGroup(product).add(newPlacemark);
        }

        @Override
        public String getPresentationName() {
            return "Insert " + placemarkDescriptor.getRoleLabel();
        }

    }

    private static class UndoablePlacemarkCopyingStrategy implements UndoablePlacemarkActionStrategy {

        private Product product;
        private Placemark newPlacemark;
        private PlacemarkDescriptor placemarkDescriptor;

        UndoablePlacemarkCopyingStrategy(Product product, Placemark newPlacemark, PlacemarkDescriptor placemarkDescriptor) {
            this.product = product;
            this.newPlacemark = newPlacemark;
            this.placemarkDescriptor = placemarkDescriptor;
        }

        @Override
        public void undo() {
            placemarkDescriptor.getPlacemarkGroup(product).remove(newPlacemark);
        }

        @Override
        public void redo() {
            placemarkDescriptor.getPlacemarkGroup(product).add(newPlacemark);
        }

        @Override
        public String getPresentationName() {
            return "Copying " + placemarkDescriptor.getRoleLabel();
        }

    }

    //todo does not work satisfyingly yet -> placemarks are added to the end of the table
    private static class UndoablePlacemarkEditingStrategy implements UndoablePlacemarkActionStrategy {

        private final Placemark oldPlacemark;
        private Product product;
        private Placemark newPlacemark;
        private Placemark placemarkInView;
        private PlacemarkDescriptor placemarkDescriptor;

        UndoablePlacemarkEditingStrategy(Product product, Placemark oldPlacemark, Placemark newPlacemark, PlacemarkDescriptor placemarkDescriptor) {
            this.product = product;
            this.oldPlacemark = oldPlacemark;
            this.newPlacemark = Placemark.createPointPlacemark(newPlacemark.getDescriptor(),
                                                               newPlacemark.getName(),
                                                               newPlacemark.getLabel(),
                                                               newPlacemark.getDescription(),
                                                               newPlacemark.getPixelPos(),
                                                               newPlacemark.getGeoPos(),
                                                               newPlacemark.getProduct().getSceneGeoCoding());
            placemarkInView = newPlacemark;
            this.placemarkDescriptor = placemarkDescriptor;
        }

        @Override
        public void undo() {
            placemarkInView.setName(oldPlacemark.getName());
            placemarkInView.setLabel(oldPlacemark.getLabel());
            placemarkInView.setDescription(oldPlacemark.getDescription());
            placemarkInView.setGeoPos(oldPlacemark.getGeoPos());
            placemarkInView.setStyleCss(oldPlacemark.getStyleCss());
        }

        @Override
        public void redo() {
            placemarkInView.setName(newPlacemark.getName());
            placemarkInView.setLabel(newPlacemark.getLabel());
            placemarkInView.setDescription(newPlacemark.getDescription());
            placemarkInView.setGeoPos(newPlacemark.getGeoPos());
            placemarkInView.setStyleCss(newPlacemark.getStyleCss());
        }

        @Override
        public String getPresentationName() {
            return "Editing " + placemarkDescriptor.getRoleLabel();
        }

    }

    private static class UndoablePlacemarkRemovalStrategy implements UndoablePlacemarkActionStrategy {

        private final List<Placemark> placemarks;
        private Product product;
        private PlacemarkDescriptor placemarkDescriptor;

        UndoablePlacemarkRemovalStrategy(Product product, List<Placemark> placemarks, PlacemarkDescriptor placemarkDescriptor) {
            this.product = product;
            this.placemarks = placemarks;
            this.placemarkDescriptor = placemarkDescriptor;
        }

        @Override
        public void undo() {
            final PlacemarkGroup placemarkGroup = placemarkDescriptor.getPlacemarkGroup(product);
            for (Placemark placemark : placemarks) {
                placemarkGroup.add(placemark);
            }
        }

        @Override
        public void redo() {
            final PlacemarkGroup placemarkGroup = placemarkDescriptor.getPlacemarkGroup(product);
            for (Placemark placemark : placemarks) {
                placemarkGroup.remove(placemark);
            }
        }

        @Override
        public String getPresentationName() {
            return "Removing " + placemarkDescriptor.getRoleLabel();
        }

    }

}
