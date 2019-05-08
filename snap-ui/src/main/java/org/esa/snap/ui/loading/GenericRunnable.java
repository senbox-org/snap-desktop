package org.esa.snap.ui.loading;

/**
 * Created by jcoravu on 29/3/2019.
 */
public abstract class GenericRunnable<ItemType> implements Runnable {

    private final ItemType item;

    public GenericRunnable(ItemType item) {
        this.item = item;
    }

    protected abstract void execute(ItemType item);

    @Override
    public void run() {
        execute(this.item);
    }
}
