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

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * @author Marco Peters
 */
public class RunAvgOpMain {

    public static void main(String[] args) throws IOException {
        SystemUtils.init3rdPartyLibs(RunAvgOpMain.class);
        final Product aatsr = ProductIO.readProduct("C:\\Users\\Marco\\Projects\\snap-average-tool\\src\\test\\resources\\org\\esa\\snap\\avg\\testSrc.dim");

        Instant start = Instant.now();

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("bandNames", "Oa04_radiance, Oa10_radiance");
        parameters.put("windowSize", "3");
        parameters.put("maskExpression", "geometry");
        final Product shadowProduct = GPF.createProduct("AvgTool", parameters, aatsr);
        ProductIO.writeProduct(shadowProduct, "H:\\_temp\\avg\\testAvgOutput.dim", "BEAM-DIMAP");
        Instant stop = Instant.now();
        SystemUtils.LOG.log(Level.INFO, "DURATION: " + Duration.between(start, stop));
    }
}
