/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.kml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Read a KML file directly.
 *
 * @author Niels Charlier, Scitus Development
 * @author Gerd Müller-Schramm, Hexagon AB
 * @source $URL$
 */
public class KMLFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

    private KMLFeatureSource source;
    private ArrayList<SimpleFeature> features;
    private Iterator<SimpleFeature> iterator;

    public KMLFeatureReader(ArrayList<SimpleFeature> features, KMLFeatureSource source)
            throws IOException {
        this.features = new ArrayList<>(features);
        this.iterator = this.features.iterator();
        this.source = source;
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return source.getSchema();
    }

    /**
     * Grab the next feature from the property file.
     *
     * @return feature
     * @throws IOException
     * @throws NoSuchElementException Check hasNext() to avoid reading off the end of the file
     */
    @Override
    public SimpleFeature next() throws IOException, NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return iterator.next();
    }

    /**
     * @see FeatureReader#hasNext()
     */
    @Override
    public boolean hasNext() throws IOException {
        return iterator.hasNext();
    }

    /**
     * Be sure to call close when you are finished with this reader; as it must close the file it
     * has open.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        iterator = features.iterator();
    }
    
}
