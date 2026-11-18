package org.esa.snap.rcp.imgfilter;

import com.bc.ceres.core.runtime.internal.IOHelper;
import com.thoughtworks.xstream.XStream;
import org.esa.snap.rcp.imgfilter.model.FilterSet;
import org.esa.snap.rcp.imgfilter.model.FilterSetStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class used to store/reload user-defined image filters.
 *
 * @author Norman
 */
public class FilterSetFileStore implements FilterSetStore {

    final Path filtersDir;

    public FilterSetFileStore(Path filtersDir) {
        this.filtersDir = filtersDir;
    }

    public List<FilterSet> loadFilterSetModels() throws IOException {
        XStream xStream = FilterSet.createXStream();

        final Collection<Path> files = IOHelper.listFiles(filtersDir, path -> path.getFileName().toString().endsWith(".xml"));
        ArrayList<FilterSet> list = new ArrayList<>();
        if (files == null) {
            return list;
        }
        IOException ioe = null;
        for (Path file : files) {
            try (InputStream is = Files.newInputStream(file)) {
                FilterSet filterSet = new FilterSet();
                xStream.fromXML(is, filterSet);
                list.add(filterSet);
            } catch (Exception e) {
                ioe = new IOException(e);
            }
        }

        if (ioe != null && list.isEmpty()) {
            throw ioe;
        }

        return list;
    }

    @Override
    public void storeFilterSetModel(FilterSet filterSet) throws IOException {
        if (!Files.exists(filtersDir) && !IOHelper.createDirectories(filtersDir)) {
            throw new IOException("Failed to create directory\n" + filtersDir);
        }
        final Path file = filtersDir.resolve(filterSet.getName().toLowerCase() + "-filters.xml");
        XStream xStream = FilterSet.createXStream();
        try (Writer fileWriter = Files.newBufferedWriter(file)) {
            xStream.toXML(filterSet, fileWriter);
        }
    }


}
