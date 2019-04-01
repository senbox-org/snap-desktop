SNAP Release Notes
==================
    
# New in SNAP 7.0
The naming of attribute features of ESRI shapefiles has changed: 
Threshold variable names are shortened by getting rid of the threshold name part 
altogether (e.g., p_90_threshold now becomes simply p_90) and indexes are not 
incremented per measure, but correspond to a band name or a time interval
(e.g., vrg_4_2 will correspond to the average of band 4 during time interval 2).
  
    [SNAP-1063] Numerical overflow cause empty binning result
    [SNAP-1056] ceres-ui module is needed by some operations in snappy
    [SNAP-948]  GLCM does not work properly when working with scaled values
    [SNAP-866]  NetCdf files with extension '.nc.gz' are not read

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
Error in version of snap-engine and snap-desktop. Unfortunately only snap-engine has been set to version 6.0.6. 
The version of snap-desktop is still 6.0.5. But this version glitch does not affect the functionality of SNAP.

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
More than 100 bugs and improvements have been solved or implemented for SNAP 6.0.
Beside these general improvements also each Toolbox has got it's own improvements.
  
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