/*
 * Copyright 2016 Tilmann Zaeschke
 * Modification Copyright 2017 Christophe Schmaltz
 * 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinspin.index.rtree;

import org.tinspin.index.RectangleEntry;

@FunctionalInterface
public interface DistanceFunction {

	public static CenterDistance CENTER = new CenterDistance();
	public static EdgeDistance EDGE = new EdgeDistance();
	// The square root is costly, and generally not required for sorting.
	@Deprecated
	public static DistanceFunction CENTER_SQUARE = DistanceFunction::centerSquareDistance;
	// The square root is costly, and generally not required for sorting.
	/** SQRT should be quit cheap nowadays, do we have any tests that confirm demonstrate
	 * an improvement? (tzaeschke) */
	@Deprecated
	public static DistanceFunction EDGE_SQUARE = DistanceFunction::edgeSquareDistance;

	double dist(double[] center, double[] min, double[] max);

	/**
	 * Some algorithm use this method on the entries containing user supplied values.
	 * This can be overridden if the min/max coordinates only represent the 
	 * bounding-box of the object.
	 * 
	 * If your entry is actually a sphere, a car, an human or a cat, you may need this.
	 */
	default double dist(double[] center, RectangleEntry<?> entry) {
		return dist(center, entry.lower(), entry.upper());
	}

	/**
	 * This class calculates the square distance to a rectangular shaped object.
	 * 
	 * This class completely ignores the center given as parameter to the interface.
	 * 
	 * TODO: maybe we could get rid of the center parameter altogether and always let 
	 *       the DistanceFunction hold it's reference points?
	 */
	public static class RectangleSquareDist implements DistanceFunction {
		private final double[] lower;
		private final double[] upper;

		public RectangleSquareDist(double[] lower, double[] upper) {
			this.lower = lower;
			this.upper = upper;
		}

		@Override
		public double dist(double[] ignored, double[] min, double[] max) {
			double dist = 0;
			for (int i = 0; i < lower.length; i++) {
				double d = 0;
				if (min[i] > upper[i]) {
					// "right" side of our rectangle
					d = min[i] - upper[i];
				} else if (max[i] < lower[i]) {
					// "left" side of our rectangle
					d = lower[i] - max[i];
				} // else intersecting
				dist += d * d;
			}
			return dist;
		}
	}

	/**
	 * Special wrapper class which takes the inverse or the given function.
	 * Can be used to get the farthest neighbors using the nearest neighbor algorithm.
	 */
	public static class FarthestNeighbor implements DistanceFunction {
		private static final double EPSILON = 2 * Double.MIN_VALUE;
		private final DistanceFunction dist;

		public FarthestNeighbor(DistanceFunction dist) {
			this.dist = dist;
		}

		@Override
		public double dist(double[] center, double[] min, double[] max) {
			double d = dist.dist(center, min, max);
			if (d < EPSILON) {
				// no divide by zero
				return Double.POSITIVE_INFINITY;
			}
			return 1 / d;
		}

		@Override
		public double dist(double[] center, RectangleEntry<?> entry) {
			double d = dist.dist(center, entry);
			if (d < EPSILON) {
				return Double.POSITIVE_INFINITY;
			}
			return 1 / d;
		}
	}

	public static class CenterDistance implements DistanceFunction {

		@Override
		public double dist(double[] center, double[] min, double[] max) {
			double dist = 0;
			for (int i = 0; i < center.length; i++) {
				double d = (min[i] + max[i]) * 0.5 - center[i];
				dist += d*d;
			}
			return Math.sqrt(dist);
		}
	}

	public static class EdgeDistance implements DistanceFunction {

		@Override
		public double dist(double[] center, double[] min, double[] max) {
			double dist = 0;
			for (int i = 0; i < center.length; i++) {
				double d = 0;
				if (min[i] > center[i]) {
					d = min[i] - center[i];
				} else if (max[i] < center[i]) {
					d = center[i] - max[i];
				}
				dist += d*d;
			}
			return Math.sqrt(dist);
		}
	}

	public static double centerSquareDistance(double[] center, double[] min, double[] max) {
		double dist = 0;
		for (int i = 0; i < center.length; i++) {
			double d = (min[i] + max[i]) * 0.5 - center[i];
			dist += d * d;
		}
		return dist;
	}

	public static double edgeSquareDistance(double[] center, double[] min, double[] max) {
		double dist = 0;
		for (int i = 0; i < center.length; i++) {
			double d = 0;
			if (min[i] > center[i]) {
				d = min[i] - center[i];
			} else if (max[i] < center[i]) {
				d = center[i] - max[i];
			}
			dist += d * d;
		}
		return dist;
	}

}
