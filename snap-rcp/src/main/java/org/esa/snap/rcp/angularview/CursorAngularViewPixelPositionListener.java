package org.esa.snap.rcp.angularview;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.ui.PixelPositionListener;

import javax.swing.SwingWorker;
import java.awt.event.MouseEvent;

public class CursorAngularViewPixelPositionListener implements PixelPositionListener {

    private final AngularTopComponent topComponent;
    private final WorkerChain workerChain;
    private final WorkerChainSupport support;

    public CursorAngularViewPixelPositionListener(AngularTopComponent topComponent) {
        this.topComponent = topComponent;
        workerChain = new WorkerChain();
        support = new WorkerChainSupport() {
            @Override
            public void removeWorkerAndStartNext(SwingWorker worker) {
                workerChain.removeCurrentWorkerAndExecuteNext(worker);
            }
        };
    }

    @Override
    public void pixelPosChanged(ImageLayer imageLayer,
                                int pixelX,
                                int pixelY,
                                int currentLevel,
                                boolean pixelPosValid,
                                MouseEvent e) {
        CursorAngularViewUpdater worker = new CursorAngularViewUpdater(pixelPosValid, pixelX, pixelY, currentLevel, e.isShiftDown(), support);
        workerChain.setOrExecuteNextWorker(worker, false);
    }

    @Override
    public void pixelPosNotAvailable() {
        CursorAngularViewRemover worker = new CursorAngularViewRemover(support);
        workerChain.setOrExecuteNextWorker(worker, false);
    }

    private boolean shouldUpdateCursorPosition() {
        return topComponent.isVisible() && topComponent.isShowingCursorAngularView();
    }


    //todo copied (and changed very slightly) from time-series-tool: Move to BEAM or Ceres
    static interface WorkerChainSupport {

        void removeWorkerAndStartNext(SwingWorker worker);
    }


    private class CursorAngularViewRemover extends SwingWorker<Void, Void> {

        private final WorkerChainSupport support;

        CursorAngularViewRemover(WorkerChainSupport support) {
            this.support = support;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (shouldUpdateCursorPosition()) {
                topComponent.removeCursorAngularViewsFromDataset();
            }
            return null;
        }

        @Override
        protected void done() {
            topComponent.updateChart();
            if (topComponent.isShowingCursorAngularView()) {
                topComponent.setMessageCursorNotOnImage();
            } else {
//                topComponent.setPlotChartMessage("Cursor Mode is de-activated.");
            }

            support.removeWorkerAndStartNext(this);
        }
    }

    private class CursorAngularViewUpdater extends SwingWorker<Void, Void> {

        private final boolean pixelPosValid;
        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;
        private final boolean adjustAxes;
        private final WorkerChainSupport support;

        CursorAngularViewUpdater(boolean pixelPosValid, int pixelX, int pixelY, int currentLevel, boolean adjustAxes,
                             WorkerChainSupport support) {
            this.pixelPosValid = pixelPosValid;
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.currentLevel = currentLevel;
            this.adjustAxes = adjustAxes;
            this.support = support;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (shouldUpdateCursorPosition()) {
                Waiter waiter = new Waiter();
                waiter.execute();
                topComponent.updateData(pixelX, pixelY, currentLevel, pixelPosValid);
                waiter.cancel(true);
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                topComponent.updateChart(adjustAxes);
            } finally {
                support.removeWorkerAndStartNext(this);
            }
        }
    }

    private class Waiter extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            Thread.sleep(1000);
            return null;
        }

        @Override
        protected void done() {
//            topComponent.setPrepareForUpdateMessage();
        }
    }


}
