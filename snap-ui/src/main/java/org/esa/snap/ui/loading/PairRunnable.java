package org.esa.snap.ui.loading;

/**
 * Created by jcoravu on 19/2/2020.
 */
public abstract class PairRunnable<First, Second> implements Runnable {

    private final First first;
    private final Second second;

    public PairRunnable(First first, Second second) {
        this.first = first;
        this.second = second;
    }

    protected abstract void execute(First first, Second second);

    @Override
    public void run() {
        execute(this.first, this.second);
    }
}
