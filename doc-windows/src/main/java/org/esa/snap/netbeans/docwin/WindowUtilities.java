package org.esa.snap.netbeans.docwin;

import org.openide.windows.TopComponent;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Various document window utilities.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
public class WindowUtilities {
    final static TcProvider DEFAULT_TC_PROVIDER = () -> TopComponent.getRegistry().getOpened();
    static TcProvider tcProvider = DEFAULT_TC_PROVIDER;

    /**
     * Gets a unique window title.
     *
     * @param titleBase  The title base.
     * @param windowType The window type.
     * @return A unique window title.
     */
    public static String getUniqueTitle(String titleBase, Class<? extends TopComponent> windowType) {
        List<String> titles = getOpened(windowType).map(TopComponent::getDisplayName).collect(Collectors.toList());

        if (titles.isEmpty()) {
            return titleBase;
        }

        if (!titles.contains(titleBase)) {
            return titleBase;
        }

        for (int i = 2; ; i++) {
            final String title = String.format("%s (%d)", titleBase, i);
            if (!titles.contains(title)) {
                return title;
            }
        }
    }

    /**
     * Gets a stream of components of type {@code T} which may be implemented by opened {@link TopComponent}s.
     * The stream also includes components that are part of opened {@link WorkspaceTopComponent}s.
     *
     * @param type The interface implemented by or the class extended by an opened {@link TopComponent}
     * @param <T>  The type's type.
     * @return A stream of components of type {@code T}.
     */
    public static <T> Stream<T> getOpened(final Class<T> type) {
        return tcProvider.getOpened().stream()
                .flatMap(topComponent -> {
                    if (topComponent instanceof WindowContainer) {
                        return Stream.concat(Stream.of(topComponent),
                                             ((WindowContainer) topComponent).getOpenedWindows().stream());
                    }
                    return Stream.of(topComponent);
                })
                .filter(topComponent -> type.isAssignableFrom(topComponent.getClass()))
                .map(topComponent -> (T) topComponent);
    }

    static interface TcProvider {
        Collection<TopComponent> getOpened();
    }
}
