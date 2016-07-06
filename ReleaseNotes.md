#New in SNAP v4.0

###New Features and Important Changes
* Supervised Classification - Random Forest, KNN, Maximum Likelihood, Minimum Distance

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


