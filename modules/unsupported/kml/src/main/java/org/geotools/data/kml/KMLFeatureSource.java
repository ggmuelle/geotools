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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.StreamingParser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Niels Charlier, Scitus Development
 * @source $URL$
 */
public class KMLFeatureSource extends ContentFeatureSource {

    private long lastReload;
    private KMLDataStore dataStore;
    private ArrayList<SimpleFeature> features;

    public KMLFeatureSource(ContentEntry entry, Query query) {
        super(entry, query);
        dataStore = (KMLDataStore) entry.getDataStore();
    }

    @Override
    protected QueryCapabilities buildQueryCapabilities() {
        return new QueryCapabilities() {
            public boolean isUseProvidedFIDSupported() {
                return true;
            }
        };
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        ReferencedEnvelope bounds =
                new ReferencedEnvelope(getSchema().getCoordinateReferenceSystem());

        FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReaderInternal(query);
        try {
            while (featureReader.hasNext()) {
                SimpleFeature feature = featureReader.next();
                bounds.include(feature.getBounds());
            }
        } finally {
            featureReader.close();
        }
        return bounds;
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        int count = 0;
        FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReaderInternal(query);
        try {
            while (featureReader.hasNext()) {
                featureReader.next();
                count++;
            }
        } finally {
            featureReader.close();
        }
        return count;
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        String typeName = getEntry().getTypeName();
        String namespace = getEntry().getName().getNamespaceURI();

        SimpleFeatureType type;
        FeatureReader<SimpleFeatureType, SimpleFeature> featureReader = getReaderInternal(query);
        try {
            type = featureReader.getFeatureType();
        } finally {
            featureReader.close();
        }

        // rename
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        if (type != null) {
            b.init(type);
        }
        b.setName(typeName);
        b.setNamespaceURI(namespace);
        return b.buildFeatureType();
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        reloadKmlFile();

        //        KMLDataStore dataStore = (KMLDataStore) getEntry().getDataStore();
        //        return new KMLFeatureReader(
        //                dataStore.getNamespaceURI(),
        //                dataStore.file,
        //                new QName(getEntry().getName().getNamespaceURI(),
        // getEntry().getTypeName()));
        return new KMLFeatureReader(features, this);
    }

    private void reloadKmlFile() throws IOException {
        File file = new File(dataStore.getInfo().getSource());
        if (lastReload == file.lastModified()) {
            return;
        }

        lastReload = file.lastModified();
        features = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file)) {
            StreamingParser parser =
                    new StreamingParser(new KMLConfiguration(), fis, KML.Placemark);
            SimpleFeature f = (SimpleFeature) parser.parse();
            while (f != null) {
                features.add(f);
                f = (SimpleFeature) parser.parse();
            }
        } catch (Exception e) {
            throw new IOException("Error processing KML file", e);
        }
    }
}
