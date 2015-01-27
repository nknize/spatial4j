/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spatial4j.core.shape;

import com.carrotsearch.randomizedtesting.RandomizedContext;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContextFactory;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.io.jts.JtsWktShapeParser;
import com.spatial4j.core.shape.impl.PointImpl;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.spatial4j.core.shape.jts.JtsPoint;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Random;

import static com.spatial4j.core.shape.SpatialRelation.CONTAINS;
import static com.spatial4j.core.shape.SpatialRelation.DISJOINT;
import static com.spatial4j.core.shape.SpatialRelation.INTERSECTS;

/** Tests {@link com.spatial4j.core.shape.jts.JtsGeometry} and some other code related
 * to {@link com.spatial4j.core.context.jts.JtsSpatialContext}.
 */
public class JtsGeometryTest extends AbstractTestShapes {

  private final String POLY_STR = "Polygon((-10 30, -40 40, -10 -20, 40 20, 0 0, -10 30))";
  private final String POLY_WITH_HOLE = "POLYGON ((-87.9127 41.7161, -87.9382 41.7441, -87.959 41.7743, -87.9745 41.8062,   " +
          "-87.9845 41.8392, -87.989 41.873, -87.9878 41.9069, -87.9808 41.9404, -87.9683 41.973, -87.9505 42.0042, -87.9274 42.0335, " +
          "-87.8997 42.0603, -87.8676 42.0844, -87.8317 42.1053, -87.7925 42.1226, -87.7507 42.1361, -87.707 42.1456, -87.662 42.1509, " +
          "-87.6164 42.152, -87.571 42.1488, -87.5266 42.1414, -87.4837 42.1298, -87.4431 42.1144, -87.4055 42.0952, -87.3715 42.0727, " +
          "-87.3415 42.0472, -87.316 42.0191, -87.2955 41.9888, -87.2803 41.9569, -87.2706 41.9237, -87.2665 41.89, -87.2681 41.8561, " +
          "-87.2754 41.8226, -87.2882 41.79, -87.3064 41.759, -87.3296 41.7298, -87.3574 41.7031, -87.3895 41.6792, -87.4254 41.6585,  " +
          "-87.4643 41.6413, -87.5059 41.6278, -87.5493 41.6184, -87.5939 41.6131, -87.6391 41.6121, -87.6841 41.6153, -87.7282 41.6226, " +
          "-87.7708 41.6341, -87.8111 41.6494, -87.8486 41.6684, -87.8826 41.6908, -87.9127 41.7161), (-87.8479 41.6691, -87.8105 41.6502, "
          + "-87.7703 41.6349, -87.7279 41.6235, -87.6839 41.6161, -87.6391 41.613, -87.594 41.614, -87.5495 41.6193, " +
          "-87.5063 41.6287, -87.4649 41.6421, -87.426 41.6592, -87.3903 41.6799, -87.3583 41.7037, -87.3306 41.7303, " +
          "-87.3074 41.7594, -87.2894 41.7904, -87.2766 41.8228, -87.2693 41.8561, -87.2677 41.8899, -87.2718 41.9236, -87.2815 41.9566, " +
          "-87.2967 41.9885, -87.3171 42.0187, -87.3424 42.0467, -87.3723 42.0721, -87.4063 42.0945, -87.4438 42.1136, -87.4842 42.129, " +
          "-87.5269 42.1405, -87.5712 42.1479, -87.6164 42.1511, -87.6618 42.15, -87.7067 42.1447, -87.7503 42.1353, -87.792 42.1218, " +
          "-87.831 42.1045, -87.8668 42.0837, -87.8988 42.0597, -87.9264 42.033, -87.9494 42.0038, -87.9672 41.9727, -87.9797 41.9402,  " +
          "-87.9866 41.9068, -87.9878 41.873, -87.9834 41.8394, -87.9733 41.8064, -87.9578 41.7747, -87.9372 41.7446, -87.9117 41.7167,  " +
          "-87.8818 41.6914, -87.8479 41.6691))";
  private JtsGeometry POLY_SHAPE;
  private final int DL_SHIFT = 180;//since POLY_SHAPE contains 0 0, I know a shift of 180 will make it cross the DL.
  private JtsGeometry POLY_SHAPE_DL;//POLY_SHAPE shifted by DL_SHIFT to cross the dateline

  public JtsGeometryTest() throws ParseException {
    super(JtsSpatialContext.GEO);
    POLY_SHAPE = (JtsGeometry) ctx.readShapeFromWkt(POLY_STR);

    if (ctx.isGeo()) {
      POLY_SHAPE_DL = shiftPoly(POLY_SHAPE, DL_SHIFT);
      assertTrue(POLY_SHAPE_DL.getBoundingBox().getCrossesDateLine());
    }
  }

  private JtsGeometry shiftPoly(JtsGeometry poly, final int lon_shift) throws ParseException {
    final Random random = RandomizedContext.current().getRandom();
    Geometry pGeom = poly.getGeom();
    assertTrue(pGeom.isValid());
    //shift 180 to the right
    pGeom = (Geometry) pGeom.clone();
    pGeom.apply(new CoordinateFilter() {
      @Override
      public void filter(Coordinate coord) {
        coord.x = normX(coord.x + lon_shift);
        if (ctx.isGeo() && Math.abs(coord.x) == 180 && random.nextBoolean())
          coord.x = - coord.x;//invert sign of dateline boundary some of the time
      }
    });
    pGeom.geometryChanged();
    assertFalse(pGeom.isValid());
    return (JtsGeometry) ctx.readShapeFromWkt(pGeom.toText());
  }

  @Test
  public void testRelations() throws ParseException {
    testRelations(false);
    testRelations(true);
  }
  public void testRelations(boolean prepare) throws ParseException {
    assert !((JtsWktShapeParser)ctx.getWktShapeParser()).isAutoIndex();
    //base polygon
    JtsGeometry base = (JtsGeometry) ctx.readShapeFromWkt("POLYGON((0 0, 10 0, 5 5, 0 0))");
    //shares only "10 0" with base
    JtsGeometry polyI = (JtsGeometry) ctx.readShapeFromWkt("POLYGON((10 0, 20 0, 15 5, 10 0))");
    //within base: differs from base by one point is within
    JtsGeometry polyW = (JtsGeometry) ctx.readShapeFromWkt("POLYGON((0 0, 9 0, 5 5, 0 0))");
    // disjoint: poly inside hole of larger poly
    JtsGeometry polyH = (JtsGeometry) ctx.readShapeFromWkt(POLY_WITH_HOLE);
    JtsGeometry polyD = (JtsGeometry) ctx.readShapeFromWkt("POLYGON((-87.6544 41.9677, -87.6544 41.9717, -87.6489 41.9717, -87.6389 41.9677, -87.6544 41.9677))");
    //a boundary point of base
    Point pointB = ctx.makePoint(0, 0);
    //a shared boundary line of base
    JtsGeometry lineB = (JtsGeometry) ctx.readShapeFromWkt("LINESTRING(0 0, 10 0)");
    //a line sharing only one point with base
    JtsGeometry lineI = (JtsGeometry) ctx.readShapeFromWkt("LINESTRING(10 0, 20 0)");

    if (prepare) base.index();
    assertRelation(CONTAINS, base, base);//preferred result as there is no EQUALS
    assertRelation(INTERSECTS, base, polyI);
    assertRelation(CONTAINS, base, polyW);
    assertRelation(DISJOINT, polyH, polyD);
    assertRelation(CONTAINS, base, pointB);
    assertRelation(CONTAINS, base, lineB);
    assertRelation(INTERSECTS, base, lineI);
    if (prepare) lineB.index();
    assertRelation(CONTAINS, lineB, lineB);//line contains itself
    assertRelation(CONTAINS, lineB, pointB);
  }

  @Test
  public void testEmpty() throws ParseException {
    Shape emptyGeom = ctx.readShapeFromWkt("POLYGON EMPTY");
    testEmptiness(emptyGeom);
    assertRelation("EMPTY", DISJOINT, emptyGeom, POLY_SHAPE);
  }

  @Test
  public void testArea() {
    //simple bbox
    Rectangle r = randomRectangle(20);
    JtsSpatialContext ctxJts = (JtsSpatialContext) ctx;
    JtsGeometry rPoly = ctxJts.makeShape(ctxJts.getGeometryFrom(r), false, false);
    assertEquals(r.getArea(null), rPoly.getArea(null), 0.0);
    assertEquals(r.getArea(ctx), rPoly.getArea(ctx), 0.000001);//same since fills 100%

    assertEquals(1300, POLY_SHAPE.getArea(null), 0.0);

    //fills 27%
    assertEquals(0.27, POLY_SHAPE.getArea(ctx) / POLY_SHAPE.getBoundingBox().getArea(ctx), 0.009);
    assertTrue(POLY_SHAPE.getBoundingBox().getArea(ctx) > POLY_SHAPE.getArea(ctx));
  }

  @Test
  @Repeat(iterations = 100)
  public void testPointAndRectIntersect() {
    Rectangle r = randomRectangle(5);

    assertJtsConsistentRelate(r);
    assertJtsConsistentRelate(r.getCenter());
  }

  @Test
  public void testRegressions() {
    assertJtsConsistentRelate(new PointImpl(-10, 4, ctx));//PointImpl not JtsPoint, and CONTAINS
    assertJtsConsistentRelate(new PointImpl(-15, -10, ctx));//point on boundary
    assertJtsConsistentRelate(ctx.makeRectangle(135, 180, -10, 10));//180 edge-case
  }

  @Test
  public void testWidthGreaterThan180() throws ParseException {
    //does NOT cross the dateline but is a wide shape >180
    JtsGeometry jtsGeo = (JtsGeometry) ctx.readShapeFromWkt("POLYGON((-161 49, 0 49, 20 49, 20 89.1, 0 89.1, -161 89.2, -161 49))");
    assertEquals(161+20,jtsGeo.getBoundingBox().getWidth(), 0.001);

    //shift it to cross the dateline and check that it's still good
    jtsGeo = shiftPoly(jtsGeo, 180);
    assertEquals(161+20,jtsGeo.getBoundingBox().getWidth(), 0.001);
  }

  private void assertJtsConsistentRelate(Shape shape) {
    IntersectionMatrix expectedM = POLY_SHAPE.getGeom().relate(((JtsSpatialContext) ctx).getGeometryFrom(shape));
    SpatialRelation expectedSR = JtsGeometry.intersectionMatrixToSpatialRelation(expectedM);
    //JTS considers a point on a boundary INTERSECTS, not CONTAINS
    if (expectedSR == SpatialRelation.INTERSECTS && shape instanceof Point)
      expectedSR = SpatialRelation.CONTAINS;
    assertRelation(null, expectedSR, POLY_SHAPE, shape);

    if (ctx.isGeo()) {
      //shift shape, set to shape2
      Shape shape2;
      if (shape instanceof Rectangle) {
        Rectangle r = (Rectangle) shape;
        shape2 = makeNormRect(r.getMinX() + DL_SHIFT, r.getMaxX() + DL_SHIFT, r.getMinY(), r.getMaxY());
      } else if (shape instanceof Point) {
        Point p = (Point) shape;
        shape2 = ctx.makePoint(normX(p.getX() + DL_SHIFT), p.getY());
      } else {
        throw new RuntimeException(""+shape);
      }

      assertRelation(null, expectedSR, POLY_SHAPE_DL, shape2);
    }
  }

  @Test
  public void testPointNormalization() throws ParseException {
    JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
    Point p = new JtsPoint((factory.getGeometryFactory().createPoint(new Coordinate(-115, -275, Double
            .NaN))), ctx);
    DistanceUtils.normPoint(p);
  }

  @Test
  public void testDatelineCross() throws ParseException {
    Shape geom = ctx.readShapeFromWkt( "Polygon((176 30, 178 40, -174 30, -176 20, 176 30))");
  }

  @Test
  public void testRussia() throws IOException, ParseException {
    final String wktStr = readFirstLineFromRsrc("/russia.wkt.txt");
    //Russia exercises JtsGeometry fairly well because of these characteristics:
    // * a MultiPolygon
    // * crosses the dateline
    // * has coordinates needing normalization (longitude +180.000xxx)

    //TODO THE RUSSIA TEST DATA SET APPEARS CORRUPT
    // But this test "works" anyhow, and exercises a ton.
    //Unexplained holes revealed via KML export:
    // TODO Test contains: 64°12'44.82"N    61°29'5.20"E
    //  64.21245  61.48475
    // FAILS
    //assertRelation(null,SpatialRelation.CONTAINS, shape, ctx.makePoint(61.48, 64.21));

    JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
    factory.normWrapLongitude = true;

    JtsSpatialContext ctx = factory.newSpatialContext();

    Shape shape = ctx.readShapeFromWkt(wktStr);
    //System.out.println("Russia Area: "+shape.getArea(ctx));
  }

  @Test
  public void testFiji() throws IOException, ParseException {
    //Fiji is a group of islands crossing the dateline.
    String wktStr = readFirstLineFromRsrc("/fiji.wkt.txt");

    JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
    factory.normWrapLongitude = true;
    JtsSpatialContext ctx = factory.newSpatialContext();

    Shape shape = ctx.readShapeFromWkt(wktStr);

    assertRelation(null,SpatialRelation.CONTAINS, shape,
            ctx.makePoint(-179.99,-16.9));
    assertRelation(null,SpatialRelation.CONTAINS, shape,
            ctx.makePoint(+179.99,-16.9));
    assertTrue(shape.getBoundingBox().getWidth() < 5);//smart bbox
    System.out.println("Fiji Area: "+shape.getArea(ctx));
  }

  private String readFirstLineFromRsrc(String wktRsrcPath) throws IOException {
    InputStream is = getClass().getResourceAsStream(wktRsrcPath);
    assertNotNull(is);
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      return br.readLine();
    } finally {
      is.close();
    }
  }
}
