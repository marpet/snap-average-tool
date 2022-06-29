/*
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.avg;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.math.MathUtils;

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Vector;

/**
 * @author Marco Peters
 */
public class AvgWinOpImage extends OpImage {

    private final int hws;
    private final int windowSize;

    public AvgWinOpImage(RenderedImage srcImage, int windowSize) {
        this(srcImage, null, windowSize);
    }

    public AvgWinOpImage(RenderedImage srcImage, RenderedImage maskImage, int windowSize) {
        super(createSourceVector(srcImage, maskImage), createLayout(srcImage), null, false);
        if (windowSize % 2 == 0) {
            throw new IllegalArgumentException("Window size must be odd");
        }
        this.windowSize = windowSize;
        hws = windowSize / 2;
    }

    private static Vector createSourceVector(RenderedImage srcImage, RenderedImage maskImage) {
        if (maskImage == null) {
            maskImage = ConstantDescriptor.create((float) srcImage.getWidth(), (float) srcImage.getHeight(), new Byte[]{1}, null);
        }
        return vectorize(srcImage, maskImage);
    }

    private static ImageLayout createLayout(RenderedImage srcImage) {
        final int bandSize = srcImage.getWidth() * srcImage.getHeight();
        final ComponentSampleModelJAI sm = new ComponentSampleModelJAI(DataBuffer.TYPE_DOUBLE, srcImage.getWidth(), srcImage.getHeight(),
                                                                       1, srcImage.getWidth(), new int[]{0, bandSize});
        final ColorModel cm = PlanarImage.createColorModel(sm);
        return new ImageLayout(0, 0, srcImage.getWidth(), srcImage.getHeight(),
                               0, 0, srcImage.getTileWidth(), srcImage.getTileHeight(), sm, cm);
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle destRect) {
        PlanarImage srcImage = planarImages[0];
        PlanarImage mskImage = planarImages[1];
        final Rectangle imgRect = srcImage.getBounds();
        int x0 = destRect.x;
        int y0 = destRect.y;
        final int x1 = x0 + destRect.width - 1;
        final int y1 = y0 + destRect.height - 1;

        for (int y = y0; y <= y1; y++) {
            final int cy = y - hws;
            for (int x = x0; x <= x1; x++) {
                final int cx = x - hws;
                final Rectangle srcRect = new Rectangle(cx, cy, windowSize, windowSize);
                final Rectangle effRect = imgRect.intersection(srcRect);

                final Raster mskRaster = mskImage.getData(effRect);
                final int[] mskData = new int[effRect.width * effRect.height];
                mskRaster.getSamples(effRect.x, effRect.y, effRect.width, effRect.height, 0, mskData);

                // is center pixel is not masked, skip further calculation
                if (mskRaster.getSample(x, y, 0) == 0) {
                    writableRaster.setSample(x, y, 0, Double.NaN);
                    writableRaster.setSample(x, y, 1, Double.NaN);
                    continue;
                }

                final Raster srcRaster = srcImage.getData(effRect);
                final double[] srcData = new double[effRect.width * effRect.height];
                srcRaster.getSamples(effRect.x, effRect.y, effRect.width, effRect.height, 0, srcData);

                int count = 0;
                double sum = 0;
                for (int i = 0; i < srcData.length; i++) {
                    double value = srcData[i];
                    final int mask = mskData[i];
                    if (!Double.isNaN(value) && mask > 0) {
                        count++;
                        sum += value;
                    }
                }
                final double avg = count == 0 ? Double.NaN : sum / count;
                writableRaster.setSample(x, y, 0, avg);
                writableRaster.setSample(x, y, 1, count);
            }
        }
    }

    @Override
    public Rectangle mapSourceRect(Rectangle rectangle, int i) {
        return new Rectangle(rectangle);
    }

    @Override
    public Rectangle mapDestRect(Rectangle rectangle, int i) {
        return new Rectangle(rectangle);
    }

}
