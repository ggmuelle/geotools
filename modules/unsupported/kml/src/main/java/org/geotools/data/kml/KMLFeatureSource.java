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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Encoder;
import org.geotools.xml.PullParser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Represents the content of one KML-file. The feature type of this source is fixed, i.e. it has always
 * the same attribute list based on the KML specification. Because of this we can read the feature type
 * by parsing a template KML file while constructing the KMLFeatureSource and don't need to change it
 * ever again.
 * 
 * @author Niels Charlier, Scitus Development
 * @author Gerd Müller-Schramm, Hexagon AB
 * @source $URL$
 */
public class KMLFeatureSource extends ContentFeatureStore implements SimpleFeatureSource {

    private long lastReload;
    private KMLDataStore dataStore;
    private SimpleFeatureType featureType;
    private ArrayList<SimpleFeature> features;

    public KMLFeatureSource(ContentEntry entry, Query query) {
        super(entry, query);
        dataStore = (KMLDataStore) entry.getDataStore();
        features = new ArrayList<>();
        buildFeatureType();
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
        return features.size();
    }

    @Override
    protected SimpleFeatureType buildFeatureType() {
        if (featureType == null) {
            String typeName = getEntry().getTypeName();
            String namespace = getEntry().getName().getNamespaceURI();

            // rename
            SimpleFeatureType type = readFeatureTypeTemplate();
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.init(type);
            b.setName(typeName);
            b.setNamespaceURI(namespace);
            b.setDefaultGeometry( type.getGeometryDescriptor().getLocalName() );
            featureType = b.buildFeatureType();
        }
        
        return featureType;
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        reloadKmlFile();
        return new KMLFeatureReader(features, this);
    }

    @Override
    protected FeatureWriter<SimpleFeatureType, SimpleFeature> getWriterInternal( Query query, int flags )
            throws IOException {
        reloadKmlFile();
        return new KMLFeatureWriter(features, this);
    }
    
    void addFeature(SimpleFeature feature) {
        features.add( feature );
    }

    void removeFeature(SimpleFeature feature) {
        features.remove( feature );
    }

    private void reloadKmlFile() throws IOException {
        File file = new File(dataStore.getInfo().getSource());
        if (!file.exists()) {
            return;
        }
        
        if (lastReload == file.lastModified()) {
            return;
        }

        lastReload = file.lastModified();
        features = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file)) {
            PullParser parser = new PullParser(new KMLConfiguration(), fis, KML.Placemark);
            SimpleFeature f = (SimpleFeature) parser.parse();
            while (f != null) {
                features.add(f);
                f = (SimpleFeature) parser.parse();
            }
        } catch (Exception e) {
            throw new IOException("Error processing KML file", e);
        }
    }
    
    private SimpleFeatureType readFeatureTypeTemplate() {
        try (InputStream fis = getClass().getResourceAsStream( "template.kml" )) {
            PullParser parser = new PullParser(new KMLConfiguration(), fis, KML.Placemark);
            SimpleFeature f = (SimpleFeature) parser.parse();
            return f.getFeatureType();
            
        } catch (Exception e) {
            throw new RuntimeException("Error processing KML template file", e);
        }
    }
    
    void writeKmlFile() throws IOException {
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        featureCollection.addAll( features );

        File file = new File(dataStore.getInfo().getSource());
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            Encoder encoder = new Encoder(new KMLConfiguration());
            encoder.setIndenting(true);
            encoder.encode(featureCollection, KML.kml, out);
        }
        
        lastReload = file.lastModified();
    }

}
