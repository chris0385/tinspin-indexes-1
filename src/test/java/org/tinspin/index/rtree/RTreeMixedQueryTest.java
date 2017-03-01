/*
 * Copyright 2017 Christophe Schmaltz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinspin.index.rtree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.tinspin.index.RectangleEntryDist;

public class RTreeMixedQueryTest {

	// seed chosen randomly using a well equilibrated dice :-)
	// [makes test reproducible]
	Random rnd = new Random(4);

	@Test
	public void test() {
		RTree<String> tree = RTree.createRStar(3);
		
		System.out.println(tree.getDepth());

		int N_ELEMENTS = 100000;
		for (int i = 0; i < N_ELEMENTS; i++) {
			double[] position = randDouble(3);
			assert tree.queryExact(position, position) == null;
			tree.insert(position, "#" + i);
			
		}
		System.out.println(tree.getDepth());
			validateTreeParent(tree);
//		if(true)return;
		
		Iterable<RectangleEntryDist<String>> q = tree.queryMixed(new double[] { 1, 1, 1 }, DistanceFunction.CENTER_SQUARE, DistanceFunction.EDGE_SQUARE,
				new double[] { 0.5, 0.5, 0.5 }, new double[] { 1, 1, 1 });
		
		
		double d = 0;
		int maxQueueSize = 0;
		int nElements = 0;
		Set<String> duplicateCheck = new HashSet<>();
		for (Iterator<RectangleEntryDist<String>> iterator = q.iterator(); iterator.hasNext();) {
			RectangleEntryDist<String> e = iterator.next();
			System.out.println(iterator + " " + e);

			assertTrue(e.value()+" @"+nElements, duplicateCheck.add(e.value()));
			assertTrue(d <= e.dist());
			d = e.dist();
			nElements++;
			maxQueueSize = Math.max(maxQueueSize, ((RTreeMixedQuery) iterator).queueSize());
			
			if (true) {
				iterator.remove();
				assertEquals(tree.size(), N_ELEMENTS - nElements);
			}
		}

		
		perfTestNN(tree);
		// should be about the size of the tree
		System.out.println(nElements * (1 << tree.getDims()));
		System.out.println(nElements);
		System.out.println(maxQueueSize);
	}

	private void validateTreeParent(RTree<?> tree) {
		assertNull(tree.getRoot().getParent());
		tree.getRoot().getEntries().forEach(e-> {
			validateTreeParent(e, tree.getRoot());
		});
	}

	private void validateTreeParent(Entry e, RTreeNode<?> parent) {
		if (e instanceof RTreeNode<?>) {
			RTreeNode<?> node = (RTreeNode) e;
			assertEquals(parent, node.getParent());
			node.getEntries().forEach(cld-> {
				validateTreeParent(cld, node);
			});
		}
	}

	private void perfTestNN(RTree<String> tree) {
		int k = tree.size() / 8;
		double[] center = new double[] { 1, 1, 1 };
		
		{
			Iterable<RectangleEntryDist<String>> q = tree.queryMixed(center, DistanceFunction.EDGE,
					DistanceFunction.EDGE, Filter.ALL);
			RTreeQueryKnn<String> res = tree.queryKNN(center, k, DistanceFunction.EDGE);
			// test equivalence
			Iterator<RectangleEntryDist<String>> iterator = q.iterator();
			int i=0;
			for (; iterator.hasNext();) {
				assertTrue("I="+i, res.hasNext());
				assertEquals(res.next().value(), iterator.next().value());
				i++;
				if (i >= k)
					break;
			}
			assertFalse(res.hasNext());
		}
		
		long timeRef = timeOf(() -> {
			RTreeQueryKnn<String> res = tree.queryKNN(center, k, DistanceFunction.EDGE);
			int cnt = 0;
			for(;res.hasNext();) {
				cnt++;
				DistEntry<String> e = res.next();
				assertNotNull(e);
			}
			System.out.println(cnt);
			assertTrue(cnt<=k);
		});
		
		long timeMixed = timeOf(() -> {
			Iterable<RectangleEntryDist<String>> q = tree.queryMixed(center, DistanceFunction.EDGE,
					DistanceFunction.EDGE, Filter.ALL);
			int cnt = 0;
			if (false) {
				/* 
				 * A lot of the speedup is simply due to the copy. Adding this
				 * makes the code 6,28 times slower for 12500 neighbors out of 100000.
				 * 
				 * Probably cache locality as my code is only faster for large results.
				 * 
				 * It seems as if executing the query multiple times is better than caching the results...
				 */
				List<RectangleEntryDist<String>> arr = new ArrayList<>();
				q.forEach(arr::add);
				q = arr;
			}
			for (Iterator<RectangleEntryDist<String>> iterator = q.iterator(); iterator.hasNext();) {
				RectangleEntryDist<String> e = iterator.next();
				assertNotNull(e);
				cnt++;
				if (cnt >= k)
					break;
			}
		});
		
		
		
		System.out.println("timeMixed=" + timeMixed + ", timeRef=" + timeRef + " # " + (timeRef / (double)timeMixed));
	}
	
	public long timeOf(Runnable run) {
		final int nRuns = 5;
		long time = 0;
		for (int i = 0; i <= nRuns; i++) {
			long timeBefore = System.nanoTime();
			run.run();
			long delta = System.nanoTime() - timeBefore;
			if (i > 0) {
				// ignore the first one for warm up
				time += delta;
			}
		}
		return time / nRuns;
	}

	private double[] randDouble(int n) {
		double[] r = new double[n];
		for (int i = 0; i < n; i++) {
			r[i] = rnd.nextDouble();
		}
		return r;
	}

}
