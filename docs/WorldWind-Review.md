# SNAP WorldWind — Review, Status and Plan

Date: 2026-10-17. Branch: `worldwind-fixes` (snap-desktop and microwave-toolbox).

## 0. Origin

Request (2026-10-17): review all the WorldWind functionality in SNAP —

1. first determine any serious bugs to fix;
2. next determine if the WorldWind version is up to date. If so, is JOGL up to date, or can it not be
   updated beyond what the WorldWind version allows?
3. next determine how it could be improved, by adding functionality already available in WorldWind or by
   adding much needed new functionality.

The review report below (§1–§3) was first presented in the review session and approved for the bug-fix
part ("yes go ahead"). It is recorded here so that the later parts are not lost. §4 records what has been
done, §5 plans the quick wins of §3, and §6 specifies the three new features chosen for implementation.

---

## 1. Serious bugs to fix (review findings)

None of the WorldWind units was production ready at the time of the review. Every BLOCKER and the main
MAJOR findings were checked against the code.

| # | Severity | Where | Bug |
|---|---|---|---|
| 1 | BLOCKER | `snap-worldwind/.../productlibrary/WorldMapPanelWrapper.java:284,287` | If 3D failed to start (remote desktop, VM, driver change) and the saved view is 3D, `worldMap3DPanel.setFlatEarth()` hits a null panel. The Product Library map never appears, on every start, because the saved setting never resets. |
| 2 | MAJOR | `snap-product-library-ui-v2/.../thread/AbstractRunnable.java:44` | Only catches `Exception`. A JOGL `UnsatisfiedLinkError` or `NoClassDefFoundError` skips the fallback, so even the 2D map never shows. The "3D" menu items also hit a null panel when 3D is unavailable. Nothing listens for OpenGL failures at render time. |
| 3 | BLOCKER | `WWProductManagerListener.java:36-53` | Adding a product ran later on a new thread; removing ran immediately on the caller's thread. Every GPF dialog adds and then removes its target product from a background thread: outlines left on the globe permanently, and a removed or disposed product still selected. |
| 4 | BLOCKER | `ProductPanel.java:116` | Each checkbox constructor called `layer.setEnabled(selected)` on the whole Products layer. Untick the last product and the next selection hides every product. For outline-only products the checkbox did nothing. |
| 5 | MAJOR | `DefaultProductLayer.java:318-322` | Outline computation blocked the UI thread with an unbounded wait, and every product add replayed all products (N²). On failure it marked the UI thread as interrupted. |
| 6 | MAJOR | `DefaultProductLayer.java:486-490` | ENVISAT wave products never got their band-outline list set; selecting, the product panel, opacity and closing crashed on it. |
| 7 | MAJOR | `DefaultProductLayer.java:233-244` | Quicklook area built from two corner pixels with no ordering (mirrored images); products crossing 180° stretched around the globe. |
| 8 | MAJOR | `AppPanel.java:109-143` | The Analysis view made a blocking request to `http://www.nasa.network.com/elev` (not a NASA domain), no timeout, errors ignored, XML parser accepting external entities. |
| 9 | MAJOR | microwave-toolbox `ocn/OWI.java:219,227` | Wind directions averaged as plain numbers (350° and 10° → 180°); arrows pointed to where the wind comes from. |
| 10 | MAJOR | `OWI.java:93-97`, `RVL.java:114-130` | `findBands(...)[0]` before the null check (dead fallback, leaked renderables); RVL no-data trim dropped the last row and column. |
| 11 | MAJOR | `ocn/ColorGradient.java:93,149` | OCN surface sector from two diagonal corners (inverted on one axis); values at or below the colour-scale minimum fully transparent. |
| 12 | MAJOR | Product Library AOI code | 2D selection start not range-checked; 3D selection across 180° ~340° wide; footprints across 180° hit-tested and drawn wrongly; starting a 3D selection twice leaked a listener that swallowed every left-button release. |
| 13 | MAJOR | `WWWorldViewToolView`, `WWAnalysisToolView` | OpenGL and Swing construction off the UI thread; listeners and GL canvas never released when closed. |

Also found: Place Names force-enabled in all three globes while its server (`worldwind22.arc.nasa.gov`)
returns 404; Political Boundaries, the OSM layer and the Analysis view's WMS server list were dead. Blue
Marble, Landsat, elevation and the Bing proxy still respond, with no guarantee from NASA.

snap-worldwind had no tests; the OCN tests only checked that objects were constructed.

## 2. Versions (review findings)

- **WorldWind 2.2.1 is the latest NASA release** (June 2022). SNAP's jar is byte-identical to upstream and was
  built against JOGL 2.4.0-rc. NASA's `develop` branch has untagged fixes from 2023–24 (display-scaling/DPI
  fixes, JOGL 2.5, EGM2008 geoid offsets). The active community fork, **WorldWindEarth v2.3.1** (Nov 2024),
  packages those. It is not on Maven Central, so SNAP would have to build and host it, as it already does
  for 2.2.1.
- **JOGL can move past what WorldWind was built for.** The latest is **2.6.0** (Aug 2025), on Maven Central.
  A check of every JOGL class and method WorldWind 2.2.1 references found nothing missing in 2.6.0 (static
  check; needs smoke tests on Windows, Linux and macOS ARM).
- **The real problems were in `snap-worldwind/pom.xml`:** macOS profiles chosen by the *build* machine's OS;
  the mac-intel profile pulled 2.4.0 natives under 2.5.0 Java jars; the mac-arm profile named Linux natives;
  32-bit Linux natives pinned to 2.3.2; redundant explicit natives; `*-main` artifacts bundling Android and
  ARMv6 natives. Releases worked only because they were built on Windows.

## 3. Improvements (review findings)

**Quick wins using what WorldWind already has:**

1. Clean up the endpoints: delete the `nasa.network.com` call and the dead layers; ship a SNAP layer config
   with live sources.
2. Bundled offline place names and borders, from Natural Earth.
3. A live base map such as EOX Sentinel-2 cloudless (commercial use needs a licence) or NASA GIBS. WorldWind
   has no WMTS client, so this goes through WMS or the existing Mercator tile class.
4. Place the product image by its four corners instead of two, and let non-map-projected products use the
   reprojection path that already exists.
5. A scale bar, grid overlays (lat/lon, UTM, MGRS), a measure tool and a terrain profile.
6. Load KML, shapefile and GeoJSON files onto the globe, and re-enable the WMS browser.
7. Offline and cache settings, and a local or Copernicus DEM as the globe's terrain.
8. Product Library: polygon AOI drawing and footprints that hit-test correctly across 180°.

**New functionality worth building:**

1. **Full-resolution product display (L):** tiles reprojected through each product's geocoding, with colours
   matching SNAP's image view. Turns the globe from a footprint map into an analysis view.
2. **Two-way sync with SNAP image views (M):** cursor, pins and GCPs.
3. **Product vector data on the globe (M).**
4. **Sentinel-1 acquisition geometry (M):** swaths, bursts, orbit ground track and look direction.
5. **Time slider for stacks and InSAR time series (M–L).**
6. **Product Library coverage map (M):** footprints coloured by date or orbit, plus a coverage heatmap.

**Strategy:** WorldWind Java is in maintenance mode (NASA suspended the project in 2019) but works and needs
no browser engine. Do the cleanup and version fixes now. Build new features on a SNAP-side model of tiles,
geometry and time that does not depend on WorldWind, with thin WorldWind adapters on top. SNAP already has
three world-map implementations that such a model could serve, and it keeps a later move to a web globe
(e.g. CesiumJS) possible — not worth doing yet.

---

## 4. Status (2026-10-17)

### Part 1 — bug fixes: done

All 13 findings and the dead endpoints are fixed, with regression tests, after two adversarial review
rounds. Local commits on `worldwind-fixes`, **not pushed**:

| Repo | Commit | Content |
|---|---|---|
| snap-desktop | `54a440f53` | JOGL/GlueGen 2.6.0 and native packaging (§2) |
| snap-desktop | `f167ae7f7` | #1–#8, #12, #13, dead endpoints (nasa.network.com removed, place names added disabled, dead WMS panel removed); first snap-worldwind tests |
| microwave-toolbox | `62ff4cba4` | #9–#11 and the further OCN/ETAD findings of the review rounds |
| microwave-toolbox | `79b629dc3` | OCN band reads moved off the EDT |

Not yet done for part 1: running the fixed views in the SNAP application (only unit tests and the real IW/WV
OCN test products through the layer code), and a third review round on the last fixes.

### Part 2 — versions: partly done

| Item | Status |
|---|---|
| JOGL/GlueGen 2.6.0, one native per platform, profiles removed | **Done** (`54a440f53`) |
| `--add-opens` for JOGL 2.6.0 in the development launcher | **Done** (`54a440f53`) |
| Rendering smoke test on Windows x64 | **Done** |
| Smoke tests on Linux x64/aarch64 and macOS (universal natives) | **Open** — needs those machines |
| Move to WorldWindEarth 2.3.1 (DPI fixes, EGM2008) | **Open decision** — SNAP would have to build and host the jar; recommended once the multi-platform JOGL tests pass, mainly for the HiDPI fixes |

### Part 3 — improvements

Quick wins: planned in §5, not started (items 4 and 8 were partly done as bug fixes: four-corner quicklook
placement, antimeridian footprint hit-testing). New features 1–3: specified in §6, being implemented.
New features 4–6: not started.

---

## 5. Quick-wins plan

Order: cheapest, most visible first; each step is its own commit with tests where testable without OpenGL.
Effort: S = up to a day, M = a few days.

| Step | Quick win | What | How (WorldWind 2.2.1 classes, all verified present) | Effort |
|---|---|---|---|---|
| Q1 | Live base map (#3) | Add NASA GIBS (Blue Marble shaded relief, VIIRS true colour) and EOX Sentinel-2 cloudless as selectable base layers | `WMSTiledImageLayer` built from the GIBS `epsg4326/best` WMS and the EOX WMS (both checked live 2026-10-17); keep Bing as default until licences are settled. EOX s2cloudless 2016 is CC BY 4.0, later years CC BY-NC-SA — commercial users need a licence, so EOX layers are added disabled with the attribution in the layer name | S |
| Q2 | SNAP layer config (#1) | One SNAP-owned layer configuration instead of WorldWind's built-in defaults, so dead NASA endpoints cannot come back | A `config/snap-worldwind-layers.xml` in the module, set via `gov.nasa.worldwind.avkey.LayersConfigurationDocument` before the first `WorldWind.createConfigurationComponent`; lists only live layers | S |
| Q3 | Offline place names and borders (#2) | Country borders and populated places that work offline | Natural Earth (public domain) 1:50m admin-0 boundaries and populated places, bundled as shapefiles (~2 MB), loaded with `ShapefileLayerFactory`; replaces the disabled place-name layer | M |
| Q4 | Map tools (#5) | Scale bar, lat/lon/UTM/MGRS graticules, measure tool, terrain profile | `ScalebarLayer`, `LatLonGraticuleLayer`, `UTMGraticuleLayer`, `MGRSGraticuleLayer` (off by default, toggled in the layer panel), `MeasureTool` with a toolbar toggle in the Analysis view, `TerrainProfileLayer` | M |
| Q5 | External data (#6) | Open KML/KMZ, shapefile and GeoJSON files onto the globe; WMS browser | `KMLRoot` + `KMLController`, `ShapefileLayerFactory`, worldwindx `GeoJSONLoader`; worldwindx `WMSLayersPanel` behind an "Add WMS layer" button, with the server list in preferences (no dead defaults) | M |
| Q6 | Reprojection of non-map-projected quicklooks (#4) | Quicklooks for SAR/swath products | Superseded by feature F1 (§6.1), which displays any geocoded product through its geocoding; the quicklook path stays for map-projected products in the World View | — |
| Q7 | Offline/cache and terrain (#7) | Offline switch, cache location and size; local DEM as terrain | `WorldWind.setOfflineMode`, file-store location in preferences; `LocalElevationModel` fed from a SNAP DEM file (Copernicus 30 m tiles already downloaded by snap-dem), added to the globe's `CompoundElevationModel` | M |
| Q8 | Product Library polygon AOI (#8) | Draw a polygon instead of a rectangle | Extend `Rectangle3DSelection`/`Map2DMouseHandler` with a polygon mode; the search already accepts WKT | M |

Risks: Q1/Q3 add network or bundle-size dependencies (licence check for EOX; bundle size for Natural
Earth); Q4/Q5 widgets need GL rendering, so they are tested in the application, not in unit tests.

---

## 6. Feature specifications

The three features share the existing extension point: `WWLayer` implementations registered in
`layer.xml` and added to the views by `WWLayerRegistry`, fed by `WWBaseToolView` with products, the selected
product and the selected raster. They add one more input: the selected image view
(`ProductSceneView`), passed with a new default method `WWLayer.setSelectedSceneView(ProductSceneView)`.
All WorldWind and Swing access happens on the EDT; product data is read on background threads, as
`DefaultProductLayer` does since `f167ae7f7`.

Following the review's strategy, the geometry work (geocoding to lat/lon, tile resampling, vector data
conversion) lives in WorldWind-independent classes that can be unit tested without OpenGL; the layers are
thin adapters.

### 6.1 F1 — Full-resolution product display

**Goal.** Show the image of a product on the globe at full resolution, placed through the product's own
geocoding (any geocoding: map-projected, tie-point, pixel/swath geocodings of SAR and other L1 products),
with the colours of SNAP's image view.

**Behaviour.**

- New layer "Product Images" (`ProductImageLayer`), shown in the Analysis view (`showIn3DToolView`), not in
  the small World View.
- When an image view is selected, the layer shows that view's image for its product: the view's rasters
  (one band, or three for RGB) with the view's colour settings (`ImageInfo`). Each product keeps the image
  of its last selected view until another view of the same product is selected or the product is closed.
- Changing the colours in the Colour Manipulation tool (the view's `imageInfo` property) refreshes the
  image.
- Detail follows the zoom level: tiles are generated on demand from the image pyramid level that matches the
  tile's resolution, so zooming in shows full-resolution pixels and zooming out stays cheap.
- No-data and areas outside the product are transparent. The layer opacity applies (layer panel).
- While a product shows a full-resolution image, its quicklook (`DefaultProductLayer` surface image) is
  hidden, so the two do not blend.

**Design.**

- `ProductTileRenderer` (WorldWind-independent): given a geographic sector and a tile size in pixels,
  returns an ARGB `BufferedImage` or null if the tile does not touch the product.
  - Inverse geocoding on a coarse grid (one node every 16 tile pixels, 33 × 33 nodes for a 512 tile):
    `GeoCoding.getPixelPos` per node; pixel positions inside a grid cell are bilinearly interpolated when all
    four nodes are valid, and computed exactly per pixel where they are not (swath edges), so edges are not
    cut back to whole cells.
  - Pyramid level: the source pixels per tile pixel are estimated from the node spacing; level =
    floor(log2(ratio)), clamped to the pyramid's levels.
  - Source image: a `ColoredBandImageMultiLevelSource` built from the view's rasters and a copy of its
    `ImageInfo` — the same colouring code as the image view, independent of the view's lifetime. Only the
    bounding rectangle of the needed source pixels at that level is read; nearest-neighbour sampling.
  - Longitude handling: lat/lon of nodes are normalised; a sector crossing the antimeridian is never
    requested because WorldWind tiles never cross it.
  - The renderer is stateless apart from the image source, and thread-safe.
- `ProductImageTiledLayer extends TiledImageLayer` (adapter): a `LevelSet` over the product's bounding sector,
  512-pixel tiles on WorldWind's global tiling (36° level-zero tiles, origin -90/-180), as many levels as the
  product's ground resolution needs plus one. `requestTexture` queues a prioritised task (nearest tiles first);
  the task renders the tile, converts it to `TextureData`, puts the tile into WorldWind's texture-tile memory
  cache, and marks the tile absent if it does not touch the product. Each image gets a unique cache name, so
  a colour change never reuses old tiles.
- Product bounding sector: from the boundary paths already computed for the outline
  (`GeoUtils.createGeoBoundaryPaths`); products crossing the antimeridian get the full longitude range.
- `ProductImageLayer` (`WWLayer`): one `ProductImageTiledLayer` per product, rendered from its own
  `doRender`; listens to the selected view's `imageInfo` property; removes and disposes the image when the
  product is removed or its view's rasters are removed.

**Edge cases.** Products without geocoding, or whose geocoding cannot compute pixel positions
(`canGetPixelPos() == false`), get no image (logged). Multi-size products: the view's raster geocoding and
pyramid are used, not the scene's. Very large products: only visible tiles at the needed level are read.
Rasters disposed while a tile renders: the task fails quietly and the tile is retried on the next frame only
if the image still exists.

**Tests (no OpenGL).** Renderer on synthetic products with a CRS geocoding and with a tie-point (swath-like,
rotated) geocoding: pixel at a known lat/lon has the expected colour; north-up orientation not mirrored;
no-data and outside are transparent; pyramid level chosen for coarse tiles; tiles outside return null; edge
cells computed exactly. Level-set sizing from resolution. Layer: image replaced on view change, removed with
the product, refreshed on `imageInfo` change, quicklook hidden/shown.

**Out of scope.** Elevation-corrected placement (terrain-corrected display would need the DEM in the
inverse geocoding); bilinear resampling of the coloured image; showing images of products that have never
been opened in an image view.

### 6.2 F2 — Two-way sync with SNAP image views: cursor, pins and GCPs

**Goal.** Link the globe and the image views: see on the globe where the cursor is in an image view and
vice versa, and see, select and place pins and GCPs on the globe.

**Behaviour.**

- New layer "Pins and Cursor" (`ImageViewSyncLayer`), in both views.
- **View → globe cursor:** moving the mouse over an image view shows a crosshair marker on the globe at the
  geo-position under the mouse; it disappears when the mouse leaves the view.
- **Globe → view cursor:** moving the mouse over the globe shows a cursor overlay (crosshair) in every open
  image view whose product covers that position; it disappears when the mouse leaves the globe. Controlled by
  a "Sync cursor with image views" toggle in the Analysis view (on by default) — the World View only
  receives the view → globe cursor.
- **Pins and GCPs:** every product's pins (yellow) and GCPs (cyan) are drawn on the globe with their labels,
  and follow additions, removals, moves and renames.
- **Selecting:** clicking a pin or GCP on the globe selects it in the product's open image view.
- **Placing:** in the Analysis view, "Place pin" (right-click menu) adds a pin at the clicked position to the
  selected product, if the position is inside it.
- **Navigating:** double-clicking on the globe centres the selected image view on that position, if the
  product covers it.

**Design.**

- `ImageViewGeoUtils` (WorldWind-independent): level-pixel → geo-position for a view (image-to-model of the
  current level, model-to-image of level 0, raster geocoding — as SNAP's `ImageCursorSynchronizer` does);
  geo → raster pixel with an inside-raster test; placemark → geo-position (from the placemark's geo-position
  or, failing that, its pixel position through the geocoding).
- `ImageViewSyncLayer` (`WWLayer`): one `PointPlacemark` per pin/GCP keyed by `Placemark` identity; a
  `ProductNodeListener` per product on its pin and GCP groups; a `PixelPositionListener` on the selected
  view; the crosshair placemark.
- `GlobeCursorOverlay` (`LayerCanvas.Overlay`): draws the globe cursor in an image view at the geo-position
  converted to view coordinates; the overlays are added to the open `ProductSceneView`s (found through the
  window registry) and removed when the cursor leaves or the view closes.
- Globe mouse events come from the `WorldWindowGLCanvas` mouse listeners and `SelectListener`; positions from
  `getCurrentPosition()`.

**Edge cases.** Products without geocoding: no pins drawn (logged once), cursor ignored. Pins with invalid
geo-positions are skipped. Products crossing the antimeridian: geo-positions normalised. Views of a product
not shown on the globe still get the globe cursor. Listener leaks: all listeners removed on product removal
and view close.

**Tests (no OpenGL).** Geo/pixel conversions with levels; inside-raster test; pin add/remove/move/rename
updates the placemarks; GCPs and pins in different colours; product removal removes the placemarks and
listeners; pin placement adds a pin at the right pixel and rejects outside positions.

**Out of scope.** Syncing the view's zoom with the globe's altitude; drawing pins as editable on the globe
(drag to move).

### 6.3 F3 — Product vector data on the globe

**Goal.** Show the vector data of open products (shapefiles imported into the product, drawn geometries,
WKT, masks defined from vectors, ...) on the globe.

**Behaviour.**

- New layer "Vector Data" (`VectorDataLayer`), in both views.
- Every non-empty vector data node of every product is drawn, except the pin and GCP nodes (shown by F2):
  polygons as filled surface polygons with holes, lines as surface paths, points as placemarks.
- The colours and line widths come from the node's and features' SNAP style (fill colour, fill opacity,
  stroke colour, stroke width), so the globe matches the image view.
- The drawing follows features being added, changed or removed, and nodes being added or removed.
- Hovering a feature in the Analysis view shows the node name and feature id.

**Design.**

- `VectorDataConverter` (WorldWind-independent): converts a `VectorDataNode`'s features to lat/lon rings,
  lines and points. Feature geometries are in the product's model coordinates; they are converted to raster
  pixels with the image-to-model transform of the scene geocoding and then to geo-positions through the
  geocoding, so the conversion is right for map-projected and non-map-projected products alike. Lines and ring
  edges longer than a few pixels are densified so that they follow the geocoding of curved swaths. Also
  parses the style (`DefaultFigureStyle`).
- `VectorDataLayer` (`WWLayer`): converts on a background executor and installs `SurfacePolygon` /
  `SurfacePolyline` / `PointPlacemark` renderables on the EDT; a `ProductNodeListener` per product re-converts
  a changed node; keyed by `VectorDataNode` identity.

**Edge cases.** Products without geocoding: skipped. Features with null or empty geometry: skipped. Invalid
geo-positions (outside the geocoding): the part is split at invalid vertices rather than drawn to 0/0. Very
large nodes: capped at 100 000 vertices per node, with a warning, to keep the globe responsive.
Geometries crossing the antimeridian are passed to the surface shapes, which handle the dateline.

**Tests (no OpenGL).** Conversion for a CRS-geocoded and a tie-point-geocoded product: polygon with hole,
line, point, multi-geometries; densification; invalid vertices split the part; style parsing; layer updates
on feature change, node add/remove, product removal; pins and GCPs not drawn.

**Out of scope.** Editing vector data on the globe; 3D (extruded) vector data.

---

## 7. Implementation notes

### 7a. First implementation (2026-10-17)

Shared:

- **Addition — layer order.** `WWLayerRegistry` kept its descriptors in a `HashMap`, which discards the folder
  order of `layer.xml`, so the drawing order of the layers was arbitrary. It is now a `LinkedHashMap`, and
  `layer.xml` gives positions: Product Images (100) below the outlines (200), Vector Data (300) and Pins and
  Cursor (400) on top.
- **Addition — module dependency.** snap-worldwind now declares `ceres-jai` (image pyramids); without it the
  NetBeans module would not see `MultiLevelSource` at runtime (the build reported "uses classes from transitive
  module").
- WorldWind-independent code is in `org.esa.snap.worldwind.model` (`ProductTileRenderer`, `GeoBounds`,
  `ImageViewGeoUtils`, `VectorDataConverter`), as §6 intended.

F1 — full-resolution display:

- **Clarification — resolution.** The ground resolution that sizes the pyramid is measured as a great-circle
  arc, not in degrees of longitude (0.01° of longitude at 50.5° N is 0.0064° of arc); caught by
  `ProductTileRendererTest.testGeoBounds` failing against the first, longitude-based expectation.
- **Addition — opacity.** The Analysis view gives the Product Images layer opacity 1.0 (the product layer keeps
  0.8 for outlines and quicklooks); the layer panel can still change it.
- **Note — magnification.** The pyramid has one level finer than the product's pixels; zooming in further
  magnifies that level with WorldWind's linear texture filtering, so pixel edges are slightly soft rather than
  the nearest-neighbour blocks of SNAP's image view.
- **Verified on a real globe** (JOGL 2.6.0, Windows x64): a map-projected product north-up with its palette
  and a 30°-rotated tie-point swath placed through its geo-coding, both aligned with their outlines; a zoomed
  view at 15 km shows the expected pixel corner at the expected lat/lon.

F2 — sync with image views:

- **Deviation — centring the image view.** §6.2 specified a double-click on the globe. WorldWind's view input
  handler uses mouse clicks for navigation, so centring is instead an item of the globe's context menu
  ("Centre Image View Here"), next to "Place Pin Here". Both items are disabled when the position is outside the
  selected product / image view.
- **Addition — round-trip check.** A geo-position is only accepted as inside a raster if the pixel found for it
  maps back to the position (within a degree), because tie-point geo-codings extrapolate pixel positions far
  outside the swath.

F3 — vector data:

- **Addition — CRS.** Features whose CRS differs from the scene's model CRS are first transformed to the model
  CRS with GeoTools.
- Hover info is a screen annotation next to the mouse, shown on rollover in the Analysis view.

### 7b. Quick wins (2026-10-19)

Q2 — SNAP layer configuration:

- **Correction — configuration key.** §5 named a `gov.nasa.worldwind.avkey.LayersConfigurationDocument` key; WorldWind
  2.2.1 has no such key (`BasicModel` reads only `LayerClassNames` or the `./LayerList` element of the global
  configuration). Instead the views build their model as `new BasicModel(globe, LayerConfiguration.createLayers())`
  from `org/esa/snap/worldwind/config/snap-worldwind-layers.xml`; WorldWind's global configuration is untouched.
- **Endpoints checked 2026-10-19** with GetMap requests: worldwind25 (Blue Marble, i-cubed Landsat, Earth at Night),
  worldwind27 (Bing) and worldwind26 (elevations) answer; worldwind22 (boundaries, place names), worldwind20 (OSM) and
  the USGS NAIP server do not, and are not configured. A test fails if one of the dead hosts comes back.
- **Addition — no capabilities downloads.** All layers set `RetrievePropertiesFromService` to false with their level
  sets in the file.
- **Addition — layer order.** SNAP's layers used to be inserted before the NASA place name layer, which no longer
  exists; they are now inserted below the overlays (world map, scale bar, compass, layer panel, place names), marked
  with `LayerConfiguration.OVERLAY`.

Q1 — live base maps:

- Added, all disabled by default (Bing stays the default): NASA GIBS Blue Marble Shaded Relief + Bathymetry, NASA
  GIBS VIIRS true colour, EOX Sentinel-2 cloudless 2016 (CC BY 4.0) and **2025** (CC BY-NC-SA 4.0; §5 did not name a
  year, 2025 is the latest EOX publishes). The licence is part of each EOX layer name. Verified on a globe: VIIRS over
  Europe, EOX at 5 km scale in the Alps, GIBS relief over the Himalaya.
- **Addition — daily imagery.** The VIIRS layer shows the last complete UTC day (`TIME` parameter in the GetMap URL,
  date in the layer name and cache name), because the current day is still being filled in and a fixed cache name
  would keep showing the first day ever downloaded.

Q3 — offline borders and place names:

- Natural Earth 1:50m boundary lines (v5.1.0, shapefile with the attribute table cut to `FEATURECLA`, 360 KB) and
  populated places (v5.1.2, 1251 places as tab-separated text, 51 KB) under `org/esa/snap/worldwind/naturalearth`,
  with a README giving the sources. Both layers are on by default, as overlays, in all three globes.
- **Deviation — place names are not a shapefile layer.** `ShapefileLayerFactory` turns points into placemarks without
  any choice by zoom level, so the places are a small text file and `NaturalEarthPlaceNameLayer` labels them with
  WorldWind's `GeographicTextRenderer` (overlap culling by priority: capitals, then population). A place is labelled
  below the eye altitude 10⁸ m / 2^`min_zoom`, from Natural Earth's recommended web-map zoom. The selection is in
  the WorldWind-independent `model.PlaceNames`.
- **Addition — disputed lines distinguished.** Boundaries whose feature class is not "International boundary"
  (disputed, line of control, indefinite) are drawn in orange instead of the pale yellow of recognised boundaries.
  *Correction (review, 2026-10-19):* this note first said "dashed"; the stipple pattern was set but WorldWind's
  `ShapefilePolylines` honours only colour and width (no stipple, no opacity), so the lines were drawn as recognised
  boundaries. Checked on a globe over Kashmir.
- `FixingPlaceNameLayer` (renamed one sea in the dead NASA layer, SNAP-1476) is removed with that layer; Natural
  Earth's populated places contain no sea names.

Q4 — map tools:

- Scale bar: already in the layer list, now shown in the layer panel so it can be switched off. Lat/lon, UTM and
  MGRS grids are in the layer configuration, off by default, drawn as overlays.
- **Deviation — measure tool in the side panel, not a toolbar.** The Analysis view has no toolbar; a "Measure" box
  under the product list holds the shape (line, path, polygon, circle), Measure/Clear buttons, the result (length,
  or area and perimeter) and a "Terrain profile" switch.
- **Addition — profile along the measured line.** The terrain profile follows the measured line or path once it has
  two points, and the cursor otherwise. The graph is placed bottom right, above the scale bar, because the layer list
  occupies the bottom left.
- Verified on a globe from the GL framebuffer: UTM grid with labels, a measured line across the Alps and its profile
  (926–2447 m). Desktop screen captures of the test window came out blank while it was covered on the screen, so
  the smoke test reads the framebuffer instead.

Q5 — external data:

- "External Data" box in the Analysis view: **Add File...** reads KML/KMZ (`KMLRoot` + `KMLController`), shapefiles
  (`ShapefileLayerFactory`, waited for so that errors are reported) and GeoJSON (worldwindx `GeoJSONLoader`) off the
  EDT into a layer named after the file; **Remove** takes added layers off the globe. Reading is in
  `layers.DataFileLayers`, tested on real KML, GeoJSON and shapefile files and on broken ones.
- **Addition — GeoJSON errors.** `GeoJSONLoader` returns an empty layer for JSON that is not GeoJSON; that is
  reported as an error instead of adding an invisible layer.
- **Add WMS...** opens WorldWind's `WMSLayersPanel` per server, in a subclass: the original inserts layers before
  the place name layer, which no longer exists, i.e. at the bottom under the base maps. Servers come from the
  preferences (Options > General > World View), default GIBS and EOX; a server typed into the dialog is added.
- **Fix — layer panel.** WorldWind's `LayerManagerLayer` refreshed its list only on mouse-over, so layers added by
  code (products, data files, WMS layers) were missing from it; `LayerPanelLayer` now refreshes on every change of
  the layer list.
- Verified on a globe: a KML polygon, a GeoJSON line and the EOX hydrography WMS layer, all listed in the layer
  panel and drawn above the base maps.

Q7 — offline, cache and terrain:

- Options > General > World View: **Work offline** (`WorldWind.setOfflineMode`, applied at start-up and on change);
  **tile cache folder** (WorldWind's `gov.nasa.worldwind.platform.alluser.store` system property, set by
  `WWActivator` before WorldWind creates its file store, so a change needs a restart); **local DEM terrain** (folder,
  default SNAP's `auxdata/dem`).
- **Deviation — cache size.** WorldWind has no size limit for its tile cache, so instead of a size setting the
  panel shows the cache's location and size with a **Clear Cache** button (asks first; refuses any folder not named
  `WorldWindData`).
- **Deviation — terrain is read with SNAP, not `LocalElevationModel.addElevations(File)`.** WorldWind's own GeoTIFF
  reader does not decode the DEFLATE-compressed Copernicus tiles. The DEM GeoTIFFs of the folder (up to 64) are read
  with SNAP's readers in the background, sampled to at most 1024 × 1024 (about 100 m for a 1° 30 m tile), and added
  as one `LocalElevationModel` to the globes' `CompoundElevationModel`, so WorldWind's elevations remain elsewhere.
  Only regular lat/lon grids are used (`CrsGeoCoding` on a geographic CRS without rotation; tie-point geo-codings
  report a geographic CRS too and are rejected). The grid model is the WorldWind-independent `model.DemGrid`.
- **Addition — Copernicus COG tiles.** SNAP's GeoTIFF reader plug-in declines these tiles in its qualification
  (`ProductIO.readProduct` returns null) although it reads them, as SNAP's DEM module does; the terrain loader falls
  back to the GeoTIFF reader directly.
- Checked headless on the real Copernicus 90 m tile N49 E010: heights 408 m at Ansbach (map 409 m) and 680 m at the
  Hesselberg (summit 689 m, smoothed by the ~100 m sampling); about 2 s per tile after start-up, mostly DEFLATE
  decoding.
- **Fix found on the globe (2026-10-19) — local DEMs take precedence.** Added to the globe's own
  `CompoundElevationModel`, the local DEM was never used: WorldWind sorts the models by resolution and the finest
  wins, and WorldWind's 30 m elevations are finer than the ~100 m sampling (the globe still showed 568 m at the
  Hesselberg). The globe's elevations are now wrapped in a compound model that always sorts local DEMs last, so they
  win where they have data and WorldWind's elevations fill in elsewhere; removing the terrain restores the original
  model. Checked on a globe: heights inside the Copernicus tile equal the tile's, outside them WorldWind's, with ~3 m
  difference across the tile edge.

Q8 — Product Library polygon AOI:

- **Correction — the search does not accept WKT.** §5 assumed it did; the area of interest is a `Rectangle2D` all the
  way down: the remote query (`RemoteRepositoriesManager`, TAO polygon of the four corners) and the local database
  (`DataAccess` → `GeometryUtils.buildPolygon`) both build the polygon from the rectangle. Q8 therefore also changes
  snap-engine (branch `worldwind-fixes` there).
- **Design — `PolygonArea extends Rectangle2D.Double`** (snap-remote-products-repository, geometry package): the
  polygon's vertices, and as a rectangle its bounding box. The parameter type stays `Rectangle2D`, so the product
  library UI needed no change; `GeometryUtils.buildPolygon`/`buildPath` and the remote query use the vertices, and
  any code that only knows rectangles searches the bounding box, which contains the polygon.
- Drawing: right-click menu "Start polygon selection (double-click to close)" on the 2D map and the 3D globe; each
  click adds a vertex, the outline follows the mouse, a double click closes it. Drawing clicks are consumed so that
  they do not also select footprints. Fewer than three vertices or collinear vertices do not close the polygon.
- Tests: polygon area, database WKT and remote query polygon (snap-engine), 2D and 3D drawing with simulated mouse
  events (snap-worldwind). Checked on screen (2026-10-19) with mouse events dispatched to the Product Library's own
  globe and map panels: a 4-vertex polygon drawn on the globe over the Alps, handed over to the 2D map in the same
  place, and a new polygon drawn on the 2D map. Polygons crossing the antimeridian are not supported.

### 7c. Review fixes (2026-10-19)

An adversarial review (five reviewers over F1–F3 and Q1–Q8) found the problems below; each was checked in the code
before fixing.

Q7 — local DEM terrain:

- The globes are redrawn when the terrain arrives (before, it appeared only at the next interaction).
- A reload interrupts a load still running for the previous settings; the loader stops between files.
- A folder already read is not read again for every new view, also when it gave no terrain.
- The 64-file cap counts usable DEMs, not every GeoTIFF found; files the GeoTIFF fallback fails on have their reader
  closed (Windows file locks); heights are kept in heap buffers instead of direct memory.
- Grids: east-to-west grids are flipped; the no-data value is compared as a float (a value such as 0.1 was missed);
  grids without any data are not used.
- Options: the default DEM folder and WMS server list are no longer written into the preferences on save, so later
  defaults reach the user; an unusable tile cache folder makes the options invalid instead of WorldWind silently
  using another folder.
- New tests: the tessellation path (`getElevations`) uses the local DEM, interrupted and unusable loads.
