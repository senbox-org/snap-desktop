#New in SNAP v3.0

###New Features and Important Changes
* New function *bit_set(bandName, bitNumber)* in Band Maths available. The method tests if the bit at 
the specified number is set or not.
* GPF operators can now compute multi-size target products. To do so the computeTile(band, tile) must 
be implemented. In order to reflect a dynamic state created in the initialize() method (the source 
and therefore the target is single-size) the methods *boolean canComputeTile()* and 
*boolean canComputeTileStack()* can be overridden.
* GPF operators can now access a product manager *Operator.getProductManager()*. This is useful to 
provide auxiliary products to other operators in the graph. 
* Quicklooks integrated with Products and Product Library
* Land masking
* LinearTodB renamed to LinearToFromdB

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our 
[issue tracking system](https://senbox.atlassian.net/projects/SNAP/versions/11000)

#Release notes of former versions

* [Resolved issues in version 2.x](https://senbox.atlassian.net/projects/SNAP/versions/11001)
* [Resolved issues in version 2.0](https://senbox.atlassian.net/projects/SNAP/versions/10200)
* [Resolved issues in version 2.0 beta](https://senbox.atlassian.net/projects/SNAP/versions/10901)
* [Resolved issues in version 1.0.1](https://senbox.atlassian.net/projects/SNAP/versions/10203)


