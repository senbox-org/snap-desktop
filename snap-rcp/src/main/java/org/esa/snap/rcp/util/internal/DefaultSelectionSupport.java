package org.esa.snap.rcp.util.internal;

import org.esa.snap.rcp.util.SelectionSupport;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DefaultSelectionSupport<T> implements SelectionSupport<T> {

    private final Class<T> type;
    private final Lookup.Result<T> itemResult;
    private final LinkedList<Handler<T>> handlerList;
    private final LookupListener lookupListener;
    private Collection<? extends T> currentlySelectedItems;
    private LookupListener theWeakListener;

    public DefaultSelectionSupport(Class<T> type) {
        this.type = type;
        currentlySelectedItems = Collections.emptyList();
        itemResult = Utilities.actionsGlobalContext().lookupResult(type);
        handlerList = new LinkedList<>();
        lookupListener = createLookupListener();
    }

    @Override
    public void addHandler(Handler<T> handler) {
        if (handlerList.isEmpty() && theWeakListener == null) { // first listener added --> add LookupListener
            theWeakListener = WeakListeners.create(LookupListener.class, lookupListener, itemResult);
            itemResult.addLookupListener(theWeakListener);
        }
        handlerList.add(handler);
    }

    @Override
    public void removeHandler(Handler<T> handler) {
        handlerList.remove(handler);
        if (handlerList.isEmpty()) { // last listener removed --> remove LookupListener
            itemResult.removeLookupListener(theWeakListener);
            theWeakListener = null;
        }
    }

    private LookupListener createLookupListener() {
        return ev -> {
            Collection<? extends T> allItems = itemResult.allInstances();


            Stream<? extends T> deselectedStream = currentlySelectedItems.stream().filter((Predicate<T>) (o) -> !allItems.contains(o));
            T[] allDeselected = deselectedStream.toArray(value -> (T[]) Array.newInstance(type, value));
            T firstDeselected = null;
            if (allDeselected.length > 0) {
                firstDeselected = allDeselected[0];
            }

            Stream<? extends T> newlySelectedStream = allItems.stream().filter((Predicate<T>) (o) -> !currentlySelectedItems.contains(o));
            T[] allNewlySelected = newlySelectedStream.toArray(value -> (T[]) Array.newInstance(type, value));
            T firstSelected = null;
            if (allNewlySelected.length > 0) {
                firstSelected = allNewlySelected[0];
            }

            currentlySelectedItems = allItems;

            // todo check if implementation is correct - ASAP!

            for (Handler<T> handler : handlerList) {
                handler.selectionChange(firstDeselected, firstSelected);
            }
        };
    }

}
