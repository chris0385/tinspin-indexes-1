package org.tinspin.index.rtree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.Assert.*;
import org.junit.Test;

public class RTreeSerializationTest {

	@Test
	public void testSerialization() throws IOException, ClassNotFoundException {
		RTree<String> tree = RTree.createRStar(3);
		tree.insert(new double[] { 0, 0, 0 }, new double[] { 1, 1, 1 }, "(0,1)");
		tree.insert(new double[] { 0, 0, 0 }, "(0,0)");

		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		try (ObjectOutputStream o = new ObjectOutputStream(dataOut)) {
			o.writeObject(tree);
			o.writeObject("dummy");
		}
		try (ObjectInputStream i = new ObjectInputStream(new ByteArrayInputStream(dataOut.toByteArray()))) {
			tree = (RTree<String>) i.readObject();
			// check for stream corruption
			assertEquals("dummy", i.readObject());
		}
		assertEquals(2, tree.size());
		assertEquals("(0,0)", tree.queryExact(new double[] { 0, 0, 0 }, new double[] { 0, 0, 0 }));
		assertEquals("(0,1)", tree.queryExact(new double[] { 0, 0, 0 }, new double[] { 1, 1, 1 }));
	}
}
