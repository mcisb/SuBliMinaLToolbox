/*******************************************************************************
 * Manchester Institute of Biotechnology
 * University of Manchester
 * Manchester M1 7ND
 * United Kingdom
 * 
 * Copyright (C) 2013 University of Manchester
 * 
 * This program is released under the Academic Free License ("AFL") v3.0.
 * (http://www.opensource.org/licenses/academic.php)
 *******************************************************************************/
package org.mcisb.subliminal.kegg;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import javax.xml.namespace.*;
import javax.xml.stream.events.*;
import org.mcisb.subliminal.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class KeggExtracter extends AbstractKeggExtracter
{
	/**
	 * 
	 */
	public KeggExtracter()
	{
		super( new KeggExtracterUtils() );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.subliminal.kegg.AbstractKeggExtracter#getPathways(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	protected List<String> getPathways( final String keggOrganismId, final String organismName ) throws IOException
	{
		final String PATH_PREFIX = "path:"; //$NON-NLS-1$
		final int ID_INDEX = 0;

		final List<String> pathways = new ArrayList<>();

		try ( final BufferedReader reader = new BufferedReader( new InputStreamReader( new URL( "http://rest.kegg.jp/list/pathway/" + keggOrganismId ).openStream(), Charset.defaultCharset() ) ) ) //$NON-NLS-1$
		{
			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				final String[] tokens = line.split( "\t" ); //$NON-NLS-1$
				pathways.add( tokens[ ID_INDEX ].replace( PATH_PREFIX, SubliminalUtils.EMPTY_STRING ) );
			}
		}
		catch( FileNotFoundException e )
		{
			throw new UnsupportedOperationException( "KEGG: pathway data unavailable for " + ( organismName == null ? keggOrganismId : organismName ) + " (" + e.getMessage() + ")" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return pathways;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.subliminal.kegg.AbstractKeggExtracter#addPathway(org.sbml.jsbml
	 * .Model, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	protected void addPathway( final Model model, final String pathway, final String keggOrganismId, final String taxonomyId ) throws Exception
	{
		parseKgml( model, new URL( "http://www.genome.jp/kegg-bin/download?entry=" + pathway + "&format=kgml" ), keggOrganismId, taxonomyId ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.subliminal.kegg.AbstractKeggExtracter#parseEntry(javax.xml.
	 * stream.events.StartElement, org.sbml.jsbml.Model, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	protected void parseEntry( final StartElement startElement, final Model model, final String keggOrganismId, final String taxonomyId ) throws Exception
	{
		String reaction = null;
		String name = null;
		String type = null;

		for( Iterator<?> iterator = startElement.getAttributes(); iterator.hasNext(); )
		{
			final Attribute attribute = (Attribute)iterator.next();

			if( attribute.getName().equals( new QName( "reaction" ) ) ) //$NON-NLS-1$
			{
				reaction = attribute.getValue();
			}
			else if( attribute.getName().equals( new QName( "name" ) ) ) //$NON-NLS-1$
			{
				name = attribute.getValue();
			}
			else if( attribute.getName().equals( new QName( "type" ) ) ) //$NON-NLS-1$
			{
				type = attribute.getValue();
			}
		}

		if( type != null && reaction != null && type.equals( "gene" ) ) //$NON-NLS-1$
		{
			for( String reactionToken : reaction.split( "\\s+" ) ) //$NON-NLS-1$
			{
				addReaction( model, getId( reactionToken ), name, keggOrganismId, taxonomyId );
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.subliminal.kegg.AbstractKeggExtracter#getUniProtSearchTerm(
	 * java.lang.String)
	 */
	@Override
	protected String getUniProtSearchTerm( final String keggGeneId )
	{
		return keggGeneId;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		final String id = args[ 0 ];
		final boolean isKeggOrganismId = !id.matches( "\\d+" ); //$NON-NLS-1$
		new KeggExtracter().run( id, new File( args[ 1 ] ), isKeggOrganismId );
	}
}