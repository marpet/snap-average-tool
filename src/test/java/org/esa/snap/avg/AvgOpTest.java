package org.esa.snap.avg;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;


/**
 * @author Marco Peters
 */
public class AvgOpTest {


    @Test
    public void testOperatorSpiIsLoaded() {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = registry.getOperatorSpi("AvgTool");
        assertNotNull(operatorSpi);
        assertEquals("AvgTool", operatorSpi.getOperatorAlias());
        assertNotNull(operatorSpi.getOperatorDescriptor());
        assertSame(operatorSpi.getOperatorClass(), operatorSpi.getOperatorDescriptor().getOperatorClass());
    }

}