package org.esa.snap.rcp.spectrallibrary.controller;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
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

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SpectralLibraryControllerTest {


    @Test
    @STTM("SNAP-4206")
    public void removePreviewProfilesForProduct_removesOnlyProductDerivedPreviewProfiles() {
        SpectralProfile profileFromClosedProduct = createProfile("Profile_from_closed_product");
        SpectralProfile profileFromOpenProduct = createProfile("Profile_from_open_product");
        SpectralProfile profileFromLibrary = createProfile("Profile_from_library");
        Fixture fixture = createFixture(profileFromClosedProduct, profileFromOpenProduct, profileFromLibrary);
        Product closedProduct = new Product("closed_product", "type", 1, 1);
        Product openProduct = new Product("open_product", "type", 1, 1);

        fixture.controller.setPreviewProfilesLimited(List.of(profileFromClosedProduct, profileFromOpenProduct, profileFromLibrary));
        fixture.controller.trackProductPreviewOrigins(closedProduct, List.of(profileFromClosedProduct));
        fixture.controller.trackProductPreviewOrigins(openProduct, List.of(profileFromOpenProduct));

        int removed = fixture.controller.removePreviewProfilesForProduct(closedProduct);

        assertEquals(1, removed);
        assertEquals(List.of(profileFromOpenProduct, profileFromLibrary), fixture.vm.getPreviewProfiles());
        assertEquals(List.of(profileFromOpenProduct, profileFromLibrary), fixture.controller.getAllPreviewProfilesForAdd());
        assertEquals(UiStatus.Severity.INFO, fixture.vm.getStatus().severity());
        assertEquals("Preview updated: removed 1 profiles for closed product 'closed_product'", fixture.vm.getStatus().message());
    }

    @Test
    @STTM("SNAP-4206")
    public void removePreviewProfilesForGeometryFeatures_removesOnlyMatchingFeaturePreviewProfiles() {
        SpectralProfile featureOneProfile = createProfile("Feature_1");
        SpectralProfile featureTwoProfile = createProfile("Feature_2");
        Fixture fixture = createFixture(featureOneProfile, featureTwoProfile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.setPreviewProfilesLimited(List.of(featureOneProfile, featureTwoProfile));
        fixture.controller.trackGeometryPreviewOrigins(product, "geometry", "geometry.1", List.of(featureOneProfile));
        fixture.controller.trackGeometryPreviewOrigins(product, "geometry", "geometry.2", List.of(featureTwoProfile));

        int removed = fixture.controller.removePreviewProfilesForGeometryFeatures(product, "geometry", Set.of("geometry.1"));

        assertEquals(1, removed);
        assertEquals(List.of(featureTwoProfile), fixture.vm.getPreviewProfiles());
        assertEquals(List.of(featureTwoProfile), fixture.controller.getAllPreviewProfilesForAdd());
        assertEquals(UiStatus.Severity.INFO, fixture.vm.getStatus().severity());
        assertEquals("Preview updated: removed 1 profiles for deleted or changed geometry", fixture.vm.getStatus().message());
    }

    @Test
    @STTM("SNAP-4206")
    public void removePreviewProfilesForChangedGeometryFeatures_keepsPreviewWhenGeometryIsUnchanged() {
        SpectralProfile featureProfile = createProfile("Feature_1");
        Fixture fixture = createFixture(featureProfile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.setPreviewProfilesLimited(List.of(featureProfile));
        fixture.controller.trackGeometryPreviewOrigins(product, "geometry", "geometry.1", "POINT (1 2)", List.of(featureProfile));

        int removed = fixture.controller.removePreviewProfilesForChangedGeometryFeatures(
                product, "geometry", Map.of("geometry.1", "POINT (1 2)"));

        assertEquals(0, removed);
        assertEquals(List.of(featureProfile), fixture.vm.getPreviewProfiles());
        assertEquals(List.of(featureProfile), fixture.controller.getAllPreviewProfilesForAdd());
        assertEquals("Preview updated (1)", fixture.vm.getStatus().message());
    }

    @Test
    @STTM("SNAP-4206")
    public void removePreviewProfilesForChangedGeometryFeatures_removesPreviewWhenGeometryChanged() {
        SpectralProfile featureProfile = createProfile("Feature_1");
        Fixture fixture = createFixture(featureProfile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.setPreviewProfilesLimited(List.of(featureProfile));
        fixture.controller.trackGeometryPreviewOrigins(product, "geometry", "geometry.1", "POINT (1 2)", List.of(featureProfile));

        int removed = fixture.controller.removePreviewProfilesForChangedGeometryFeatures(
                product, "geometry", Map.of("geometry.1", "POINT (2 3)"));

        assertEquals(1, removed);
        assertEquals(List.of(), fixture.vm.getPreviewProfiles());
        assertEquals(List.of(), fixture.controller.getAllPreviewProfilesForAdd());
        assertEquals("Preview updated: removed 1 profiles for deleted or changed geometry", fixture.vm.getStatus().message());
    }

    @Test
    @STTM("SNAP-4206")
    public void removePreviewProfilesForPin_removesOnlyMatchingPinPreviewProfiles() {
        SpectralProfile pinProfile = createProfile("Pin_1");
        SpectralProfile otherPinProfile = createProfile("Pin_2");
        Fixture fixture = createFixture(pinProfile, otherPinProfile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.setPreviewProfilesLimited(List.of(pinProfile, otherPinProfile));
        fixture.controller.trackPinPreviewOrigins(product, "pin.1", List.of(pinProfile));
        fixture.controller.trackPinPreviewOrigins(product, "pin.2", List.of(otherPinProfile));

        int removed = fixture.controller.removePreviewProfilesForPin(product, "pin.1");

        assertEquals(1, removed);
        assertEquals(List.of(otherPinProfile), fixture.vm.getPreviewProfiles());
        assertEquals("Preview updated: removed 1 profiles for deleted pin 'pin.1'", fixture.vm.getStatus().message());
    }

    @Test
    @STTM("SNAP-4206")
    public void clearPreview_clearsPreviewOrigins() {
        SpectralProfile profile = createProfile("Profile");
        Fixture fixture = createFixture(profile);
        Product product = new Product("product", "type", 1, 1);

        fixture.controller.setPreviewProfilesLimited(List.of(profile));
        fixture.controller.trackProductPreviewOrigins(product, List.of(profile));

        fixture.controller.clearPreview();
        int removed = fixture.controller.removePreviewProfilesForProduct(product);

        assertEquals(0, removed);
        assertEquals(List.of(), fixture.vm.getPreviewProfiles());
        assertEquals(List.of(), fixture.controller.getAllPreviewProfilesForAdd());
        assertEquals(UiStatus.Severity.IDLE, fixture.vm.getStatus().severity());
    }

    @Test
    @STTM("SNAP-4206")
    public void removePreviewProfilesForProduct_cancelsRunningExtractionForSameProduct() throws Exception {
        CountDownLatch extractionStarted = new CountDownLatch(1);
        CountDownLatch releaseExtraction = new CountDownLatch(1);
        SpectralLibraryService service = mock(SpectralLibraryService.class);
        SpectralLibraryViewModel vm = new SpectralLibraryViewModel();
        SpectralAxis axis = new SpectralAxis(new double[]{500.0}, "nm");
        SpectralLibrary library = SpectralLibrary.create("Library", axis, "value");
        vm.setActiveLibraryId(library.getId());
        when(service.getLibrary(library.getId())).thenReturn(Optional.of(library));
        when(service.extractProfile(anyString(), any(), anyList(), anyInt(), anyInt(), anyInt(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    extractionStarted.countDown();
                    assertTrue(releaseExtraction.await(2, TimeUnit.SECONDS));
                    return Optional.of(createProfile("Late_Profile"));
                });

        SpectralLibraryController controller = new SpectralLibraryController(
                service,
                mock(SpectralLibraryIO.class),
                vm,
                mock(PreviewPanel.class)
        );
        Product product = new Product("closing_product", "type", 1, 1);

        controller.extractPreviewAtCursor(product, axis, "value", List.of(mock(Band.class)), 0, 0, 0, Set.of(), "Profile_");
        assertTrue(extractionStarted.await(2, TimeUnit.SECONDS));
        controller.removePreviewProfilesForProduct(product);
        releaseExtraction.countDown();
        flushSwingEvents();

        assertEquals(List.of(), vm.getPreviewProfiles());
        assertEquals(List.of(), controller.getAllPreviewProfilesForAdd());
    }

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

    private static Fixture createFixture(SpectralProfile... profiles) {
        SpectralLibraryService service = new SpectralLibraryServiceImpl();
        SpectralLibraryViewModel vm = new SpectralLibraryViewModel();
        SpectralLibrary library = service.createLibrary("Library", new SpectralAxis(new double[]{500.0}, "nm"), "value");
        service.addProfiles(library.getId(), List.of(profiles));
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

    private static void flushSwingEvents() throws Exception {
        for (int i = 0; i < 5; i++) {
            SwingUtilities.invokeAndWait(() -> {
            });
            Thread.sleep(20);
        }
    }

    private record Fixture(SpectralLibraryController controller, SpectralLibraryViewModel vm) {
    }
}
