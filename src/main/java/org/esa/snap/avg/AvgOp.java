package org.esa.snap.avg;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.TreeMap;

/**
 * @author Marco Peters
 */
@OperatorMetadata(alias = "AvgTool",
        category = "Raster",
        version = "0.2",
        authors = "Marco Peters",
        copyright = "(c) 2022 by Brockmann Consult",
        description = "Operator averaging pixels of a masked area within a defined window.")
public class AvgOp  extends Operator {

    private static final String AVG_SUFFIX = "_avg";
    private static final String CNT_SUFFIX = "_cnt";
    @SourceProduct(label = "Source product",
            description = "Any source product.")
    private Product sourceProduct;

    @Parameter(label = "Names of the bands", description = "The names of the bands to be processed", rasterDataNodeType = Band.class)
    private String[] bandNames;

    @Parameter(label = "The window size", description = "The size is used in horizontal and vertical direction.", interval = "[3,125]", defaultValue = "3")
    private int windowSize;

    @Parameter(label = "Expression defining the masked area")
    private String maskExpression;
    private TreeMap<String, RenderedImage> imageMap = new TreeMap<>();
    private MultiLevelImage maskImage;

    @Override
    public void initialize() throws OperatorException {
        validateInput();
        final Dimension rasterSize = sourceProduct.getRasterDataNode(bandNames[0]).getRasterSize();
        final Mask validMask = Mask.BandMathsType.create("__avgValidMask", "", rasterSize.width, rasterSize.height,
                                                         maskExpression,
                                                       Color.yellow, 0.5f);
        validMask.setOwner(sourceProduct);
        maskImage = validMask.getSourceImage();

        final Product target = new Product(sourceProduct.getName() + AVG_SUFFIX, "Avg", rasterSize.width, rasterSize.height);
        ProductUtils.copyGeoCoding(sourceProduct, target);
        target.setStartTime(sourceProduct.getStartTime());
        target.setEndTime(sourceProduct.getEndTime());

        for (String rasterName : bandNames) {
            final Band avgBand = target.addBand(getAvgTargetName(rasterName), ProductData.TYPE_FLOAT64);
            avgBand.setNoDataValueUsed(true);
            avgBand.setNoDataValue(Double.NaN);
            final Band cntBand = target.addBand(getCntTargetName(rasterName), ProductData.TYPE_FLOAT32);
            cntBand.setNoDataValueUsed(true);
            cntBand.setNoDataValue(Float.NaN);
        }
        setTargetProduct(target);
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        for (String rasterName : bandNames) {
            final MultiLevelImage srcImage = sourceProduct.getRasterDataNode(rasterName).getGeophysicalImage();
            final AvgWinOpImage avgWinOpImage = new AvgWinOpImage(srcImage, maskImage, windowSize);
            imageMap.put(rasterName, avgWinOpImage);
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle rect = targetTile.getRectangle();
        final String targetBandName = targetBand.getName();
        final String sourceRasterName = targetBandName.substring(0, targetBandName.length() - 4);
        if(targetBandName.endsWith(AVG_SUFFIX)) {
            final double[] dArray = new double[rect.width * rect.height];
            final Raster bandData = imageMap.get(sourceRasterName).getData(new Rectangle(rect.x, rect.y, rect.width, rect.height));
            bandData.getSamples(rect.x, rect.y, rect.width, rect.height, 0, dArray);
            targetTile.setSamples(dArray);
        }else if(targetBandName.endsWith(CNT_SUFFIX)) {
            final float[] fArray = new float[rect.width * rect.height];
            final Raster bandData = imageMap.get(sourceRasterName).getData(new Rectangle(rect.x, rect.y, rect.width, rect.height));
            bandData.getSamples(rect.x, rect.y, rect.width, rect.height, 1, fArray);
            targetTile.setSamples(fArray);
        }else {
            throw new OperatorException(String.format("Unknown target band with name '%s'", targetBandName));
        }

    }

    private String getAvgTargetName(String rasterName) {
        return rasterName + AVG_SUFFIX;
    }

    private String getCntTargetName(String bandName) {
        return bandName + CNT_SUFFIX;
    }

    private void validateInput() {
        if(bandNames == null) {
            throw new OperatorException("Names of input bands are not provided");
        }
        if (windowSize % 2 == 0) {
            throw new OperatorException("Window size must be odd");
        }
        if (StringUtils.isNullOrEmpty(maskExpression)) {
            throw new OperatorException("No mask expression provided");
        }
        int[][] bandSizes = new int[bandNames.length][];
        for (int i = 0; i < bandNames.length; i++) {
            String bandName = bandNames[i];
            if (!sourceProduct.containsBand(bandName)) {
                throw new OperatorException(String.format("Source product does not contain a band with the name '%s'", bandName));
            }
            final Dimension bandSize = sourceProduct.getBand(bandName).getRasterSize();
            bandSizes[i] = new int[]{bandSize.width, bandSize.height};
        }
        final int[] bandSizeRef = bandSizes[0];
        for (int i = 1; i < bandSizes.length; i++) {
            int[] bandSize = bandSizes[i];
            if(!Arrays.equals(bandSize, bandSizeRef)) {
                throw new OperatorException(String.format("Size of bands must be equal. But found [%d,%d] and [%d,%d].",
                                                          bandSizeRef[0], bandSizeRef[1], bandSize[0], bandSize[1]));
            }
        }

        try {
            Parser parser = sourceProduct.createBandArithmeticParser();
            parser.parse(maskExpression);
        } catch (ParseException pe) {
            throw new OperatorException("The masked expression is not compatible with the source product", pe);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AvgOp.class);
        }
    }

}
