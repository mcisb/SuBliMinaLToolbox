package org.mcisb.subliminal.kegg;

import java.io.*;

/**
 * 
 * @author Neil Swainston
 */
public class KeggExtracterUtils extends KeggUtils
{
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.subliminal.kegg.KeggUtils#getTaxonomyId(java.lang.String)
	 */
	@Override
	public String getTaxonomyId( final String keggOrganismId ) throws IOException
	{
		final String regExp = "(?<=\\s" + keggOrganismId + "(,\\s[A-Z]{1,16})?,\\s)\\d+(?=[\\s;,].*)"; //$NON-NLS-1$ //$NON-NLS-2$
		return getValue( keggOrganismId, regExp );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.subliminal.kegg.KeggUtils#getOrganismName(java.lang.String)
	 */
	@Override
	public String getOrganismName( final String keggOrganismId ) throws IOException
	{
		final String regExp = "(?<=\\s" + keggOrganismId + "(,\\s[A-Z]{1,16})?,\\s\\d{1,16}[\\s;,]\\s).*(?=$)"; //$NON-NLS-1$ //$NON-NLS-2$
		return getValue( keggOrganismId, regExp );
	}
}