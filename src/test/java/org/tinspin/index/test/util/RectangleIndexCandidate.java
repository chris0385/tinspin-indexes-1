/*
 * Copyright 2011-2017 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package org.tinspin.index.test.util;

import java.util.Arrays;

import org.tinspin.index.Index;
import org.tinspin.index.QueryIterator;
import org.tinspin.index.QueryIteratorKNN;
import org.tinspin.index.RectangleEntry;
import org.tinspin.index.RectangleEntryDist;
import org.tinspin.index.RectangleIndex;
import org.tinspin.index.rtree.Entry;
import org.tinspin.index.rtree.RTree;
import org.tinspin.index.test.util.TestStats.INDEX;

public class RectangleIndexCandidate extends Candidate {
	
	private final RectangleIndex<Object> idx;
	private final int dims;
	private final int N;
	private double[] data;
	private static final Object O = new Object();
	private QueryIterator<RectangleEntry<Object>> query = null;
	private QueryIteratorKNN<RectangleEntryDist<Object>> queryKnn = null;
	private final boolean bulkloadSTR;
	
	/**
	 * @param ri index 
	 * @param ts test stats
	 */
	@SuppressWarnings("unchecked")
	public RectangleIndexCandidate(RectangleIndex<?> ri, TestStats ts) {
		this.N = ts.cfgNEntries;
		this.dims = ts.cfgNDims;
		this.idx = (RectangleIndex<Object>) ri;
		this.bulkloadSTR = ts.INDEX.equals(INDEX.STR);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void load(double[] data, int dims) {
		this.data = data;
		if (bulkloadSTR) {
			Entry<Object>[] entries = new Entry[N];
			int pos = 0;
			for (int i = 0; i < N; i++) {
				double[] lo = new double[dims];
				double[] hi = new double[dims];
				System.arraycopy(data, pos, lo, 0, dims);
				pos += dims;
				System.arraycopy(data, pos, hi, 0, dims);
				pos += dims;
				entries[i] = new Entry<Object>(lo, hi, O);
			}
			RTree<Object> rt = (RTree<Object>) idx;
			rt.load(entries);
		} else {
			int pos = 0;
			for (int n = 0; n < N; n++) {
				double[] lo = new double[dims];
				double[] hi = new double[dims];
				System.arraycopy(data, pos, lo, 0, dims);
				pos += dims;
				System.arraycopy(data, pos, hi, 0, dims);
				pos += dims;
				idx.insert(lo, hi, O);
			}
		}
	}

	@Override
	public Object preparePointQuery(double[][] q) {
		return q;
	}

	@Override
	public int pointQuery(Object qA) {
		int n = 0;
		double[][] dA = (double[][]) qA; 
		for (int i = 0; i < dA.length; i+=2) {
			if (idx.queryExact(dA[i], dA[i+1]) != null) {
				n++;
			}
		}
		return n;
	}
	
	@Override
	public boolean supportsPointQuery() {
		return dims <= 16;
	}
	
	@Override
	public int unload() {
		int n = 0;
		double[] lo = new double[dims];
		double[] hi = new double[dims];
		for (int i = 0; i < N>>1; i++) {
			System.arraycopy(data, i*dims*2, lo, 0, dims);
			System.arraycopy(data, i*dims*2+dims, hi, 0, dims);
			n += idx.remove(lo, hi) != null ? 1 : 0;
			int i2 = N-i-1;
			System.arraycopy(data, i2*dims*2, lo, 0, dims);
			System.arraycopy(data, i2*dims*2+dims, hi, 0, dims);
			n += idx.remove(lo, hi) != null? 1 : 0;
		}
		if ((N%2) != 0) {
			int i = (N>>1);
			System.arraycopy(data, i*dims*2, lo, 0, dims);
			System.arraycopy(data, i*dims*2+dims, hi, 0, dims);
			n += idx.remove(lo, hi) != null ? 1 : 0;
		}
		return n;
	}
	
	
	@Override
	public int query(double[] min, double[] max) {
		if (query == null) {
			query = idx.queryIntersect(min, max);
		} else {
			query.reset(min, max);
		}
		int n = 0;
		while (query.hasNext()) {
			query.next();
			n++;
		}
		return n;
	}
	
	@Override
	public double knnQuery(int k, double[] center) {
		if (k == 1) {
			return idx.query1NN(center).dist();
		}
		if (queryKnn == null) {
			queryKnn = idx.queryKNN(center, k);
		} else {
			queryKnn.reset(center, k);
		}
		double ret = 0;
		while (queryKnn.hasNext()) {
			RectangleEntryDist<Object> e = queryKnn.next();
			ret += e.dist();
		}
		return ret;
	}

	@Override
	public boolean supportsKNN() {
		return true;
	}
	
	@Override
	public void release() {
		data = null;
	}

	public Index<Object> getNative() {
		return idx;
	}

	@Override
	public void getStats(TestStats S) {
		S.statNnodes = idx.getNodeCount(); 
		S.statNpostlen = idx.getDepth();
	}
	
	@Override
	public int update(double[][] updateTable) {
		int n = 0;
		for (int i = 0; i < updateTable.length; ) {
			double[] lo1 = updateTable[i++];
			double[] up1 = updateTable[i++];
			double[] lo2 = Arrays.copyOf(updateTable[i++], dims);
			double[] up2 = Arrays.copyOf(updateTable[i++], dims);
			if (idx.update(lo1, up1, lo2, up2) != null) {
				n++;
			}
		}
		return n;
	}
	
	@Override
	public boolean supportsUpdate() {
		return dims <= 16;
	}

	@Override
	public boolean supportsUnload() {
		return dims <= 16;
	}
	
	@Override
	public String toString() {
		return idx.toString(); 
	}
	
	@Override
	public String toStringTree() {
		return idx.toStringTree();
	}

	@Override
	public void clear() {
		idx.clear();
	}

	@Override
	public int size() {
		return idx.size();
	}
}
