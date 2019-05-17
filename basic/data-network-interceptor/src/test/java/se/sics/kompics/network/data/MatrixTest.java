/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.network.data;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;
import org.ujmp.core.SparseMatrix;
import org.ujmp.core.calculation.Calculation;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class MatrixTest {

    @Test
    public void maxTest() {
        double[][] baseM = new double[][] { { 0.0, 1.0, 3.0, 1.0, 0.0 }, { 0.0, 1.0, 3.0, 1.0, 0.0 },
                { 0.0, 1.0, 3.0, 1.0, 0.0 }, { 0.0, 1.0, 3.0, 1.0, 0.0 }, { 0.0, 1.0, 3.0, 1.0, 0.0 } };
        Matrix m = DenseMatrix.Factory.importFromArray(baseM);
        // Matrix ms = SparseMatrix.Factory.zeros(5, 5);
        System.out.println("Matrix is: " + m);
        Matrix row = m.getRowList().get(3);
        // Matrix rows = ms.getRowList().get(3); // doesn't work...in sparse matrices rows do not exist
        System.out.println("Row is: " + row);
        Matrix maxRow = row.indexOfMax(Calculation.Ret.NEW, Calculation.COLUMN);
        // Matrix maxRowS = rows.indexOfMax(Calculation.Ret.NEW, Calculation.COLUMN);
        System.out.println("Max is: " + maxRow);
        long mPos00 = maxRow.getAsLong(0, 0);
        // assertNotNull(maxRowS);
        assertEquals(2, mPos00);
    }

    @Test
    public void sparseTest() {
        Matrix m = SparseMatrix.Factory.zeros(5, 5);
        System.out.println("SparseMatrix is: " + m);
        double zero = m.getAsDouble(2, 2);
        System.out.println("Sparse value (2, 2) -> " + zero);
        assertEquals(0.0, zero, 0.0001);
    }
}
