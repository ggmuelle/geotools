/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotools.referencing.factory;

import java.util.HashMap;
import org.geotools.factory.Hints;
import org.geotools.metadata.iso.IdentifierImpl;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.metadata.iso.citation.ResponsiblePartyImpl;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.util.Version;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.referencing.NoSuchAuthorityCodeException;

/**
 * A AuthorityFactory to match authority "urn:adv:crs".
 *
 * @since 19.0
 * @source $URL$
 * @version $Id$
 * @author Gerd Müller-Schramm, 2019
 */
public class Adv_AuthorityFactory extends Abstract_URI_AuthorityFactory {

    private static final String ADV_AUTHORITY = "urn:adv:crs";

    /** The name used in {@link Hints#FORCE_AXIS_ORDER_HONORING} for this factory. */
    public static final String HINTS_AUTHORITY = "urn";

    public static final Citation URN_ADV;

    private static final HashMap<String, String> epsgCodeMapping = new HashMap<>();

    static {
        final CitationImpl c = new CitationImpl("URN in ADV namespace");
        c.getIdentifiers().add(new IdentifierImpl(ADV_AUTHORITY));
        c.getCitedResponsibleParties().add(ResponsiblePartyImpl.OGC);
        c.getPresentationForm().add(PresentationForm.DOCUMENT_DIGITAL);
        c.freeze();
        URN_ADV = c;

        // TODO: add more mapping based on
        // http://www.adv-online.de/AAA-Modell/Dokumente-der-GeoInfoDok/GeoInfoDok-5.1/binarywriterservlet?imgUid=62370a7d-753b-8a01-e1f4-351ec0023010&uBasVariant=11111111-1111-1111-1111-111111111111&isDownload=true
        epsgCodeMapping.put("ETRS89_UTM31", "25831");
        epsgCodeMapping.put("ETRS89_UTM32", "25832");
        epsgCodeMapping.put("ETRS89_UTM33", "25833");
    }

    /**
     * Constructor.
     *
     * @see
     *     org.geotools.referencing.factory.Abstract_URI_AuthorityFactory#Abstract_URI_AuthorityFactory(String)
     */
    public Adv_AuthorityFactory() {
        super(HINTS_AUTHORITY);
    }

    /**
     * Constructor.
     *
     * @see
     *     org.geotools.referencing.factory.Abstract_URI_AuthorityFactory#Abstract_URI_AuthorityFactory(Hints,
     *     String)
     */
    public Adv_AuthorityFactory(Hints userHints) {
        super(userHints, HINTS_AUTHORITY);
    }

    /**
     * Constructor.
     *
     * @see
     *     org.geotools.referencing.factory.Abstract_URI_AuthorityFactory#Abstract_URI_AuthorityFactory(AllAuthoritiesFactory)
     */
    public Adv_AuthorityFactory(AllAuthoritiesFactory factory) {
        super(factory);
    }

    /** @see org.geotools.referencing.factory.Abstract_URI_AuthorityFactory#getAuthority() */
    @Override
    public Citation getAuthority() {
        return URN_ADV;
    }

    /**
     * @see
     *     org.geotools.referencing.factory.Abstract_URI_AuthorityFactory#buildParser(java.lang.String)
     */
    @Override
    protected URI_Parser buildParser(String urn) throws NoSuchAuthorityCodeException {
        final String code = urn.trim();
        if (code.toLowerCase().startsWith(ADV_AUTHORITY)) {
            String advCode = code.substring(ADV_AUTHORITY.length() + 1);
            String epsgCode = epsgCodeMapping.get(advCode);

            if (epsgCode != null) {
                return new URN_Parser(urn, URI_Type.get("crs"), "EPSG", new Version(""), epsgCode);
            }
        }

        throw new NoSuchAuthorityCodeException(
                Errors.format(ErrorKeys.ILLEGAL_IDENTIFIER_$1, "CRS"), ADV_AUTHORITY, "CRS");
    }
}
