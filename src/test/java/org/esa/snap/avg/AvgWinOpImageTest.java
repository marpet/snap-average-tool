package org.esa.snap.avg;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ImageUtils;
import org.junit.Test;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class AvgWinOpImageTest {

    @Test
    public void testAvgWinOpImage() {
        float[] values = new float[]{
                12, 13, Float.NaN, 6,
                14, 1, 25, 50,
                Float.NaN, Float.NaN, 17, 7,
                Float.NaN, Float.NaN, 13, 21,
        };

        final RenderedImage source = ImageUtils.createRenderedImage(4, 4, ProductData.createInstance(values));
        final AvgWinOpImage avgImage = new AvgWinOpImage(source, 3);
        assertEquals(4, avgImage.getWidth());
        assertEquals(4, avgImage.getHeight());

        final Raster avgRaster = avgImage.getData();
        final double[] avgData = new double[4 * 4];
        final int[] cntData = new int[4 * 4];
        avgRaster.getSamples(0, 0, 4, 4, 0, avgData);
        avgRaster.getSamples(0, 0, 4, 4, 1, cntData);
        assertEquals(10, avgData[0], 1.0e-6);
        assertEquals(4, cntData[0]);
        assertEquals(13, avgData[1], 1.0e-6);
        assertEquals(5, cntData[1]);
        assertEquals(19, avgData[2], 1.0e-6);
        assertEquals(5, cntData[2]);
        assertEquals(27, avgData[3], 1.0e-6);
        assertEquals(3, cntData[3]);
        assertEquals(10, avgData[4], 1.0e-6);
        assertEquals(4, cntData[4]);
        assertEquals(14, avgData[9], 1.0e-6);
        assertEquals(5, cntData[9]);
        assertEquals(Double.NaN, avgData[12], 1.0e-6);
        assertEquals(0, cntData[12]);
    }

    @Test
    public void testAvgWinOpImage_WithMask() {
        float[] values = new float[]{
                12, 13, Float.NaN, 6,
                14, 1, 25, 50,
                Float.NaN, Float.NaN, 17, 7,
                Float.NaN, Float.NaN, 13, 21,
        };

        byte[] mask = new byte[]{
                1, 0, 1, 1,
                1, 1, 1, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
        };

        final RenderedImage sourceImage = ImageUtils.createRenderedImage(4, 4, ProductData.createInstance(values));
        final RenderedImage maskImage = ImageUtils.createRenderedImage(4, 4, ProductData.createInstance(mask));
        final AvgWinOpImage avgImage = new AvgWinOpImage(sourceImage, maskImage, 3);
        assertEquals(4, avgImage.getWidth());
        assertEquals(4, avgImage.getHeight());

        final Raster avgRaster = avgImage.getData();
        final double[] avgData = new double[4 * 4];
        final int[] cntData = new int[4 * 4];
        avgRaster.getSamples(0, 0, 4, 4, 0, avgData);
        avgRaster.getSamples(0, 0, 4, 4, 1, cntData);
        assertEquals(9, avgData[0], 1.0e-6);
        assertEquals(3, cntData[0]);
        assertEquals(10.6666667, avgData[6], 1.0e-6);
        assertEquals(3, cntData[6]);

        assertEquals(Double.NaN, avgData[10], 1.0e-6);
        assertEquals(0, cntData[10]);
        assertEquals(Double.NaN, avgData[15], 1.0e-6);
        assertEquals(0, cntData[15]);
    }
}