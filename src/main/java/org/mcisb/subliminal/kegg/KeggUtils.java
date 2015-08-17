package org.mcisb.subliminal.kegg;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

/**
 * 
 * @author Neil Swainston
 */
public abstract class KeggUtils
{
	/**
	 * 
	 */
	private final static String KEGG_ORGANISM_ID_REGEXP = "(?<=\\t)[a-z]+(?=[\\t])"; //$NON-NLS-1$

	/**
	 * 
	 * @param keggOrganismId
	 * @return boolean
	 */
	public static boolean isTNumber( final String keggOrganismId )
	{
		final String KEGG_T_NUMBER_REGEXP = "T\\d{5}"; //$NON-NLS-1$
		return keggOrganismId.matches( KEGG_T_NUMBER_REGEXP );
	}

	/**
	 * 
	 * @return String[]
	 * @throws IOException
	 */
	public static String[] getOrganismIds() throws IOException
	{
		final Collection<String> organismIds = new TreeSet<>();

		final BufferedReader reader = new BufferedReader( new InputStreamReader( new URL( "http://rest.kegg.jp/list/organism" ).openStream(), Charset.defaultCharset() ) ); //$NON-NLS-1$
		String line = null;

		while( ( line = reader.readLine() ) != null )
		{
			final Matcher matcher = Pattern.compile( KEGG_ORGANISM_ID_REGEXP ).matcher( line );

			while( matcher.find() )
			{
				organismIds.add( line.substring( matcher.start(), matcher.end() ) );
			}
		}

		return organismIds.toArray( new String[ organismIds.size() ] );
	}

	/**
	 * 
	 * @param taxonomyId
	 * @return String
	 * @throws IOException
	 */
	public static String getKeggOrganismId( final String taxonomyId ) throws IOException
	{
		final String regExp = "(?<=\\s)[a-z]+(?=(,\\s[A-Z]{1,16})?,\\s" + taxonomyId + ";)"; //$NON-NLS-1$ //$NON-NLS-2$
		return getValue( taxonomyId, regExp );
	}

	/**
	 * 
	 * @param keggOrganismId
	 * @return String
	 * @throws IOException
	 */
	public abstract String getTaxonomyId( final String keggOrganismId ) throws IOException;

	/**
	 * 
	 * @param keggOrganismId
	 * @return String
	 * @throws IOException
	 */
	public abstract String getOrganismName( final String keggOrganismId ) throws IOException;

	/**
	 * 
	 * @param searchTerm
	 * @param regExp
	 * @return String
	 * @throws IOException
	 */
	protected static String getValue( final String searchTerm, final String regExp ) throws IOException
	{
		final BufferedReader reader = new BufferedReader( new InputStreamReader( new URL( "http://rest.kegg.jp/find/genome/" + searchTerm ).openStream(), Charset.defaultCharset() ) ); //$NON-NLS-1$
		String line = null;

		while( ( line = reader.readLine() ) != null )
		{
			final Matcher matcher = Pattern.compile( regExp ).matcher( line );

			while( matcher.find() )
			{
				return line.substring( matcher.start(), matcher.end() );
			}
		}

		return null;
	}
}