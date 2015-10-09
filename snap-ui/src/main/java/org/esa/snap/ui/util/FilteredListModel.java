package org.esa.snap.ui.util;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;

// found at http://stackoverflow.com/questions/14758313/filtering-jlist-based-on-jtextfield
public class FilteredListModel<T> extends AbstractListModel {

    public static interface Filter<T> {

        boolean accept(T element);
    }

    private final ListModel<T> sourceModel;
    private Filter<T> filter;
    private final ArrayList<Integer> indices = new ArrayList<>();

    public FilteredListModel(ListModel<T> source) {
        if (source == null) {
            throw new IllegalArgumentException("Source is null");
        }
        sourceModel = source;
        sourceModel.addListDataListener(new ListDataListener() {
            public void intervalRemoved(ListDataEvent e) {
                doFilter();
            }

            public void intervalAdded(ListDataEvent e) {
                doFilter();
            }

            public void contentsChanged(ListDataEvent e) {
                doFilter();
            }
        });
    }

    public void setFilter(Filter<T> f) {
        filter = f;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                doFilter();
                return null;
            }

            @Override
            protected void done() {
                fireContentsChanged(this, 0, getSize() - 1);
            }
        };
        worker.execute();
    }

    private void doFilter() {
        indices.clear();

        Filter<T> f = filter;
        if (f != null) {
            int count = sourceModel.getSize();
            for (int i = 0; i < count; i++) {
                T element = sourceModel.getElementAt(i);
                if (f.accept(element)) {
                    indices.add(i);
                }
            }
        }
    }

    public int getSize() {
        return (filter != null) ? indices.size() : sourceModel.getSize();
    }

    public T getElementAt(int index) {
        return (filter != null) ? sourceModel.getElementAt(indices.get(index)) : sourceModel.getElementAt(index);
    }
}
