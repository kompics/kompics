/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package passwordstore.dndx;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * An implementation of Transferable that allows you to specify the flavor and
 * data for the transfer.
 *
 * @author sky
 */
public class DefaultTransferable implements Transferable {
    private ObjectFlavorPair[] _data;
    
    public DefaultTransferable(DataFlavor flavor, Object data) {
        this(new ObjectFlavorPair(flavor, data));
    }
    
    public DefaultTransferable(ObjectFlavorPair...data) {
        _data = data;
    }

    public DataFlavor[] getTransferDataFlavors() {
        DataFlavor[] flavors = new DataFlavor[_data.length];
        for (int i = 0; i < _data.length; i++) {
            flavors[i] = _data[i].getDataFlavor();
        }
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (ObjectFlavorPair data : _data) {
            if (data.getDataFlavor().equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        for (ObjectFlavorPair data : _data) {
            if (data.getDataFlavor().equals(flavor)) {
                return data.getData();
            }
        }
        throw new UnsupportedFlavorException(flavor);
    }
    
    
    public static final class ObjectFlavorPair {
        private final Object _data;
        private final DataFlavor _flavor;
        
        public ObjectFlavorPair(DataFlavor flavor, Object data) {
            _data = data;
            _flavor = flavor;
        }
        
        public Object getData() {
            return _data;
        }
        
        public DataFlavor getDataFlavor() {
            return _flavor;
        }
    }
}
