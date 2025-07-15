package org.esa.snap.rcp.spectrum;

//todo copied from time-series-tool: move to BEAM or Ceres

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class WorkerChain {

    private final List<SwingWorker> synchronizedWorkerChain;
    private SwingWorker unchainedWorker;
    private boolean workerIsRunning = false;

    WorkerChain() {
        synchronizedWorkerChain = Collections.synchronizedList(new ArrayList<SwingWorker>());
    }

    synchronized void setOrExecuteNextWorker(SwingWorker w, boolean chained) {
        if (w == null) {
            printDebugMsg("w == null");
            return;
        }
        if (workerIsRunning) {
            printDebugMsg("workerIsRunning");
            if (chained) {
                printDebugMsg("synchronizedWorkerChain.add(w)");
                synchronizedWorkerChain.add(w);
            } else {
                printDebugMsg("unchainedWorker = w");
                unchainedWorker = w;
            }
        } else {
            printDebugMsg("workerIsRunning = FALSE");
            if (chained) {
                printDebugMsg("executeFirstWorkerInChain");
                synchronizedWorkerChain.add(w);
                executeFirstWorkerInChain();
            } else {
                unchainedWorker = w;
                printDebugMsg("w.execute");
                w.execute();
            }
            workerIsRunning = true;
        }
    }

    synchronized void removeCurrentWorkerAndExecuteNext(SwingWorker currentWorker) {
        synchronizedWorkerChain.remove(currentWorker);
        if (unchainedWorker == currentWorker) {
            unchainedWorker = null;
        }
        if (synchronizedWorkerChain.size() > 0) {
            executeFirstWorkerInChain();
            return;
        }
        if (unchainedWorker != null) {
            unchainedWorker.execute();
            return;
        }
        workerIsRunning = false;
    }

    private void executeFirstWorkerInChain() {
        synchronizedWorkerChain.get(0).execute();
    }

    static void printDebugMsg(String msg) {
        boolean debugOn = false;
        if (debugOn) {
            System.out.println(msg);
        }
    }
}
