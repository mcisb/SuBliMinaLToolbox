package org.mcisb.subliminal.mnxref;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 
 * @author Neil Swainston
 */
public class MxnRefReactionUtils extends MxnRefUtils
{
	/**
	 * 
	 */
	private static final int REACTION = 1;

	/**
	 * 
	 */
	private static final int DESCRIPTION = 2;

	/**
	 * 
	 */
	private static final int BALANCE = 3;

	/**
	 * 
	 */
	private static final int EC = 4;

	/**
	 * 
	 */
	private static final String MXNREF_ID_REG_EXP = "MNXR[0-9]+"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String EC_SEPARATOR = ";"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final URL dataUrl = new URL( "http://metanetx.org/cgi-bin/mnxget/mnxref/reac_prop.tsv" ); //$NON-NLS-1$

	/**
	 * 
	 */
	private static MxnRefReactionUtils instance = null;

	/**
	 * 
	 * @return MxnRefChemUtils
	 * @throws MalformedURLException
	 */
	public static MxnRefReactionUtils getInstance() throws MalformedURLException
	{
		if( instance == null )
		{
			instance = new MxnRefReactionUtils();
		}

		return instance;
	}

	/**
	 * 
	 * @throws MalformedURLException
	 */
	private MxnRefReactionUtils() throws MalformedURLException
	{
		super( MXNREF_ID_REG_EXP, new URL( "http://metanetx.org/cgi-bin/mnxget/mnxref/reac_xref.tsv" ) ); //$NON-NLS-1$
	}

	/**
	 * 
	 * @param id
	 * @return String
	 * @throws IOException
	 */
	public synchronized String getEquation( final String id ) throws IOException
	{
		return getData( id, REACTION, dataUrl );
	}

	/**
	 * 
	 * @param id
	 * @return String
	 * @throws IOException
	 */
	public synchronized String getDescription( final String id ) throws IOException
	{
		return getData( id, DESCRIPTION, dataUrl );
	}

	/**
	 * 
	 * @param id
	 * @return boolean
	 * @throws IOException
	 */
	public synchronized boolean getBalance( final String id ) throws IOException
	{
		final String balance = getData( id, BALANCE, dataUrl );

		if( balance != null )
		{
			return Boolean.parseBoolean( balance );
		}

		return false;
	}

	/**
	 * 
	 * @param id
	 * @return Collection<String>
	 * @throws IOException
	 */
	public synchronized Collection<String> getEC( final String id ) throws IOException
	{
		final Collection<String> ecTerms = new ArrayList<>();
		final String data = getData( id, EC, dataUrl );

		if( data != null )
		{
			for( String ecTerm : data.split( EC_SEPARATOR ) )
			{
				ecTerms.add( ecTerm );
			}
		}

		return ecTerms;
	}
}