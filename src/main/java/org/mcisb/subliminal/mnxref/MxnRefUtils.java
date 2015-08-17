package org.mcisb.subliminal.mnxref;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.lang3.*;
import org.mcisb.subliminal.*;

/**
 * 
 * @author Neil Swainston
 */
public abstract class MxnRefUtils
{
	/**
	 * 
	 * @author Neil Swainston
	 * 
	 */
	public enum Evidence
	{
		identity, inferred, structural
	}

	/**
	 * 
	 */
	private static final int XREF_ID_SOURCE = 0;

	/**
	 * 
	 */
	private static final int XREF_ID_VALUE = 1;

	/**
	 * 
	 */
	private final String mxnRefIdRegExp;

	/**
	 * 
	 */
	private final URL xrefUrl;

	/**
	 * 
	 */
	protected Map<String,Map<String,Collection<String>>> mxnRefIdToXrefIds = null;

	/**
	 * 
	 */
	private Map<String,String[]> mxnRefIdToData = null;

	/**
	 * 
	 */
	private Map<String,String> xrefIdToMxnRefId = null;

	/**
	 * 
	 */
	private Map<String,Evidence> xrefIdToEvidence = null;

	/**
	 * 
	 * @param mxnRefIdRegExp
	 * @param xrefUrl
	 */
	protected MxnRefUtils( final String mxnRefIdRegExp, final URL xrefUrl )
	{
		this.mxnRefIdRegExp = mxnRefIdRegExp;
		this.xrefUrl = xrefUrl;
	}

	/**
	 * 
	 * @param id
	 * @return String
	 * @throws IOException
	 */
	public synchronized String getMxnRefId( final String id ) throws IOException
	{
		if( id.matches( mxnRefIdRegExp ) )
		{
			return id;
		}

		if( xrefIdToMxnRefId == null )
		{
			initXrefs();
		}

		return xrefIdToMxnRefId.get( SubliminalUtils.getNormalisedId( id ) );
	}

	/**
	 * 
	 * @param id
	 * @return Map<String,Collection<String>>
	 * @throws IOException
	 */
	public synchronized Map<String,Collection<String>> getXrefIds( final String id ) throws IOException
	{
		final String mxnRefId = getMxnRefId( id );

		if( mxnRefIdToXrefIds == null )
		{
			initXrefs();
		}

		return mxnRefIdToXrefIds.get( mxnRefId );
	}

	/**
	 * 
	 * @param id
	 * @return Collection<String>
	 * @throws IOException
	 */
	public synchronized Collection<String> getXrefIds( final String id, final String source ) throws IOException
	{
		final Map<String,Collection<String>> xrefIds = getXrefIds( id );

		if( xrefIds == null )
		{
			return new ArrayList<>();
		}

		final Collection<String> sourceXrefIds = xrefIds.get( source );

		return sourceXrefIds == null ? new ArrayList<String>() : sourceXrefIds;
	}

	/**
	 * 
	 * @param xrefId
	 * @return Evidence
	 * @throws IOException
	 */
	public synchronized Evidence getEvidence( final String xrefId ) throws IOException
	{
		if( xrefIdToMxnRefId == null )
		{
			initXrefs();
		}

		return xrefIdToEvidence.get( SubliminalUtils.getNormalisedId( xrefId ) );
	}

	/**
	 * 
	 * @param id
	 * @param column
	 * @param dataUrl
	 * @return String
	 * @throws IOException
	 */
	protected synchronized String getData( final String id, final int column, final URL dataUrl ) throws IOException
	{
		if( mxnRefIdToData == null )
		{
			initMxnRefIdToData( dataUrl );
		}

		final String mxnRefId = getMxnRefId( id );

		if( mxnRefId == null )
		{
			return null;
		}

		final String[] data = mxnRefIdToData.get( mxnRefId );

		if( data != null )
		{
			return data[ column ];
		}

		return null;
	}

	/**
	 * 
	 * @throws IOException
	 */
	protected void initXrefs() throws IOException
	{
		final int ID = 0;
		final int MXNREF_ID = 1;
		final int EVIDENCE_ID = 2;

		try ( final InputStream is = xrefUrl.openStream(); final BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) )
		{
			mxnRefIdToXrefIds = new TreeMap<>();
			xrefIdToMxnRefId = new TreeMap<>();
			xrefIdToEvidence = new TreeMap<>();

			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				if( !line.startsWith( "#" ) ) //$NON-NLS-1$
				{
					String[] tokens = line.split( "\t" ); //$NON-NLS-1$
					final String xrefId = tokens[ ID ];
					final String normalisedXRefId = SubliminalUtils.getNormalisedId( xrefId );
					final String mnxRefId = tokens[ MXNREF_ID ];

					xrefIdToMxnRefId.put( normalisedXRefId, mnxRefId );

					if( tokens.length > EVIDENCE_ID )
					{
						final Evidence evidence = Evidence.valueOf( tokens[ EVIDENCE_ID ] );
						xrefIdToEvidence.put( normalisedXRefId, evidence );
					}

					Map<String,Collection<String>> xrefIds = mxnRefIdToXrefIds.get( mnxRefId );

					if( xrefIds == null )
					{
						xrefIds = new LinkedHashMap<>();
						mxnRefIdToXrefIds.put( mnxRefId, xrefIds );
					}

					final String[] xrefIdTokens = xrefId.split( ":" ); //$NON-NLS-1$
					final String xrefIdSource = xrefIdTokens[ XREF_ID_SOURCE ];
					final String xrefIdValue = xrefIdTokens[ XREF_ID_VALUE ];

					Collection<String> xrefIdValues = xrefIds.get( xrefIdSource );

					if( xrefIdValues == null )
					{
						xrefIdValues = new LinkedHashSet<>();
						xrefIds.put( xrefIdSource, xrefIdValues );
					}

					xrefIdValues.add( xrefIdValue );
				}
			}
		}
	}

	/**
	 * 
	 * @param url
	 * @throws IOException
	 */
	private void initMxnRefIdToData( final URL url ) throws IOException
	{
		final int ID = 0;

		try ( final InputStream is = url.openStream(); final BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) )
		{
			mxnRefIdToData = new TreeMap<>();

			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				if( !line.startsWith( "#" ) ) //$NON-NLS-1$
				{
					final String[] tokens = getValidXml( line ).split( "\t" ); //$NON-NLS-1$
					mxnRefIdToData.put( tokens[ ID ], tokens );
				}
			}
		}
	}

	/**
	 * 
	 * @param input
	 * @return String
	 */
	private static String getValidXml( final String input )
	{
		final char UNDERSCORE = '_';
		final int MAX_VALUE = 127;
		final char[] output = input.toCharArray();

		for( int i = 0; i < output.length; i++ )
		{
			final char c = input.charAt( i );

			if( c > MAX_VALUE )
			{
				output[ i ] = UNDERSCORE;
			}
		}

		return StringEscapeUtils.escapeXml10( new String( output ) );
	}
}