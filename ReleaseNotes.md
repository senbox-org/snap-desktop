Release Notes - Sentinel Application Platform
=============================================

# New in SNAP 13.0.0
    [SNAP-1517] Empty results when using only Write operator with gpt
    [SNAP-3641] Bug when writing flags to DIMAP, NetCDF and other formats
    [SNAP-3697] Signed 8-Bit GeoTiff not read correctly
    [SNAP-3762] World Map Layer
    [SNAP-3765] Create Filtered Band Error
    [SNAP-3841] Clumped UI elements of contextual dialogs on Product Explorer tab
    [SNAP-3860] NullPointerException on ProductSceneViewTopComponent
    [SNAP-3888] Specific TIF files read differently (wrong?) than in other tools
    [SNAP-3907] Statistics - Negative pixel count for very large image
    [SNAP-3916] Graph Builder - File name is extended with file extension several times for ZNAP format
    [SNAP-3950] Project creation and XML file extension
    [SNAP-3949] Creation of project structure when opening non-existent project
    [SNAP-3948] Projects menu items enabled by default
    [SNAP-3969] Prevent fillValue zarr chunks
    [SNAP-3978] OLCI L1 - correct FWHM, frame_offset, lambba0 and solar_flux mapping
    [SNAP-4008] Land-Sea-Mask Op does not correctly persist the source bands selection
    [SNAP-4019] Improve GraphBuilder validation handling
    [SNAP-4048] Add cyclic color assignment for placemarks
    [SNAP-4054] Tile size not properly intialized in NetCDF reader for certain products
    [SNAP-4055] L3 Binning - Issues adding directories as input source

# New in SNAP 12.0.0
    [SNAP-3962] Colorpalettes lose label when persisted
    [SNAP-3961] Colortable label problems
    [SNAP-3953] Spectrum not working correctly for SYN data
    [SNAP-3946] Exception when adding WKT to a product without GeoCoding
    [SNAP-3945] SeaDAS File Reader fails in GPT processing mode
    [SNAP-3929] Sentinel 2 Mosaicking - NullPointerException
    [SNAP-3914] pconvert not writing RGB images from PACE OCI L2 AOP products
    [SNAP-3897] Refactor glayer module
    [SNAP-3893] NullPointerException in Band Groups Manager
    [SNAP-3886] Testfile Merra not correctly flipped
    [SNAP-3885] Bugs when reading CCI products
    [SNAP-3872] Exporting as ZNAP fails when paths have spaces
    [SNAP-3869] NPE in BandGroupsManager
    [SNAP-3843] ZNAP reader not releasing file handles properly
    [SNAP-3839] Re-organise import menu
    [SNAP-3636] ZNAP product not opened after processing
    [SNAP-3825] Scaling is not applied to coordinates   
    [SNAP-3764] Integrate BandGroups in PinManager
    [SNAP-3758] Error subsetting Sentinel-1 image
    [SNAP-3736] Ensure NetCDF compatibility with ESA/Copernicus data
    [SNAP-3730] Update to use GraalVM
    [SNAP-3729] Update to JDK 21
    [SNAP-3674] Handle tie-points grids smaller 2x2
    [SNAP-3644] Wrongly listed services
    [SNAP-3597] Titled border "Source product" of the JPanel of the Read operator has wrong dimensions
    [SNAP-3592] java.lang.NoClassDefFoundError on dialogs help
    [SNAP-3514] Problem reading SST-CCI Level 4 data

# New in SNAP 11.0.1
    [SNAP-3894] Issue with Compute Slope and Aspect tool - user complaint
    [SNAP-3893] NullPointerException in Band Groups Manager
    [SNAP-3892] New band group not correctly considered for band subset

# New in SNAP 11.0.0
    [SNAP-3709] Update GUI - use of band groups
    [SNAP-3702] Introduce band groups
    [SNAP-3684] Support PACE OCI L1C data
    [SNAP-3683] Support PACE OCI L1B data
    [SNAP-3680] Update MERIS auxdata module
    [SNAP-3666] S3 data cannot be opened as zip
    [SNAP-3658] S3 FRP not opening
    [SNAP-3636] ZNAP product not opened after processing
    [SNAP-3445] Support PRISMA Data
    [SNAP-1691] Improve Presentation of FRP data
    [SNAP-1684] Support SL FRP data of processing baseline FRP_NTC.004.07.00
    [SNAP-1585] GeoCodingFactory should be updated or removed
    [SNAP-1157] Let Product implement Closeable
	[SNAP-3718] Landcover UI should use a tree rather than a long list
	[SRM-112]	Support STAC client
	[SRM-186]	Seamless automated access to the ESA WorldCover 10m dataset

## New in SNAP 10.0

    Update SNAP to run with JDK 11
    Update 3rd party libraries to the latest version
    Update NetBeans platform
    Update SNAPPY to run with Python 3.10 & improved SNAPPY installation
    New browsing, voting, and commenting functionalities added to SNAP Plugin Dialog
    Integrate Copernicus DataSpace in SNAP Product Library
    Update Open-JPEG, NetCDF, and GDAL libraries to run on AARCH-64 MacOs

## List of solved issues

### Task
    [SNAP-1581] Apply fixes for XStream security issues
    [SNAP-3538] Integration of Online Help in SNAP

### Sub-task
    [SNAP-3470] Extract doc-window and tiled-editors modules from snap-desktop and use it as library
    [SNAP-3472] Consolidate JNI implementations
    [SNAP-3481] Remove jdom1 library and replace the usage with jdom2

#### Bug
	[SNAP-653] Update ImageIO-Ext
	[SNAP-936] snappy can't be used along with dask
	[SNAP-1432] Help is missing for Land-Cover-Mask operator
	[SNAP-1571] Application menu entries with MacOS Ventura not shown
	[SNAP-1572] Help for CSV vector data import is not clear about the separator char
	[SNAP-1601] Custom SNAP Plugin Dialog
	[SNAP-1619] Sentinel-3 WST products have issues with geolocation since PUG v3.35
	[SNAP-1639] Required dependency not declared
	[SNAP-2065] S2 Resampling Error "Failed to Read Data for level 0 and rectangle"
    [SNAP-2115] S2Resampling can't handle S2 data other format as SAFE
	[SNAP-3446] Get rid of duplicated library of jai-core
    [SNAP-3471] Copernicus DEM complications when coregistering S1
	[SNAP-3487] Space in path to zipped ZNAP product leads to IllegalArgumentException
	[SNAP-3498] Help files don't open on dev build
	[SNAP-3501] Resampling losing product description
	[SNAP-3505] Move GDAL reader & writer into SNAP
	[SNAP-3507] Move general-purpose implementations from opttbx-commons to snap
    [SNAP-3518] Datum not transfered from GCPGeoCoding
	[SNAP-3521] Labels are not shown in WorldWind View
	[SNAP-3523] Retrieving the installed GDAL versions not work on some Docker containers
	[SNAP-3526] S2 cannot be opened from installed SNAP 10 - "GDAL NOT initialised!" error
	[SNAP-3527] GDAL Internal distribution install on Mac failure, due to hash missmatch
	[SNAP-3530] Wrong unit requested
	[SNAP-3532] SNAP crashes with SIGKILL when opening S2 product on Mac platform
	[SNAP-3533] PolarView animation not stopped on product close
	[SNAP-3534] Product Library issues on SNAP10
    [SNAP-3549] Product Library - CDAS: "Bad request" when searching Sentinel-2 products by 'Product Name'
	[SNAP-3576] AddElevation does not report error on doExecute
    [SNAP-3595] No help for the operator 'Multi-size Mosaic'
    [SNAP-3600] Help content is either missing, non-existent, out-of-date, or with errors
    [SNAP-3601] Sentinel-5 nc files load problem in SNAP
	[SNAP-3605] IllegalStateException while creating HSV image
    [SNAP-3626] GPT generates two outputs
    [SNAP-3640] Sentinel-5 nc Geocoding not loaded
    [SNAP-3660] CollocationOp parameter renamed

### Improvement
    [SNAP-626] Make snappy compatible with Python 3.7
    [SNAP-709] All plugins are marked as 'Community Contributed Plugin'
    [SNAP-760] Help is not in line with Convert-Datatype operator
    [SNAP-843] Documentation for GLCM should be enhanced
    [SNAP-858] Documentation of Terrain Mask should be improved
    [SNAP-953] ENL and Coef Variation not explained in help
    [SNAP-1043] Improved snappy installation
    [SNAP-1579] The two green dudes in the Plugin Manager shall be changed or removed
    [SNAP-1591] Update Product Library for CDAS
    [SNAP-3440] Consider to compile the internal GDAL distribution also for AARCH64-bit platform
    [SNAP-3517] Increment the TAO version used on SNAP Product Library
    [SNAP-3541] Compile the Open-JPEG native libraries for AARCH64-bit platform
    [SNAP-3567] Update GDAL to version 3.7.2
    [SNAP-3568] Enable SNAP Community Plugins Update Center by default in SNAP
    [SNAP-3584] NetCDF reader geocoding read performance
    [SNAP-3587] Update SNAP AboutBoxes links to point to the Release Notes pages hosted on the STEP website
    [SNAP-3591] SNAP removes Ubuntu's snap directories
    [SNAP-3597] Titled border "Source product" of the JPanel of the Read operator has wrong dimensions
    [SNAP-3612] Product library - remove Scihub

### Requirement
    [SNAP-937] Consider renaming of snappy

# New in SNAP 9.0.6
    [SNAP-3473] Improve behaviour of output parameters dialog in Reprojection tool
    [SNAP-3479] Exception in decode qualification of ZnapProductReader
    [SNAP-3482] GDAL reader opens too many file handles
    [SNAP-3488] Error opening any S2 product (which contains JP2 files) after installing the updates (9.0.5) and GDAL is not installed on the system
    [SNAP-3489] Filtered images yield wrong result
    [SNAP-3493] SampleCoding of bands can get lost during collocation

# New in SNAP 9.0.5
    [SNAP-793]  Links to external pages in classifier help don't work
	[SNAP-1584] Graph Builder - Windowed reading - Read operator generates both pixelRegion and geometryRegion in graph xml
	[SNAP-3436] GPT hangs when running a S2 processing graph
	[SNAP-3458] Update SNAP & toolboxes AboutBox with proper release notes links
	[SNAP-3459] JP2OpenJPEG EPSG:4326 - axis order different depending OpenJPEG version
	[SNAP-3467] The vector import tools are sometimes disabled in the menu

# New in SNAP 9.0.4
    [SNAP-1573] NetCDF expects distinct names within metadata groups    
	[SNAP-1576] SRTM 1S HGT has incorrect nodata value

# New in SNAP 9.0.3
    [SNAP-1570] Shapefile import should be more lenient    
    [SNAP-1539] If errors in shapefile are detected, exception should be thrown

# New in SNAP 9.0.2
    [SNAP-1568] Metadata of product is not written to NetCDF-CF format    
    [SNAP-1562] Collocation operator should copy metadata of secondary products
    [SNAP-1561] Connection to WMS service cannot be established 
    [SNAP-1560] Memory leak in ceres-binio
    [SNAP-1552] Improve SNAP startup time by initialising Product Library on first usage
    [SNAP-1234] Image-Filter operator consumes to much memory
    [SNAP-254]  Align FilterOperator with Image Filters from GUI

# New in SNAP 9.0.1

    [SNAP-1551] Improve binning documentation about spatial data-day
    [SNAP-1549] Evaluation of implemented methods happens too late for Python operators
    [SNAP-1545] Flags might be wrong when reading from GeoTiff
    [SNAP-1533] Bing Aerial layers not working in WorlWind view 

# New in SNAP 9.0.0

The SNAP 9 release provides several new tools, features, and bug fixes to the users. The noteworthy news is highlighted
in the following sections. For the full list of changes check our issue tracker: https://bit.ly/snap9_changelog

### New ZNAP Data Format.

With SNAP 8, we provided a first look on the newly developed data format. Now we are considering it as feature complete.
This new format shall replace the BEAM-DIMAP format as SNAPs default format. For now BEAM-DIMAP is still the default.
The ZNAP format has some benefits. Smaller footprint on disk, faster writing, and it uses a single file or directory for
storing the data. As format for the binary data Zarr is used by default. This allows Python developer to read the data
easily with Xarray, for example. Even binary data format can be configured and GeoTiff or NetCDF can be used to store
the binary data.

### Improved Colour Manipulation Tool

Our friends from the NASA OBPG who develop SeaDAS, have improved the Colour Manipulation Tool. Some points which have
been enhanced are:

* Auto-applied color schemes based on a band-name lookup (configurable in preferences)
* Palette reversal capability
* Color Manipulation preferences page
* Some new RGB profiles and Colour Palettes
* New auto-range adjustment buttons

### Copernicus 90m and 30m DEM

The new Copernicus DEM replaces the SRTM as the default free and open Digital Elevation Model.

## Sentinel-1 Toolbox

The Sentinel-1 Toolbox continues to support most SAR missions with updates to the Sentinel-1 format and support for
Cosmo-Skymed SG, Gaofen-3 and Spacety. ARD functionality has been enhanced with the addition of a Noise Power Image and
Gamma-to-Sigma ratio image in Terrain Flattening and the estimation of noise equivalent beta0, sigma0 and gamma0 in
Thermal Noise Removal. InSAR functionality now includes ionospheric estimation and correction using a splitbandwidth
approach and retrieval of Vertical and E W motion components from a pair of interferograms. Polarimetric processing now
includes Kennaugh Matrix, and Huynen, Krogager, Cameron, Yang decompositions as well as Radar Vegetation Indices.

## Sentinel-2 Toolbox

The GDAL version is updated from 3.0.0 to 3.2.1. Also, a plugin for COG writer is added. Corrections made for visual
artifacts noticed in produced images when writing in parallel, from multiple threads. Allow setting which installed GDAL
distribution is used by SNAP (when the user desires to use and installed distribution instead of the internal one, and
there are several GDAL versions installed). Solved operators whose UI was not well functioning in Graph Builder. Updates
made for operators: Reflectance to Radiance, Spectral Angle Mapper, Multi-size Mosaic, GeFolki Co-registration. Windowed
Reading of Products available now in Graph Builder. It allows specifying in Graph Builder, for read operation, a
window (spatial subset) of either pixel coordinates or geographical coordinates, so that, the reader will read directly
the region of interest as a full-fledged product. New plugin adapter for MAJA 4.5.3 and Sen2Cor 2.9 are added in SNAP
menu. A new change vector analysis operator allows to compare two geolocalized images at different dates. Support for the third party sentinel-2 Products: Landsat processed by ESA, Landsat L2 (L2H, L2F, L8H, L8F, L9H, L9F) is available.


## Sentinel-3 Toolbox

The Sentinel-3 Toolbox offers some new operators which are mainly dedicated for the preprocessing of Sentinel-3 data.
There is the OLCI Anomaly Detection operator which can detect and flag anomalous pixels, an operator which harmonises
the data of OLCI A and B and an operator which can perform Dark Object Subtraction (DOS) on optical data.  
For OLCI L2 Land and Water data the Quality Working Group (QWG) have defined recommended flags. They are now
incorporated in the products as masks and should be considered when using the data. In addition, an operator is now
available which makes MERIS 4th reprocessing data compatible with already existing operators. This allows to use the new
data set with older operators. In SNAP8 the pixel-based geo-coding has been reimplemented, in this release further
improvements have been achieved. Now the Sentinel-3 data uses the pixel-based geo-coding by default and not the
tie-points anymore. Support for Landsat Collection-2 has been implemented. Supported are Level-1 and Level-2 data.

### Resolved Issues

    [SNAP-1520] Add file association for znap format as option to installer
    [SNAP-1514] Remote Execution operator - slave graph does not execute on remote linux machine
    [SNAP-1508] Race condition when creating directory in Getasse30 reader
    [SNAP-1503] Displayed name in Product Explorer is not updated if wavelength is changed
    [SNAP-1499] Prevent adding masks with same name as an existing node
    [SNAP-1498] ProductIO does not rewrite header if product has changed during writing
    [SNAP-1497] ProductNodeListener could be null
    [SNAP-1494] Older "Slice" products (SENTINEL_SAFE format) not able to be opened in SNAP 8.0
    [SNAP-1485] Provided geoRegion is not considered in subsetting graph if quoted
    [SNAP-1482] ZipUtils does not handle null on root path
    [SNAP-1481] Remove old reference to SeaDAS help
    [SNAP-1477] Change label to 'Sea of Japan/East Sea
    [SNAP-1476] Label for the sea between Korea and japan should be changed
    [SNAP-1474] Consider write option for ADS when writing NetCDF4-BEAM
    [SNAP-1461] Subset operator: computed spatial region are different for the same original resolution
    [SNAP-1454] DEM download certificateexception: no subject alternative dns name
    [SNAP-1450] Masks of type Geometry not stored in BEAM-DIMAP
    [SNAP-1429] Writing is getting slow after some time for huge data products
    [SNAP-1428] The homepage URL is wrongly generated
    [SNAP-1427] JP2 reader - inconsistency between Int16 and UInt16
    [SNAP-1419] Subset fails for very small areas
    [SNAP-1414] Scatter plot fails if bands use different tiling
    [SNAP-1406] Reading Merra data from NetCDF fails
    [SNAP-1387] Missing pixels after reprojection when using pixel-based geo-coding
    [SNAP-1383] SRTM download URL not working
    [SNAP-1382] Missing tiles in image view
    [SNAP-1379] Opening BigGeoTiff file results in ArrayIndexOutOfBoundsException
    [SNAP-1377] Discontinuity information is not correctly stored in DIMAP
    [SNAP-1374] NullPointerException is thrown in Mosaic GUI
    [SNAP-1370] If multiple products shall be closed only one is closed
    [SNAP-1369] Vector data cannot be restored from BEAM-DIMAP
    [SNAP-1367] Azul JDK does not include fonts
    [SNAP-1365] In Reprojection always nearest neighbour resampling is used
    [SNAP-1358] Data not correctly written to DIMAP
    [SNAP-1355] In certain edge cases the WriteOp writes data wrong
    [SNAP-1331] Coordinates are not imported correctly
    [SNAP-1301] Error creating product subset using Subset operator
    [SNAP-1267] Super-sampling has no effect in binning
    [SNAP-1167] Geotiff writer geocoding is using wrong geocoding in some cases
    [SNAP-1183] Export of transect pixels does not show correct values
    [SNAP-1072] Update SVVP wiki page
    [SNAP-1057] Make land/sea mask compatible with multi-size products
    [SNAP-611] S2 Masks can't be used in Band Math in Graph Builder
    [SNAP-1496] Product Library - improvements
    [SNAP-1495] Improve error message of Product Library
    [SNAP-1489] Update snap jp2 reader for more 4 bands
    [SNAP-1464] Set only GCP and Pin layer to visible by default
    [SNAP-1462] Improve JAI tile cache handling when writing data
    [SNAP-1456] Support ASTER v3
    [SNAP-1449] Allow whitespaces for array parameter values
    [SNAP-1444] Improve memory consumption by GraphProcessor
    [SNAP-1443] Geocoding interpolating property should be taken into account in persistables
    [SNAP-1438] Improvement of Colour Manipulation Tool
    [SNAP-1437] Geo-coding information not displayed when product uses band-wise geo-coding
    [SNAP-1436] JP2 reader - use shorter file names for decompressed tiles
    [SNAP-1435] Product Library - Scientific Data Hub : API URL changed
    [SNAP-1434] URLs to the updatecenter to use HTTPS
    [SNAP-1411] Product Library - Scientific Data Hub : Too Many Requests
    [SNAP-1410] Product Library - USGS : response format changed
    [SNAP-1405] Product Library - Amazon Web Services - S2 "requester pays" bucket
    [SNAP-1401] Enable lookup tables with decreasing coordinate axis values
    [SNAP-1400] JP2 reader - GML metadata for EPSG:4326
    [SNAP-1385] Product Library - Alaska Satellite Facility : add relative orbit number in search results for SAR sensors
    [SNAP-1384] Product Library - Scientific Data Hub : add relative orbit number in search results for SAR sensors
    [SNAP-1376] Use lazy loading for tie-points in DIMAP
    [SNAP-1359] Product Library database should allow multiple connections
    [SNAP-1357] BMP should not be the default format when exporting images
    [SNAP-1354] The supported Python versions should be updated in the installer
    [SNAP-1315] All help material shall be revised
    [SNAP-1194] Provide a SNAP Core only installer and server only installers
    [SNAP-1193] Service loading should be done in background task
    [SNAP-1091] Update Install4J version
    [SNAP-1029] Improve visual analysis of the images
    [SNAP-1028] Improve status bar
    [SNAP-981] Improve resampling operator
    [SNAP-980] Improve the integration and generalisation of the existing Mosaicking operators
    [SNAP-979] Make the "Transfer Mask" action compatible with multi-size products.
    [SNAP-970] Make the Binning operator compatible with multi-size products.
    [SNAP-758] Clean up operator descriptions
    [SNAP-613] Support "Use CRS of" in Reprojection operator in Graph Builder
    [SNAP-1386] Mean aggregator for binning operator
    [SNAP-1381] Improve Graph Editor in a new Graph Builder extension
    [SNAP-1031] Implement tools for change detection
    [SNAP-520] RGB profile shall support min and max value for each channel
    [SNAP-1442] Evaluate effort to replace ZipFileSystem in Alos2GeoTiffProductReaderPlugIn
    [SNAP-1215] Write a System Design Document (SDD)
    [SNAP-1206] Collocation of multiple products (S1, S2 or S3) shall be documented
    [SNAP-1187] Execute unit tests also on Windows platform
    [SNAP-1051] Investigate the possibility of SNAP becoming one of the OSGeo projects

# New in SNAP 8.0.0

In this release the development teams worked on general features like remote access of data, data processing,  
IO performance and the memory management. Also, sensor specific improvements and features have been implemented.
Noteworthy is also that we will drop the support for Python 2.7 for snappy with the next release. With version 9 of
SNAP, a Python 3 environment will be required. Here we just highlight the main improvements. Check out the full list of
issues (>200) solved for SNAP 8 in our issue database: https://bit.ly/SNAP8_changelog

### Performance improvements

In order to increase the performance of SNAP and improve the user experience some tasks have been performed. The reading
and writing performance of the BEAM-DIMAP format has been significantly improved. In some cases opening an image in SNAP
took up to 5 minutes. Now, it only takes seconds. Similar magnitude of improvement has been achieved for the writing. A
first experimental step has also been made to lower the memory consumption. An experimental Tile Cache Operator has been
implemented which gives more control of the memory usage. Please see the separate topic below for more details.

### Reworked Product Library

The Product Library has been upgraded so that it can accommodate in a flexible way any type of sensor (radar, optical,
atmospheric, etc.). In addition, the display / interaction of / with product frames or quicklooks can be done on a 3D
visualisation of Earth (NASA WorldWind). The possibility to access (and search for) remote data was introduced by
regarding remote data sources as independent SNAP plugins. Besides, the parametrized search (with parameters in the form
of values), it also handles the selection of the area of interest on the 3D visualisation of Earth. New remote data
repositories were added:

* Copernicus Scientific Data Hub (SciHub): for Sentinel-1, Sentinel-2 and Sentinel-3 data * Amazon Web Services (AWS):
  for Sentinel-2 and Landsat-8 data * Alaska Satellite Facility (ASF): for Sentinel-1 and ALOS data * US Geological
  Survey (USGS): for Landsat-8 data

### Windowed Reading of Products

Allows specifying a window (spatial subset) of either pixel coordinates or geographical coordinates, so that, instead of
first opening a full product and then subsetting it to a region of interest, the reader will open directly the region of
interest as a full-fledged product.

### New NetCDF Library Version

We now use version 5.3 of the NetCDF library. If you use the NetCDF API directly exposed through SNAP you might be
interested in the changes of the library. You can check the documentation provided by unidata
at https://docs.unidata.ucar.edu/netcdf-java/5.3/userguide/index.html. A specific migration guide is
provided (https://docs.unidata.ucar.edu/netcdf-java/5.3/userguide/upgrade_to_50.html). We also removed the support for
writing NetCDF3. They can still be read but not written anymore. If you still used the old format names 'NetCDF-CF' or '
NetCDF-BEAM' you should switch to NetCDF-CF or NetCDF-BEAM. You will benefit from smaller file sizes.

### New Reimplemented GeoCoding

The pixel-based GeoCoding had some issues in the past. It was slow, and in some situations it just didn't work and
produced artifacts. These problems affect especially Sentinel-3 data. With SNAP 8 we now use a new implementation. While
it is now faster and more accurate you might notice slight differences to in geo-location to previous SNAP versions.

### Experimental Tile Cache Operator

To further improve the memory management, especially during data processing, we introduced a special operator which can
be used in processing graphs. This operator will cache only the data of its input. If this is used, and the general
cache is disabled the amount of used memory can be reduced. The idea is that the cache memory is done automatically in
the long-term, but to do so we need some experience on when data needs to be cached. Here we would need your feedback
too. More information can be found in the wiki. https://senbox.atlassian.net/wiki/x/VQCTLw

### Sensor specific improvements by Toolboxes

Capella and SAOCOM are now supported, and the support for RCM has been updated. There is a new Soil Moisture Toolkit for
Radarsat-2/RCM. Sentinel-3 SLSTR L1 oblique view is now correctly handled. The Sentinel-3 L2 FRP (
Fire-Radiative-Products) are supported.

### New Internal Default Data Format.

With SNAP 8, we also want to introduce a new data format which will replace the BEAM-DIMAP. First we will just provide a
BETA version as plugin shortly after the SNAP release. The version should not be used in a productive system or for
operational services. But we would like to get your feedback on this new development, to further improve it. More
information will be provided with the release of the plugin.

### Known Issues

#### SRTM 3 sec DEM

During testing we observed sometimes wrong values provided by the SRTM data. Instead of the expected elevation the DEM
is rturning the elevation of an adjacent pixel. This is not always happening but depends on the download and on the
operating system. It has been only observed on latest Unix systems. Deleting and redownloading the DEM file often helped
to get the right values. For more information and updates on this issue please
see https://senbox.atlassian.net/browse/SNAP-1344

### Resolved Issues

    [SNAP-356] Products with PixelGeoCoding extremely slow to open
    [SNAP-494] BigGeoTiff writer should use compression by default.
    [SNAP-791] Write operator writes data more efficiently then ProductIO
    [SNAP-884] AbstractNetCdfReaderPlugIn leaves files open
    [SNAP-964] Switching between tabs in graph builder is slow
    [SNAP-979] Make the "Transfer Mask" action compatible with multi-size products.
    [SNAP-981] Improve resampling operator
    [SNAP-993] Revise initialise method in operator implementations
    [SNAP-999] Improve pixel-based GeoCoding
    [SNAP-1098] NetCDF writer should ensure that file has file extension
    [SNAP-1139] Writing in WriteOp is inefficient in certain case
    [SNAP-1140] Suppressed update-pop-up-windows on startup cannot be reactivated
    [SNAP-1046] Solve single core processing issue
    [SNAP-1057] Make land/sea mask compatible with multi-size products
    [SNAP-1076] doExecute() not working correctly in all scenarios
    [SNAP-1129] The output directory is not considered when loading parameters into the GUI of PixEx
    [SNAP-1148] 2 vulnerabilities found in jackson-databind - update to latest
    [SNAP-1152] Subsetting in graph does not consider tie-point grids correctly
    [SNAP-1153] Empty radiation_wavelength attribute causes exception
    [SNAP-1154] Flag is added twice if flag_mask and flag_value are different
    [SNAP-1158] Selection of predefined CRS does not work correctly
    [SNAP-1161] Product Library GUI Enhancements
    [SNAP-1162] SciHub Search UI Enhancement in Product Library
    [SNAP-1163] Support for New External Data Access Sources via Product Library
    [SNAP-1164] L3 AggregatorAveraging weights default is inappropriate
    [SNAP-1165] L3 Binning using two aggregators on the same input variable creates incorrect results
    [SNAP-1166] Subset operator should update scenegeocoding if output is a single size product
    [SNAP-1169] Subset not working if input is multi-resolution
    [SNAP-1173] Allow exchanging the L3 Binning formatter
    [SNAP-1174] More options for geo-position format
    [SNAP-1175] Make panner tool the default tool
    [SNAP-1176] Fixed-ratio for the width and height of exported image
    [SNAP-1177] Enhance graticule layer with more options
    [SNAP-1180] GeoTiffReader has too high priority
    [SNAP-1182] Reading in BandOpImage consumes too much memory
    [SNAP-1195] Operators should be shown in the same menu position for the GraphBuilder and the main menu.
    [SNAP-1196] Writing NetCDF-BEAM products with "lon" and "lat" bands fails
    [SNAP-1201] Make TileCacheOp available to users
    [SNAP-1204] Switch to OpenJDK 8
    [SNAP-1207] FAQ shall be made public
    [SNAP-1208] All help material shall be revised
    [SNAP-1213] Provide BETA version of new SNAP IO format for SNAP 8
    [SNAP-1221] Add zoom buttons to 2D worldmap
    [SNAP-1225] Implement different validation of graphs in GraphBuilder
    [SNAP-1229] Xmx computed by the smart configurator is not having into account the available memory.
    [SNAP-1231] Export to CSV file not working
    [SNAP-1232] Masks not correctly copied by ProductUtils.copyProductNodes method
    [SNAP-1238] Writing to NetCDF file does not always work if product has (x,y) dimensions
    [SNAP-1240] Allow setting intermediate writing mode via method
    [SNAP-1243] PixelGeoCoding incorrect product-boundary detection
    [SNAP-1246] IndexOutOfBoundsException when using DEM
    [SNAP-1247] NetCDF library logs a lot of useless warnings
    [SNAP-1249] Data not correctly read if data shifted by 180° 
    [SNAP-1257] Improve the error message in case of out of range for operator parameter
    [SNAP-1263] Support time attribute in NetCDF
    [SNAP-1264] Binning should be graceful if overlap region cannot be created
    [SNAP-1265] Dimensions 'rows' and 'columns' are swapped in NetCDF
    [SNAP-1269] snap-classification module does not build
    [SNAP-1270] PixelGeoCoding not correctly working if product width or height is <= 2 pixels
    [SNAP-1271] Reduce default log level for users' convenience
    [SNAP-1275] Update netcdf library to latest version
    [SNAP-1279] Access to preferences file leads to hanging binning process
    [SNAP-1282] AverageOutlier aggregator skips values if they are equal.
    [SNAP-1283] NullPointerException when running a graph in the Batch Processing dialog
    [SNAP-1293] Band_Index tag is not written for BEAM-DIMAP when PixelGeoCoding is used for multiple bands
    [SNAP-1300] Binning fails when writing to netcdf
    [SNAP-1301] Error creating product subset using Subset operator
    [SNAP-1302] Result of spectral unmixing has no GeoCoding in certain cases
    [SNAP-1303] Display format names in sorted order
    [SNAP-1304] Quicklooks Open RGB NPE when no band is opened
    [SNAP-1306] Created tie point grid for GeoTiff files is too sparse
    [SNAP-1307] Created band names for some netcdf files cause issues
    [SNAP-1310] Ensure correct update of an old SNAP installation
    [SNAP-1311] Snappy init script not fully compatible with Python3
    [SNAP-1312] PixelGeoCoding skips boundary pixels in getPixelPosUsingEstimator()
    [SNAP-1313] Allow cloning of GeoCodings
    [SNAP-1317] Update splash image and icons
    [SNAP-1321] DEM EastingNorthingParser not thread safe
    [SNAP-1322] The chunk size of NetCDF files is not correctly considered
    [SNAP-1323] On MacOs "[EMPTY]" is displayed in the title bar instead of the application name
    [SNAP-1324] Improve write performance of DIMAP
    [SNAP-1325] Improve read performance of DIMAP
    [SNAP-1326] Provide snap-jython as seperate plugin

A comprehensive list of all issues resolved in this version of SNAP can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10100&version=12702)

# New in SNAP 7.0.3

    [SNAP-1169] Subset not working if input is multi-resolution

# New in SNAP 7.2

No issues fied in this release.

# New in SNAP 7.1

    [SNAP-1129] The output directory is not considered when loading parameters into the GUI of PixEx
    [SNAP-1152] Subsetting in graph does not consider tie-point grids correctly
    [SNAP-1153] Empty radiation_wavelength attribute causes exception
    [SNAP-1154] Flag is added twice if flag_mask and flag_value are different
    [SNAP-1158] Selection of predefined CRS does not work correctly
    [SNAP-1168] In SubsetUI it is required a reference band although it is not needed (single size inputs) and it writes it to the graph
    [SNAP-1098] NetCDF writer should ensure that file has file extension
    [SNAP-1159] Immprove reading of level image data
    [SNAP-1160] Allow custom S3 VFS parameters for AWS headers
    [SNAP-1166] Subset operator should update scenegeocoding if output is a single size product

# New in SNAP 7.0

The naming of attribute features of ESRI shapefiles has changed:
Threshold variable names are shortened by getting rid of the threshold name part altogether (e.g., p_90_threshold now
becomes simply p_90) and indexes are not incremented per measure, but correspond to a band name or a time interval
(e.g., vrg_4_2 will correspond to the average of band 4 during time interval 2).

    [STEP-3] Virtual File System for Remote Data Access
    [STEP-4] Graphical User Interface for Remote Execution
    [SNAP-1138]	Virtual File System Display Issue - Remote File Repositories Configuration: labels truncated on Unix
    [SNAP-1133]	BigGeoTiffReader has threading issues
    [SNAP-1132]	Unexpected closing of channels on S3 VFS
    [SNAP-1131]	Issue with File/Dir Attributes for files from S3 VFS
    [SNAP-1130]	When opening products it shall be first checked if they exist
    [SNAP-1128]	GeoTools swallows exception within by registering a JAI listener
    [SNAP-1125]	Virtual File System help page screenshots are blurry
    [SNAP-1124]	Paths of recently opened products, containing illegal characters, can break product opening
    [SNAP-1120]	Export dialog suggests inconvenient file name
    [SNAP-1116] Remote File Repositories UI permanently brings popup message dialog about saving Remote File Repositories configurations
    [SNAP-1117] Issue with Remote Execution operator on Linux and Mac
    [SNAP-1118]	Product data access does not work via http for NASA SeaDAS
    [SNAP-1115]	GeoTiffProductReader pattern should exclude RapidEye L1 products
    [SNAP-1114]	Preferred color for a layer is not preserved when changed in option dialog
    [SNAP-1113]	Drawn line of range finder tool is hardly visible
    [SNAP-1112]	Show popup-window on startup indicating to check for updates
    [SNAP-1111]	CommandLineToolTemplateTest not running on Mac
    [SNAP-1100]	Specral unmixing shall be tested
    [SNAP-1097]	File chooser should use the selection mode of the filter if there is only one filter
    [SNAP-1094]	Prevent that SampleCoding can have two samples with the same name
    [SNAP-1093]	THIRDPARTY_LICENSES file is not included in the installer
    [SNAP-1089]	FileLocation not set in ReadOp for opened products
    [SNAP-1087]	Product names with dots are not correctly handled by pconvert
    [SNAP-1080]	Make AddElevation operator compatible with multi-size products
    [SNAP-1069]	Create wiki page for graph tests
    [SNAP-1068]	Data gets lost when reprojecting or mosaicking small areas
    [SNAP-1063]	Numerical overflow cause empty binning result
    [SNAP-1056]	ceres-ui module is needed by some operations in snappy
    [SNAP-1047]	Investigate concurrent-tile-cache implementation
    [SNAP-1012]	Define a "test schedule"
    [SNAP-1010]	Install and configure the test and build platform
    [SNAP-1009]	Merge operator does not retain order of bands in source products
    [SNAP-991]	URL in help outdated for ASTER data
    [SNAP-988]	Display third party licenses
    [SNAP-978]	Make the "Export Transect pixels" action compatible with multi-size products.
    [SNAP-976]	Make the "Export Mask pixels" action compatible with multi-size products.
    [SNAP-974]	Make the "Copy pixel Info to clipboard" action compatible with multi-size products.
    [SNAP-971]	Make the Reproject operator compatible with multi-size products.
    [SNAP-950]	Opening high resolution data with longitude values from 0-360 from NetCDF lead to a NPE
    [SNAP-949]	Option incorrectly named for Orthorectification
    [SNAP-948]	GLCM does not work properly when working with scaled values
    [SNAP-940]	Pixel position not shown in status bar if  scene has no geo-coding
    [SNAP-917]	NetCDF does not support wavelength attribute of bands
    [SNAP-886]	Implement outlier aware averaging aggregator
    [SNAP-885]	Allow growable vector in spatial binning
    [SNAP-866]  NetCdf files with extension '.nc.gz' are not read
    [SNAP-849]	Allow import of several masks at once
    [SNAP-769]	Allow disabling the access to external auxdata
    [SNAP-593]	NetCDF file containing bands with same name lead to exception

# New in SNAP 6.0.9

    SNAP-1086 Link to changelog not correct in plugin description for SNAP

# New in SNAP 6.0.8

    [SNAP-1084] DateTimeUtils does not consider UTC
    [SNAP-1083] Error while adjusting mosaic bounds to input products
    [SNAP-1082] Angle too high when opening DIMAP product (multi-size)
    [SNAP-1078] Module updates shall come with release notes / changelog
    [SNAP-1079] Saved NetCDF files can not be read by ncdump
    [SNAP-1061] Update performance parameters help

# New in SNAP 6.0.7

    [SNAP-1008] ResamplingOp fails if downsampling method is different from default value 'First'
    [SNAP-1060] Downsampling fails with ArrayIndexOutOfBoundsException
    [SNAP-1065] Consider alignment of grids when resampling
    [SNAP-1064] Handling of no-data-values during aggregation is not correct
	[SNAP-1077] Performance improvements to geo2xyzWGS84 for improved terrain flattening

# New in SNAP 6.0.6

Error in version of snap-engine and snap-desktop. Unfortunately only snap-engine has been set to version 6.0.6. The
version of snap-desktop is still 6.0.5. But this version glitch does not affect the functionality of SNAP.

    [SNAP-1067] Fix SRTM 3sec remote url

# New in SNAP 6.0.5

    [SNAP-934] No products considered when they don't have time informatin
    [SNAP-931] NPE when using products without time information in StatisticsOp
    [SNAP-929] Modules not loaded when snappy is not started from the system drive
    [SNAP-925] GLCM performance goes down if scene contains NaN values
    [SNAP-924] GLCM operator does not check for cancelation request
    [SNAP-900] NPE in GLCM

# New in SNAP 6.0.4

    [SNAP-921] Setting snap.jai.tileCacheSize to higher value than 2000 leads to 0 sized cache
    [SNAP-919] To many approximations created for some products
    [SNAP-918] When writing out statistics to a csv file, entries will be written multiple times
    [SNAP-868] Using LAT and LON in BandMaths operator leads to wrong results

# New in SNAP 6.0.3

    [SNAP-911] Scatterplot can not be computed
    [SNAP-910] Merge operator shows internal parameter in help
    [SNAP-908] It shall be possible to turn off temporal aggregation in the StatisticsOp  
    [SNAP-907] Statistics Op shall be able to retrieve categoric statistics for integer bands
    [SNAP-905] Property for controlling the creation of MERIS pixel-based Geo-Coding not correctly read
    [SNAP-880] Update WMS URLs in Layer Manager

# New in SNAP 6.0.2

    [SNAP-901] XMLSupport leaves stream open
    [SNAP-897] Resampling yields different results in desktop and gpt
    [SNAP-893] Land/Sea Mask operator is not working when using a vector with some space in the name
    [SNAP-892] Number of approximation tiles should not depend on degrees spanned on globe
    [SNAP-891] Export of pins to shapefile does not work with certain CRS

# New in SNAP 6.0.1

    [SNAP-878] Operator Import-Vector fails with products which have a pixel-based GeoCoding
    [SNAP-874] Allow to switch between INCLUDE and INTERSECT with bbox when searching for products
    [SNAP-867] Classify by raster issues

# New in SNAP 6.0

More than 100 bugs and improvements have been solved or implemented for SNAP 6.0. Beside these general improvements also
each Toolbox has got it's own improvements.

##### Some of the noteworthy improvements are:

    [SNAP-227] - GPT memory configuration
    [SNAP-371] - Smart installer does not allow execution of user-defined graphs
    [SNAP-650] - Provide progress on the command line
    [SNAP-671] - My keyboard stops working with SNAP 5.0
    [SNAP-683] - Add AAFC 2014-2016 land cover
    [SNAP-695] - Implement exclude for Merge operator
    [SNAP-736] - Unchecked 'Save As' functionality not working in graph builder
    [SNAP-743] - Support plotting of metadata values
    |SNAP-783] - Module updates are not correctly considered by snappy
    [SNAP-807] - Export of pins to Google Earth KMZ format

A comprehensive list of all issues resolved in this version of SNAP can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10100&version=12200)

# New in SNAP v5.0

The SNAP release concentrates on bug fixes and improvements. New features are mainly implemented in the Toolboxes.

##### Some of the noteworthy improvements are:

    [SNAP-373] - Export Sentinel-1 data to HDF5 is not working
    [SNAP-396] - Cannot open multiple products if progress dialog is shown
    [SNAP-398] - Names of operators shall be handeled case insensitive by gpt
    [SNAP-549] - Resampling operator does not preserve time information of the source product
    [SNAP-551] - Geo-locations are sometimes not correclty shown in status bar
    [SNAP-555] - GPF should load operator SPIs automatically
    [SNAP-559] - Can not open multiple products at once
    [SNAP-591] - Resample and Reprojection are not user friendly in GraphBuilder
    [SNAP-634] - Scene View should create default layers automatically

A comprehensive list of all issues resolved in this version of SNAP can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10100&version=11503)

# New in SNAP v4.0

### New Features and Important Changes

* Supervised Classification - Random Forest, KNN, Maximum Likelihood, Minimum Distance
* Fractional Land/Water Mask operator has been integrated into SNAP
* It is now possible to import multiple shape files at once

### Solved issues

#### Bugs

    [SNAP-186] - Metadata Table View incorrectly renders UTC attributes
    [SNAP-357] - Message needed: source product not found and operator finished
    [SNAP-358] - Sen2Cor operator is disapearing when restarting SNAP
    [SNAP-401] - Adapter suppressed when error occures
    [SNAP-408] - Problems with field content being edited
    [SNAP-420] - External Tool adapter deleted on startup
    [SNAP-438] - Resampling operator duplicates masks
    [SNAP-439] - STA: System variables are not working
    [SNAP-442] - STA: Error is not displayed
    [SNAP-443] - STA: No popup window if operator terminated ok
    [SNAP-444] - STA: Sytem parameters are not visible
    [SNAP-446] - STA: Product does not open or opens with blank images
    [SNAP-447] - STA: Execution output window hidden
    [SNAP-448] - STA: execution output window does not display output
    [SNAP-449] - STA: Source product and source product file looks editable
    [SNAP-451] - STA: target product name tag is not right
    [SNAP-456] - Netcdf check if Conventions is empty and not just null
    [SNAP-459] - SubsetOp throws exception if no intersection with region
    [SNAP-461] - Generated executables suppress log output
    [SNAP-462] - Updated modules are not considered by gpt
    [SNAP-470] - Time information is not correctly shown
    [SNAP-473] - STA: Error pattern not saved
    [SNAP-478] - No help content shown for some entries
    [SNAP-483] - DateTime is not considered in measurement files in Pixel Extraction Tool
    [SNAP-485] - Pixel size is not preserved for rectified products when exporting to ENVI file format
    [SNAP-486] - Writing NetCDF4-BEAM file fails if metadata has very long attributes
    [SNAP-504] - Error when reading processor parameters
    [SNAP-508] - All source product selectors are initialised with the selected product
    [SNAP-512] - Sen2Cor adapter crashes at startup with latest STA
    [SNAP-513] - Sen2Cor adapter does not open the output product
    [SNAP-514] - STA: new operator parameters are blank
    [SNAP-516] - Subset broken from Graphs
    [SNAP-517] - NullPointerException occurs when when using PixelGeoCoding with OLCI data
    [SNAP-519] - Graph builder does not remember the name of the current graph when saving a graph.
    [SNAP-524] - Pixel Extraction writes only the lower 16 bit of a flag band

#### Task

    [SNAP-489] - Integrate Fractional Land/Water Mask operator into SNAP

#### Improvement

    [SNAP-404] - There is no easy way to have optional parameters on an adapter
    [SNAP-450] - NodeId should be included in exception message
    [SNAP-452] - STA: external tools menu appears even if there are no tools
    [SNAP-453] - STA: Target product is displayed even if there are no target product to open
    [SNAP-457] - Duplicate lat lon bands in readOp if source product has pixel geocoding
    [SNAP-460] - Allow selection of mulitple shape files to be imported at once
    [SNAP-471] - SRTM 3 sec cannot download due to site down
    [SNAP-479] - Allow dragging view when pressing space bar
    [SNAP-487] - Assembly of snap-engine should contain scripts to start gpt
    [SNAP-490] - Allow merging of scenes without geographic boundary check
    [SNAP-515] - STA: folders are not supported as processor parameters
    [SNAP-523] - STA: processors with no input and no output declared show an "almost" empty I/O Pannel

A comprehensive list of all issues resolved in this version of SNAP can be found in our
[issue tracking system](https://senbox.atlassian.net/issues/?filter=11700)

# New in SNAP v3.0

### New Features and Important Changes

* A new Resampling Operator has been introduced. Its main purpose is to make the bands of a multi-size product equal in
  size. It is possible to choose for the resampling different aggregation and interpolation methods. If a user invokes
  an action which can not handle multi-size products the user is asked to resample the product.
* New function *bit_set(bandName, bitNumber)* in Band Maths available. The method tests if the bit at the specified
  number is set or not.
* GPF operators can now compute multi-size target products. To do so the computeTile(band, tile) must be implemented. In
  order to reflect a dynamic state created in the initialize() method (the source and therefore the target is
  single-size) the methods *boolean canComputeTile()* and
  *boolean canComputeTileStack()* can be overridden.
* GPF operators can now access a product manager *Operator.getProductManager()*. This is useful to provide auxiliary
  products to other operators in the graph.
* Support for quicklooks have been integrated in Products Explorer and Product Library
* Operations to perform land masking have been added.
* The operator LinearTodB has been renamed to LinearToFromdB

A comprehensive list of all issues resolved in this version of SNAP can be found in our
[issue tracking system](https://senbox.atlassian.net/issues/?filter=11500)

# Release notes of former versions

* [Resolved issues in version 2.x](https://senbox.atlassian.net/issues/?filter=11501)
* [Resolved issues in version 2.0](https://senbox.atlassian.net/issues/?filter=11502)
* [Resolved issues in version 2.0 beta](https://senbox.atlassian.net/issues/?filter=11503)
* [Resolved issues in version 1.0.1](https://senbox.atlassian.net/issues/?filter=11504)