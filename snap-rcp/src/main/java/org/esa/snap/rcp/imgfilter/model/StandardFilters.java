package org.esa.snap.rcp.imgfilter.model;

/**
 * Created by Norman on 20.03.2014.
 */
// JAN2019 - Daniel Knowles - Added some new filters
//                          - Removed the "Arithmetic Mean 4x4" filter because it is misleading as it is not centered about the target pixel.

public class StandardFilters {
    public static Filter[] LINE_DETECTION_FILTERS = {
            new Filter("Horizontal Edges", "he", 3, 3, new double[]{
                    -1, -1, -1,
                    +2, +2, +2,
                    -1, -1, -1
            }, 1.0),
            new Filter("Vertical Edges", "ve", 3, 3, new double[]{
                    -1, +2, -1,
                    -1, +2, -1,
                    -1, +2, -1
            }, 1.0),
            new Filter("Left Diagonal Edges", "lde", 3, 3, new double[]{
                    +2, -1, -1,
                    -1, +2, -1,
                    -1, -1, +2
            }, 1.0),
            new Filter("Right Diagonal Edges", "rde", 3, 3, new double[]{
                    -1, -1, +2,
                    -1, +2, -1,
                    +2, -1, -1
            }, 1.0),

            new Filter("Compass Edge Detector", "ced", 3, 3, new double[]{
                    -1, +1, +1,
                    -1, -2, +1,
                    -1, +1, +1,
            }, 1.0),

            new Filter("Diagonal Compass Edge Detector", "dced", 3, 3, new double[]{
                    +1, +1, +1,
                    -1, -2, +1,
                    -1, -1, +1,
            }, 1.0),

            new Filter("Roberts Cross North-West", "rcnw", 2, 2, new double[]{
                    +1, 0,
                    0, -1,
            }, 1.0),

            new Filter("Roberts Cross North-East", "rcne", 2, 2, new double[]{
                    0, +1,
                    -1, 0,
            }, 1.0),
    };
    public static Filter[] GRADIENT_DETECTION_FILTERS = {
            new Filter("Sobel North", "sn", 3, 3, new double[]{
                    -1, -2, -1,
                    +0, +0, +0,
                    +1, +2, +1,
            }, 1.0),
            new Filter("Sobel South", "ss", 3, 3, new double[]{
                    +1, +2, +1,
                    +0, +0, +0,
                    -1, -2, -1,
            }, 1.0),
            new Filter("Sobel West", "sw", 3, 3, new double[]{
                    -1, 0, +1,
                    -2, 0, +2,
                    -1, 0, +1,
            }, 1.0),
            new Filter("Sobel East", "se", 3, 3, new double[]{
                    +1, 0, -1,
                    +2, 0, -2,
                    +1, 0, -1,
            }, 1.0),
            new Filter("Sobel North East", "sne", 3, 3, new double[]{
                    +0, -1, -2,
                    +1, +0, -1,
                    +2, +1, -0,
            }, 1.0),
    };
    public static Filter[] SMOOTHING_FILTERS = {
            new Filter("Arithmetic Mean 3x3", "am3", 3, 3, new double[]{
                    +1, +1, +1,
                    +1, +1, +1,
                    +1, +1, +1,
            }, 9.0),


            // NOTE: Removed the "Arithmetic Mean 4x4" filter because it is misleading as it is not centered about the target pixel

            

            new Filter("Arithmetic Mean 5x5", "am5", 5, 5, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            }, 25.0),

            new Filter("Arithmetic Mean 7x7", "am7", 7, 7, new double[]{
                    +1, +1, +1, +1, +1, 1, 1,
                    +1, +1, +1, +1, +1, 1, 1,
                    +1, +1, +1, +1, +1, 1, 1,
                    +1, +1, +1, +1, +1, 1, 1,
                    +1, +1, +1, +1, +1, 1, 1,
                    +1, +1, +1, +1, +1, 1, 1,
                    +1, +1, +1, +1, +1, 1, 1,
            }, 49.0),



            // Linear filter applied to a 3x3 grid with pixels weighted to emulate 2x2 square grid centered about the target pixel
            new Filter("Weighted Mean 2x2", "am2", 3, 3, new double[]{
                    0.25, 0.50, 0.25,
                    0.50, 1.00, 0.50,
                    0.25, 0.50, 0.25
            }, 4.0),


            // Linear filter applied to a 5x5 grid with pixels weighted to emulate 4x4 square grid centered about the target pixel
            new Filter("Weighted Mean 4x4", "am4", 5, 5, new double[]{
                    0.25, 0.50, 0.50, 0.50, 0.25,
                    0.50, 1.00, 1.00, 1.00, 0.50,
                    0.50, 1.00, 1.00, 1.00, 0.50,
                    0.50, 1.00, 1.00, 1.00, 0.50,
                    0.25, 0.50, 0.50, 0.50, 0.25
            }, 16.0),

            // Linear filter applied to a 3x3 grid with pixels weighted to emulate a circle of 3 pixel diameter centered about the target pixel
            new Filter("Weighted Circular Mean 3x3", "cm3", 3, 3, new double[]{
                    0.5454, 0.9717, 0.5454,
                    0.9717, 1.0000, 0.9717,
                    0.5454, 0.9717, 0.5454

            }, 7.0684),


            // Linear filter applied to a 5x5 grid with pixels weighted to emulate a circle of 5 pixel diameter centered about the target pixel
            new Filter("Weighted Circular Mean 5x5", "cm5", 5, 5, new double[]{
                    0.1369, 0.7693, 0.9832, 0.7693, 0.1369,
                    0.7693, 1.0000, 1.0000, 1.0000, 0.7693,
                    0.9832, 1.0000, 1.0000, 1.0000, 0.9832,
                    0.7693, 1.0000, 1.0000, 1.0000, 0.7693,
                    0.1369, 0.7693, 0.9832, 0.7693, 0.1369
            }, 19.6348),



            new Filter("Low-Pass 3x3", "lp3", 3, 3, new double[]{
                    +1, +2, +1,
                    +2, +4, +2,
                    +1, +2, +1,
            }, 16.0),
            new Filter("Low-Pass 5x5", "lp5", 5, 5, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +4, +4, +4, +1,
                    +1, +4, 12, +4, +1,
                    +1, +4, +4, +4, +1,
                    +1, +1, +1, +1, +1,
            }, 60.0),
    };



    public static Filter[] STRAYLIGHT_FILTERS = {

            // Same as the "Arithmetic Mean 3x3" filter but with the center pixel having zero weight such that all contribution comes from straylight
            new Filter("Straylight 3x3", "str_am3", 3, 3, new double[]{
                    +1, +1, +1,
                    +1, +0, +1,
                    +1, +1, +1,
            }, 8.0),


            // Same as the "Arithmetic Mean 5x5" filter but with the center pixel having zero weight such that all contribution comes from straylight
            new Filter("Straylight 5x5", "str_am5", 5, 5, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +0, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            }, 24.0),


            // Similar to the "Straylight 5x5" filter but with proximity weighting
            new Filter("Straylight 5x5 #2", "str_am52", 5, 5, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +2, +2, +2, +1,
                    +1, +2,  0, +2, +1,
                    +1, +2, +2, +2, +1,
                    +1, +1, +1, +1, +1,
            }, 32.0),


            // Same as the "Weighted Circular Mean 3x3" filter but with the center pixel having zero weight such that all contribution comes from straylight
            new Filter("Straylight Circular (3x3)", "cm3", 3, 3, new double[]{
                    0.5454, 0.9717, 0.5454,
                    0.9717, 0.0000, 0.9717,
                    0.5454, 0.9717, 0.5454

            }, 6.0684),


            // Same as the "Weighted Circular Mean 5x5" filter but with the center pixel having zero weight such that all contribution comes from straylight
            new Filter("Straylight Circular (5x5)", "cm5", 5, 5, new double[]{
                    0.1369, 0.7693, 0.9832, 0.7693, 0.1369,
                    0.7693, 1.0000, 1.0000, 1.0000, 0.7693,
                    0.9832, 1.0000, 0.0000, 1.0000, 0.9832,
                    0.7693, 1.0000, 1.0000, 1.0000, 0.7693,
                    0.1369, 0.7693, 0.9832, 0.7693, 0.1369
            }, 18.6348),
    };




    public static Filter[] SHARPENING_FILTERS = {
            new Filter("High-Pass 3x3 #1", "hp31", 3, 3, new double[]{
                    -1, -1, -1,
                    -1, +9, -1,
                    -1, -1, -1
            }, 1.0),


            new Filter("High-Pass 3x3 #2", "hp32", 3, 3, new double[]{
                    +0, -1, +0,
                    -1, +5, -1,
                    +0, -1, +0
            }, 1.0),

            new Filter("High-Pass 5x5", "hp5", 5, 5, new double[]{
                    +0, -1, -1, -1, +0,
                    -1, +2, -4, +2, -1,
                    -1, -4, 13, -4, -1,
                    -1, +2, -4, +2, -1,
                    +0, -1, -1, -1, +0,
            }, 1.0),

    };
    public static Filter[] LAPLACIAN_FILTERS = {
            new Filter("Laplace 3x3 (a)", "lap3a", 3, 3, new double[]{
                    +0, -1, +0,
                    -1, +4, -1,
                    +0, -1, +0,
            }, 1.0),
            new Filter("Laplace 3x3 (b)", "lap3b", 3, 3, new double[]{
                    -1, -1, -1,
                    -1, +8, -1,
                    -1, -1, -1,
            }, 1.0),
            new Filter("Laplace 5x5 (a)", "lap5a", 5, 5, new double[]{
                    0, 0, -1, 0, 0,
                    0, -1, -2, -1, 0,
                    -1, -2, 16, -2, -1,
                    0, -1, -2, -1, 0,
                    0, 0, -1, 0, 0,
            }, 1.0),
            new Filter("Laplace 5x5 (b)", "lap5b", 5, 5, new double[]{
                    -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1,
                    -1, -1, 24, -1, -1,
                    -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1,
            }, 1.0),
    };
    public static Filter[] NON_LINEAR_FILTERS = {
            new Filter("Minimum 3x3", "min3", Filter.Operation.MIN, 3, 3),
            new Filter("Minimum 5x5", "min5", Filter.Operation.MIN, 5, 5),
            new Filter("Minimum 7x7", "min7", Filter.Operation.MIN, 7, 7),
            new Filter("Maximum 3x3", "max3", Filter.Operation.MAX, 3, 3),
            new Filter("Maximum 5x5", "max5", Filter.Operation.MAX, 5, 5),
            new Filter("Maximum 7x7", "max7", Filter.Operation.MAX, 7, 7),
            new Filter("Mean 3x3", "mean3", Filter.Operation.MEAN, 3, 3),
            new Filter("Mean 5x5", "mean5", Filter.Operation.MEAN, 5, 5),
            new Filter("Mean 7x7", "mean7", Filter.Operation.MEAN, 7, 7),
            new Filter("Median 3x3", "median3", Filter.Operation.MEDIAN, 3, 3),
            new Filter("Median 5x5", "median5", Filter.Operation.MEDIAN, 5, 5),
            new Filter("Median 7x7", "median7", Filter.Operation.MEDIAN, 7, 7),
            new Filter("Standard Deviation 3x3", "stddev3", Filter.Operation.STDDEV, 3, 3),
            new Filter("Standard Deviation 5x5", "stddev5", Filter.Operation.STDDEV, 5, 5),
            new Filter("Standard Deviation 7x7", "stddev7", Filter.Operation.STDDEV, 7, 7),
    };
    public static Filter[] MORPHOLOGICAL_FILTERS = {
            new Filter("Erosion 3x3", "erode3", Filter.Operation.ERODE, 3, 3),
            new Filter("Erosion 5x5", "erode5", Filter.Operation.ERODE, 5, 5),
            new Filter("Erosion 7x7", "erode7", Filter.Operation.ERODE, 7, 7),
            new Filter("Dilation 3x3", "dilate3", Filter.Operation.DILATE, 3, 3),
            new Filter("Dilation 5x5", "dilate5", Filter.Operation.DILATE, 5, 5),
            new Filter("Dilation 7x7", "dilate7", Filter.Operation.DILATE, 7, 7),
            new Filter("Opening 3x3", "open3", Filter.Operation.OPEN, 3, 3),
            new Filter("Opening 5x5", "open5", Filter.Operation.OPEN, 5, 5),
            new Filter("Opening 7x7", "open7", Filter.Operation.OPEN, 7, 7),
            new Filter("Closing 3x3", "close3", Filter.Operation.CLOSE, 3, 3),
            new Filter("Closing 5x5", "close5", Filter.Operation.CLOSE, 5, 5),
            new Filter("Closing 7x7", "close7", Filter.Operation.CLOSE, 7, 7),
    };
}
