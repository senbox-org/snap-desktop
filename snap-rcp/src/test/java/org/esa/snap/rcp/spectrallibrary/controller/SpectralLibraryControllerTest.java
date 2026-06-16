package org.esa.snap.rcp.spectrallibrary.controller;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.PreviewPanel;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.impl.SpectralLibraryServiceImpl;
import org.esa.snap.speclib.io.SpectralLibraryIO;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;


public class SpectralLibraryControllerTest {


    @Test
    @STTM("SNAP-4206")
    public void addProfilesAsVectorLayer_reportsProductExplorerDestinationOnSuccess() {
        SpectralProfile profile = createProfile("Profile_1")
                .withAttribute("wkt", AttributeValue.ofString("POINT (1 2)"));
        Fixture fixture = createFixture(profile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.addProfilesAsVectorLayer(product, List.of(profile.getId()), "spectral_profiles");

        VectorDataNode vectorDataNode = product.getVectorDataGroup().get("spectral_profiles");
        assertNotNull(vectorDataNode);
        assertEquals(1, vectorDataNode.getFeatureCollection().size());
        assertEquals(UiStatus.Severity.INFO, fixture.vm.getStatus().severity());
        assertEquals("Vector layer 'spectral_profiles' added under Product Explorer > Vector Data (1 features)",
                fixture.vm.getStatus().message());
    }

    @Test
    @STTM("SNAP-4206")
    public void addProfilesAsVectorLayer_reportsProductExplorerDestinationWhenLayerAlreadyExists() {
        SpectralProfile profile = createProfile("Profile_1")
                .withAttribute("wkt", AttributeValue.ofString("POINT (1 2)"));
        Fixture fixture = createFixture(profile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.addProfilesAsVectorLayer(product, List.of(profile.getId()), "spectral_profiles");
        VectorDataNode firstVectorDataNode = product.getVectorDataGroup().get("spectral_profiles");
        fixture.controller.addProfilesAsVectorLayer(product, List.of(profile.getId()), "spectral_profiles");

        assertSame(firstVectorDataNode, product.getVectorDataGroup().get("spectral_profiles"));
        assertEquals(UiStatus.Severity.WARN, fixture.vm.getStatus().severity());
        assertEquals("Vector layer 'spectral_profiles' already exists under Product Explorer > Vector Data.",
                fixture.vm.getStatus().message());
    }

    @Test
    @STTM("SNAP-4206")
    public void addProfilesAsVectorLayer_reportsMissingWktWhenNoGeometryCanBeAdded() {
        SpectralProfile profile = createProfile("Profile_1");
        Fixture fixture = createFixture(profile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.addProfilesAsVectorLayer(product, List.of(profile.getId()), "spectral_profiles");

        assertNull(product.getVectorDataGroup().get("spectral_profiles"));
        assertEquals(UiStatus.Severity.WARN, fixture.vm.getStatus().severity());
        assertEquals("No vector layer created: selected profiles have no valid 'wkt' geometry",
                fixture.vm.getStatus().message());
    }

    private static Fixture createFixture(SpectralProfile profile) {
        SpectralLibraryService service = new SpectralLibraryServiceImpl();
        SpectralLibraryViewModel vm = new SpectralLibraryViewModel();
        SpectralLibrary library = service.createLibrary("Library", new SpectralAxis(new double[]{500.0}, "nm"), "value");
        service.addProfiles(library.getId(), List.of(profile));
        vm.setActiveLibraryId(library.getId());

        SpectralLibraryController controller = new SpectralLibraryController(
                service,
                mock(SpectralLibraryIO.class),
                vm,
                mock(PreviewPanel.class)
        );
        return new Fixture(controller, vm);
    }

    private static SpectralProfile createProfile(String name) {
        return SpectralProfile.create(name, SpectralSignature.of(new double[]{1.0}));
    }

    private record Fixture(SpectralLibraryController controller, SpectralLibraryViewModel vm) {
    }
}
