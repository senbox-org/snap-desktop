This modules wraps the tango icon library.

Current wrapped version is 0.8.90.

To update the module, download the latest tango distribution and unzip it to src/main/resources/tango. 
Make sure to update the VERSION file and remove all makefiles. Then run org.esa.snap.tango.internal.CodeGenerator 
with current working directory being this module directory. It will generate the two Java classes:

* org.esa.snap.tango.TangoIcons
* org.esa.snap.tango.TangoIconsTest