SNAP Release Notes
==================

#New in SNAP v5.0
The SNAP release concentrates on bug fixes and improvements. New features are implemented in the Toolboxes. 
Below is a list of bug fixes and smaller small improvements. A comprehensive list of all issues resolved in this version of SNAP can be found in our 
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10100&version=11503)

### Solved issues
####Bug
    [SNAP-80] - Variables are not automatically quoted if needed
    [SNAP-182] - Importing options cause exception
    [SNAP-190] - STA adapters don't uninstall
    [SNAP-260] - Multiple product selection allowed but not working
    [SNAP-279] - When multiple products are selected for closing, only one is closed
    [SNAP-345] - Cloud probability processing leads to a NullPointerException if the source data file is a reprojected product.
    [SNAP-349] - Title of SNAP does not reflect the product currently in focus
    [SNAP-368] - SNAP main window title not correctly updated
    [SNAP-371] - Smart installer does not allow execution of user-defined graphs. 
    [SNAP-373] - Export Sentinel-1 data to HDF5 is not working
    [SNAP-396] - Cannot open multiple products if progress dialog is shown
    [SNAP-416] - Time stamp not displayed properly for OLCI L1B products
    [SNAP-421] - Uninstalling STA plugins doesn't work
    [SNAP-496] - Processing time shown by L3 Binning is not correct
    [SNAP-497] - No progress indicator is shown when selecting in processor UI a source product
    [SNAP-498] - Pixel Info View does not always consider the selected view
    [SNAP-507] - Fractional water mask processor fails with NPE for certain regions
    [SNAP-529] - STA: update leads to a NullPointerException
    [SNAP-532] - STA: adapters are not in Graph Builder anymore
    [SNAP-543] - Allow to invert the mouse zooming
    [SNAP-549] - Resampling operator does not preserve time information of the source product
    [SNAP-551] - Geo-locations are sometimes not correclty shown in status bar
    [SNAP-556] - LandCover mask UI does not update class list with single landcover band
    [SNAP-559] - Can not open multiple products at once
    [SNAP-584] - Subset can duplicate flag codings
    [SNAP-588] - AbstractedMetadataIO not handling file without extension
    [SNAP-591] - Resample and Reprojection are not user friendly in GraphBuilder
    [SNAP-595] - Statistics operator not usable via API
    [SNAP-603] - Resampling a product without GeoCoding causes a NPE
    [SNAP-618] - SRTM 1 src DEM offset
    [SNAP-629] - GeoTiff reader fails to read Kompsat-5 Geotiff geocoding
	[SNAP-636] - Graph Builder: Output name does not change when changing input/read image

####Improvements & new Features
    [SNAP-50] - Task Queue: Sound at end of processing
    [SNAP-63] - gpt states that it can't find a product reader for a file which does not exist
    [SNAP-168] - Dialog window sizes nor attributes do not stick
    [SNAP-339] - Sound-effect at the end of a processing-task
    [SNAP-377] - Sound-alert for end-of-processing
    [SNAP-392] - Smart Configurator: results can't be sorted
    [SNAP-398] - Names of operators shall be handeled case insensitive by gpt
    [SNAP-435] - Closing multiple products
    [SNAP-458] - Help button in Plugin window is not working
    [SNAP-518] - Merge project-functionality with session functionality?
    [SNAP-526] - STA Operators have a wrong alias in the Graph Builder
    [SNAP-528] - Help for removed Product Grabber shall be removed too
    [SNAP-533] - Date pattern should support also MMM
    [SNAP-534] - Help for tools which do not exist any more should be removed
    [SNAP-536] - Help page for layer menu is not correctly referenced
    [SNAP-537] - An exception is thrown in the background when opening a Landsat 8 product
    [SNAP-540] - Add script for retrieving format names from reader and writer
    [SNAP-548] - LineTimeCoding constructor should not throw IOException
    [SNAP-552] - Extend ProductData for data type long
    [SNAP-555] - GPF should load operator SPIs automatically
    [SNAP-567] - Update bundeled Java JRE
    [SNAP-569] - Allow equal minimum and maximum value when creating a mask based on value range
    [SNAP-570] - Make message boxes copy their text to clipboard when pressing CTRL+C
    [SNAP-573] - Reconsider icon sizes for menu items
    [SNAP-575] - Give access to the logging of SNAP
    [SNAP-576] - Make cancelling operators less scary
    [SNAP-577] - Select bands by default in mask creation dialog
    [SNAP-583] - Let user open multiple bands at once
    [SNAP-585] - Update NetBeans dependencies
    [SNAP-589] - Projects and Session Functionalities
    [SNAP-592] - Allow lazy loading of tie-point grids
    [SNAP-597] - Extent list of known dimension names for NetCDF
    [SNAP-604] - Information dialog should show module name of product reader
    [SNAP-610] - Allow saving session to another file
    [SNAP-612] - Selection doesn't work properly when chosing a predefined CRS
    [SNAP-619] - Usability: Graph Builder slow to open
    [SNAP-625] - Convert computed band to "real" band
    [SNAP-630] - CollocateOp does not properly handle flag codings
    [SNAP-631] - NetCDF readers read global attributes, but writers do not write them
    [SNAP-634] - Scene View should create default layers automatically

#New in SNAP v4.0

###New Features and Important Changes
* Supervised Classification - Random Forest, KNN, Maximum Likelihood, Minimum Distance
* Fractional Land/Water Mask operator has been integrated into SNAP
* It is now possible to import multiple shape files at once

### Solved issues
####Bugs
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
####Task
    [SNAP-489] - Integrate Fractional Land/Water Mask operator into SNAP
####Improvement
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


#New in SNAP v3.0

###New Features and Important Changes
* A new Resampling Operator has been introduced. Its main purpose is to make the bands of a multi-size 
product equal in size. It is possible to choose for the resampling different aggregation and 
interpolation methods. If a user invokes an action which can not handle multi-size products the user is 
asked to resample the product.   
* New function *bit_set(bandName, bitNumber)* in Band Maths available. The method tests if the bit at 
the specified number is set or not.
* GPF operators can now compute multi-size target products. To do so the computeTile(band, tile) must 
be implemented. In order to reflect a dynamic state created in the initialize() method (the source 
and therefore the target is single-size) the methods *boolean canComputeTile()* and 
*boolean canComputeTileStack()* can be overridden.
* GPF operators can now access a product manager *Operator.getProductManager()*. This is useful to 
provide auxiliary products to other operators in the graph. 
* Support for quicklooks have been integrated in Products Explorer and Product Library
* Operations to perform land masking have been added.
* The operator LinearTodB has been renamed to LinearToFromdB

A comprehensive list of all issues resolved in this version of SNAP can be found in our 
[issue tracking system](https://senbox.atlassian.net/issues/?filter=11500)

#Release notes of former versions

* [Resolved issues in version 2.x](https://senbox.atlassian.net/issues/?filter=11501)
* [Resolved issues in version 2.0](https://senbox.atlassian.net/issues/?filter=11502)
* [Resolved issues in version 2.0 beta](https://senbox.atlassian.net/issues/?filter=11503)
* [Resolved issues in version 1.0.1](https://senbox.atlassian.net/issues/?filter=11504)


