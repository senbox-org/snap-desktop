package org.esa.snap.rcp.util;

import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SelectionChangeSupport<T> {

    private final Class<T> type;
    private final Lookup.Result<T> itemResult;
    private final LinkedList<Listener<T>> listenerList;
    private final LookupListener lookupListener;
    private Collection<? extends T> currentlySelectedItems;

    public SelectionChangeSupport(Class<T> type) {
        this.type = type;
        currentlySelectedItems = Collections.emptyList();
        itemResult = Utilities.actionsGlobalContext().lookupResult(type);
        listenerList = new LinkedList<>();
        lookupListener = createLookupListener();
    }

    public void addSelectionChangeListener(Listener<T> scl) {
        if (listenerList.isEmpty()) { // first listener added --> add LookupListener
            itemResult.addLookupListener(WeakListeners.create(LookupListener.class, lookupListener, itemResult));
        }
        listenerList.add(scl);
    }

    public void removeSelectionChangeListener(Listener<T> scl) {
        listenerList.remove(scl);
        if (listenerList.isEmpty()) { // last listener removed --> remove LookupListener
            itemResult.removeLookupListener(WeakListeners.create(LookupListener.class, lookupListener, itemResult));
        }
    }

    private LookupListener createLookupListener() {
        return ev -> {
            Collection<? extends T> allItems = itemResult.allInstances();


            Stream<? extends T> deselectedStream = currentlySelectedItems.stream().filter((Predicate<T>) (o) -> !allItems.contains(o));
            T[] allDeselected = deselectedStream.toArray(value -> (T[]) Array.newInstance(type, value));
            T firstDeselected = null;
            T[] moreDeselected = null;
            if (allDeselected.length > 0) {
                firstDeselected = allDeselected[0];
                moreDeselected = allDeselected.length > 1 ? Arrays.copyOfRange(allDeselected, 1, allDeselected.length) : (T[]) Array.newInstance(type, 0);
            }

            Stream<? extends T> newlySelectedStream = allItems.stream().filter((Predicate<T>) (o) -> !currentlySelectedItems.contains(o));
            T[] allNewlySelected = newlySelectedStream.toArray(value -> (T[]) Array.newInstance(type, value));
            T firstSelected = null;
            T[] moreSelected = null;
            if (allNewlySelected.length > 0) {
                firstSelected = allNewlySelected[0];
                moreSelected = allNewlySelected.length > 1 ? Arrays.copyOfRange(allNewlySelected, 1, allNewlySelected.length) : (T[]) Array.newInstance(type, 0);
            }

            currentlySelectedItems = allItems;

            if (firstDeselected != null) {
                for (Listener<T> listener : listenerList) {
                    listener.deselected(firstDeselected, moreDeselected);
                }
            }
            if (firstSelected != null) {
                for (Listener<T> listener : listenerList) {
                    listener.selected(firstSelected, moreSelected);
                }
            }

        };
    }

    public static interface Listener<T> {
        void selected(T first, T... more);
        void deselected(T first, T... more);
    }
}
