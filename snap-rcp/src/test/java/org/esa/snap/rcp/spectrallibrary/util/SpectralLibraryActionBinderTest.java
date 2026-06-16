package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.ui.product.spectrum.DisplayableSpectrum;
import org.esa.snap.ui.product.spectrum.SpectrumShapeProvider;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpectralLibraryActionBinderTest {

    @Test
    public void buildSpectraForChooser_limitsBandsToActiveAxisAndUsesValidSymbolIndexes() throws Exception {
        Product product = new Product("p", "t", 1, 1);
        StringBuilder grouping = new StringBuilder();
        Set<String> activeAxisBands = new LinkedHashSet<>();

        for (int i = 0; i < 15; i++) {
            if (i > 0) {
                grouping.append(':');
            }
            grouping.append("group").append(i).append("_*");
            String bandName = "group" + i + "_band";
            addSpectralBand(product, bandName, 500.0f + i);
            activeAxisBands.add(bandName);
        }

        addSpectralBand(product, "outside_axis_band", 900.0f);
        product.setAutoGrouping(grouping.toString());

        DisplayableSpectrum[] spectra = invokeBuildSpectraForChooser(
                product, product.getBand("group0_band"), activeAxisBands);

        int bandCount = 0;
        for (DisplayableSpectrum spectrum : spectra) {
            assertTrue(spectrum.getSymbolIndex() >= 0);
            assertTrue(spectrum.getSymbolIndex() < SpectrumShapeProvider.getShapeIcons().length);
            for (Band band : spectrum.getSpectralBands()) {
                assertTrue(activeAxisBands.contains(band.getName()));
                bandCount++;
            }
        }
        assertEquals(activeAxisBands.size(), bandCount);
    }

    @Test
    public void saveFilterState_canPersistChosenAxisAfterRestoreInitializedDefaultAxis() throws Exception {
        Product product = new Product("p", "t", 1, 1);
        addSpectralBand(product, "axis_band_1", 500.0f);
        addSpectralBand(product, "axis_band_2", 501.0f);
        addSpectralBand(product, "other_axis_band", 900.0f);

        SpectralLibraryActionBinder binder = new SpectralLibraryActionBinder(null, null, null, null);
        UUID libraryId = UUID.randomUUID();
        Set<String> chosenAxisBands = new LinkedHashSet<>();
        chosenAxisBands.add("axis_band_1");
        chosenAxisBands.add("axis_band_2");

        invokeRestoreOrInitFilterState(binder, libraryId, product);
        invokeSaveFilterState(binder, libraryId, chosenAxisBands);
        invokeRestoreOrInitFilterState(binder, libraryId, product);

        DisplayableSpectrum[] spectra = invokeBuildSpectraForChooser(
                binder, product, product.getBand("axis_band_1"), invokeGetAllowedBands(binder, product));

        int bandCount = 0;
        for (DisplayableSpectrum spectrum : spectra) {
            for (Band band : spectrum.getSpectralBands()) {
                assertTrue(chosenAxisBands.contains(band.getName()));
                bandCount++;
            }
        }
        assertEquals(chosenAxisBands.size(), bandCount);
    }

    private static DisplayableSpectrum[] invokeBuildSpectraForChooser(Product product, Band raster,
                                                                      Set<String> activeAxisBands) throws Exception {
        SpectralLibraryActionBinder binder = new SpectralLibraryActionBinder(null, null, null, null);
        return invokeBuildSpectraForChooser(binder, product, raster, activeAxisBands);
    }

    private static DisplayableSpectrum[] invokeBuildSpectraForChooser(SpectralLibraryActionBinder binder,
                                                                      Product product, Band raster,
                                                                      Set<String> activeAxisBands) throws Exception {
        Method method = SpectralLibraryActionBinder.class.getDeclaredMethod(
                "buildSpectraForChooser", Product.class, org.esa.snap.core.datamodel.RasterDataNode.class, Set.class);
        method.setAccessible(true);
        return (DisplayableSpectrum[]) method.invoke(binder, product, raster, activeAxisBands);
    }

    private static void invokeRestoreOrInitFilterState(SpectralLibraryActionBinder binder, UUID libraryId,
                                                       Product product) throws Exception {
        Method method = SpectralLibraryActionBinder.class.getDeclaredMethod(
                "restoreOrInitFilterState", UUID.class, Product.class);
        method.setAccessible(true);
        method.invoke(binder, libraryId, product);
    }

    private static void invokeSaveFilterState(SpectralLibraryActionBinder binder, UUID libraryId,
                                              Set<String> activeAxisBands) throws Exception {
        Method method = SpectralLibraryActionBinder.class.getDeclaredMethod(
                "saveFilterState", UUID.class, Map.class, Set.class);
        method.setAccessible(true);
        method.invoke(binder, libraryId, null, activeAxisBands);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> invokeGetAllowedBands(SpectralLibraryActionBinder binder, Product product) throws Exception {
        Method method = SpectralLibraryActionBinder.class.getDeclaredMethod("getAllowedBands", Product.class);
        method.setAccessible(true);
        return (Set<String>) method.invoke(binder, product);
    }

    private static void addSpectralBand(Product product, String name, float wavelength) {
        Band band = new Band(name, ProductData.TYPE_FLOAT32, 1, 1);
        band.setSpectralWavelength(wavelength);
        product.addBand(band);
    }
}
