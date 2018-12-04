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

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFactorySpi;

/**
 * DataStore factory that creates {@linkplain org.geotools.data.kml.KMLDataStore}s
 *
 * @author NielsCharlier, Scitus Development
 * @author Gerd Müller-Schramm, Hexagon AB
 * @source $URL$
 */
public class KMLDataStoreFactory implements FileDataStoreFactorySpi {

    public static final String[] EXTENSIONS = new String[] { ".kml" };
    
    public static final Param FILE = new Param("file", File.class, "Property file", true);

    public static final Param NAMESPACE =
            new Param("namespace", String.class, "namespace of datastore", false);

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        File file = fileLookup((File)FILE.lookUp(params));
        String namespaceURI = (String) NAMESPACE.lookUp(params);
        return new KMLDataStore(file, namespaceURI);
    }

    @Override
    public FileDataStore createDataStore( URL url ) throws IOException {
        try {
            Map<String, Serializable> params = new HashMap<>();
            params.put( FILE.key, new File(url.toURI()) );
            return (FileDataStore)createDataStore( params );
            
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        File file = fileLookup((File)FILE.lookUp(params));
        if (file.exists()) {
            throw new IOException(file + " already exists");
        }

        String namespaceURI = (String) NAMESPACE.lookUp(params);
        return new KMLDataStore(file, namespaceURI);
    }

    @Override
    public String getDisplayName() {
        return "KML Feature Store";
    }

    @Override
    public String getDescription() {
        return "Allows access to KML files containing Feature information (ignores styles)";
    }

    /**
     * @see #FILE
     * @see #NAMESPACE
     */
    @Override
    public Param[] getParametersInfo() {
        return new Param[] {FILE, NAMESPACE};
    }

    /**
     * Test to see if this datastore is available, if it has all the appropriate libraries to
     * construct a datastore. This datastore just returns true for now. This method is used for gui
     * apps, so as to not advertise data store capabilities they don't actually have.
     *
     * @return <tt>true</tt> if and only if this factory is available to create DataStores.
     * @task <code>true</code> property datastore is always available
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    @Override
    public String[] getFileExtensions() {
        return EXTENSIONS;
    }

    /**
     * Works for a file directory or property file
     *
     * @param params Connection parameters
     * @return true for connection parameters indicating a directory or property file
     */
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            fileLookup((File)FILE.lookUp( params ));
            return true;
        } catch (Exception erp) {
            // can't process, just return false
            return false;
        }
    }

    @Override
    public boolean canProcess( URL url ) {
        try {
            fileLookup(new File(url.toURI()));
            return true;
        } catch (Exception erp) {
            // can't process, just return false
            return false;
        }
    }

    @Override
    public String getTypeName( URL url ) throws IOException {
        DataStore ds = createDataStore( url );
        String[] names = ds.getTypeNames();
        assert names.length == 1 : "Invalid number of type names for KML file store";
        ds.dispose();
        return names[0];
    }
    
    /**
     * Lookups the property file in the params argument, and returns the corresponding <code>
     * java.io.File</code>.
     *
     * <p>The file is first checked for existence as an absolute path in the filesystem. If such a
     * directory is not found, then it is treated as a relative path, taking Java system property
     * <code>"user.dir"</code> as the base.
     *
     * @param params
     * @throws IllegalArgumentException if file is a directory.
     * @throws FileNotFoundException if directory does not exists
     * @throws IOException if {@linkplain #DIRECTORY} doesn't find parameter in <code>params</code>
     *     file does not exists.
     */
    private File fileLookup(File file)
            throws IOException, FileNotFoundException, IllegalArgumentException {
        if (!file.getName().endsWith( EXTENSIONS[0] )) {
            throw new IllegalArgumentException(
                    "File seems not be a KML file (wrong extension): " + file.getAbsolutePath());
        }
        
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IllegalArgumentException(
                        "File is required (not a directory): " + file.getAbsolutePath());
            }
            return file;
            
        } else {
            File dir = file.getParentFile();
            if (dir == null || !dir.exists()) {
                // quickly check if it exists relative to the user directory
                File currentDir = new File(System.getProperty("user.dir"));

                File file2 = new File(currentDir, file.getPath());
                if (file2.exists()) {
                    return file2;
                }
            }
            return file;
        }
    }

}
