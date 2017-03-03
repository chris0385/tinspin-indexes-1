package org.tinspin.index.rtree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.tinspin.index.RectangleEntryDist;
import org.tinspin.index.rtree.DistanceFunction.RectangleSquareDist;

import static org.junit.Assert.*;

public class RTreeDistanceTest {

	@Test
	public void testFarthestDist() {
		RTree<Object> tree = RTree.createRStar(2);

		tree.insert(new double[] { 0, 0 }, "0,0");
		tree.insert(new double[] { 1, 1 }, "1,1");
		tree.insert(new double[] { 0, 1 }, "0,1");
		tree.insert(new double[] { 1, 0 }, "1,0");
		tree.insert(new double[] { 3, 1 }, "divideByZeroSafe");

		DistanceFunction dist = new DistanceFunction.FarthestNeighbor(DistanceFunction.EDGE);
		Iterator<RectangleEntryDist<Object>> q = tree
				.queryRangedNearestNeighbor(new double[] { 3, 1 }, dist, dist, Filter.ALL).iterator();

		assertEquals("0,0", q.next().value());
		assertEquals("0,1", q.next().value());
		assertEquals("1,0", q.next().value());
		assertEquals("1,1", q.next().value());
		assertEquals("divideByZeroSafe", q.next().value());
		assertFalse(q.hasNext());
	}

	@Test
	public void testRectangleDist() {
		RTree<Object> tree = RTree.createRStar(2);

		// 5 within the rectangle
		tree.insert(new double[] { 0, 0 }, "0,0");
		tree.insert(new double[] { 1, 1 }, "1,1");
		tree.insert(new double[] { 0, 1 }, "0,1");
		tree.insert(new double[] { 1, 0 }, "1,0");
		tree.insert(new double[] { 0.5, 0.5 }, "middle");
		// outside. We put the expected distance as value
		tree.insert(new double[] { 2, 1 }, 1.);
		tree.insert(new double[] { 2, 2 }, 2.);
		tree.insert(new double[] { -1, 1 }, 1.);
		tree.insert(new double[] { -10, -5 }, 100. + 25.);

		RectangleSquareDist dist = new DistanceFunction.RectangleSquareDist(new double[] { 0, 0 },
				new double[] { 1, 1 });
		Iterator<RectangleEntryDist<Object>> q = tree
				.queryRangedNearestNeighbor(new double[] { 3, 1 }, dist, dist, Filter.ALL).iterator();

		// 5 within the rectangle
		Set foundWithinRectangle = new HashSet<>();
		for (int i = 0; i < 5; i++) {
			RectangleEntryDist<Object> next = q.next();
			assertEquals(0, next.dist(), 0.00001);
			foundWithinRectangle.add(next.value());
		}
		assertTrue(foundWithinRectangle.containsAll(Arrays.asList("0,0", "1,1", "0,1", "1,0", "middle")));

		// 4 outside
		double lastDist = 0;
		for (int i = 0; i < 4; i++) {
			RectangleEntryDist<Object> next = q.next();
			assertEquals((double) next.value(), next.dist(), 0.00001);
			assertTrue(next.dist() >= lastDist);
			lastDist = next.dist();
		}
	}
}
