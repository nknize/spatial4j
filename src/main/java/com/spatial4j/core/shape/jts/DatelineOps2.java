package com.spatial4j.core.shape.jts;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * {DatelineOps2}.java
 * nknize, 1/26/15 10:07 AM
 * <p/>
 * Description:
 */
public class DatelineOps2 {

    /**
     * NOTE: This geometry factory servers as a work-around for a JTS bug. See
     * @see TestJTSIntersectionAndCoordinateFilterBug
     *
     * http://sourceforge.net/p/jts-topo-suite/mailman/message/32619267/
     */
    private static final GeometryFactory FACTORY = new GeometryFactory(
            new PackedCoordinateSequenceFactory());

    /**
     * If <code>geom</code> spans the dateline, then this modifies it to be a
     * valid JTS geometry that extends to the right of the standard -180 to +180
     * width such that some points are greater than +180 but some remain less.
     * Takes care to invoke
     * {@link com.vividsolutions.jts.geom.Geometry#geometryChanged()} if needed.
     *
     * @return The number of times the geometry spans the dateline. >= 0
     *
     * @see com.spatial4j.core.shape.jts
     */
//    public static int unwrapDateline(Geometry geom, final double systemBoundsX, final double systemBoundsY) {
//
//    }

}
