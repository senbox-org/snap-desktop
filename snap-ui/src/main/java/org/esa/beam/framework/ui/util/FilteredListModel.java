package org.esa.beam.framework.ui.util;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
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
        doFilter();
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
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public int getSize() {
        return (filter != null) ? indices.size() : sourceModel.getSize();
    }

    public T getElementAt(int index) {
        return (filter != null) ? sourceModel.getElementAt(indices.get(index)) : sourceModel.getElementAt(index);
    }
}