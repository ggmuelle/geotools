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
import java.util.UUID;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Gerd Müller-Schramm, Hexagon AB
 */
public class KMLFeatureWriter implements FeatureWriter<SimpleFeatureType, SimpleFeature> {

    private KMLFeatureSource source;
    private Iterator<SimpleFeature> iterator;
    private ArrayList<SimpleFeature> features;
    protected SimpleFeature currentFeature;


    public KMLFeatureWriter( ArrayList<SimpleFeature> features, KMLFeatureSource featureSource ) {
        this.features = new ArrayList<>(features);
        this.iterator = this.features.iterator();
        this.source = featureSource;
    }


    @Override
    public SimpleFeatureType getFeatureType() {
        return source.getSchema();
    }


    @Override
    public boolean hasNext() throws IOException {
        return iterator.hasNext();
    }


    @Override
    public SimpleFeature next() throws IOException {
        if (hasNext()) {
            return (currentFeature = iterator.next());
        }
        
        return (currentFeature = DataUtilities.template(getFeatureType(), 
                UUID.randomUUID().toString(), 
                new Object[source.getSchema().getAttributeCount()]));
    }


    @Override
    public void remove() throws IOException {
        if (currentFeature == null) {
            throw new IOException( "Current feature is null" );
        }

        source.removeFeature( currentFeature );
        currentFeature = null;
    }


    @Override
    public void write() throws IOException {
        if (currentFeature == null) {
            throw new IOException( "Current feature is null" );
        }
        
        source.addFeature( currentFeature );
    }


    @Override
    public void close() throws IOException {
        iterator = features.iterator();
        source.writeKmlFile();
    }

}
