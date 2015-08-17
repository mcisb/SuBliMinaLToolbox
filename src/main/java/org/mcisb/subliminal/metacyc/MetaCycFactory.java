/*******************************************************************************
 * Manchester Centre for Integrative Systems Biology
 * University of Manchester
 * Manchester M1 7ND
 * United Kingdom
 * 
 * Copyright (C) 2008 University of Manchester
 * 
 * This program is released under the Academic Free License ("AFL") v3.0.
 * (http://www.opensource.org/licenses/academic.php)
 *******************************************************************************/
package org.mcisb.subliminal.metacyc;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import org.mcisb.subliminal.*;

/**
 * @author Neil Swainston
 */
class MetaCycFactory
{
	/**
	 * 
	 */
	public static final String UNIQUE_ID = "UNIQUE-ID - "; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String ENZYMATIC_REACTION = "ENZYMATIC-REACTION - "; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String EC_NUMBER = "EC-NUMBER - "; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String ORGANISM_ID = "ORGID"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String ENZRXN = "ENZRXN"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final int ID = 0;

	/**
	 * 
	 */
	private static final int VALUE = 1;

	/**
	 * 
	 */
	private static final int ENZYMES = 19;

	/**
	 * 
	 */
	private static final int ENZYME_ID = 1;

	/**
	 * 
	 */
	private static final int STOICHIOMETRY = 0;

	/**
	 * 
	 */
	private final Map<String,Collection<String>> reactionIdToEnzymaticReactionIds = new HashMap<>();

	/**
	 * 
	 */
	private final Map<String,Collection<String>> reactionIdToEcNumbers = new HashMap<>();

	/**
	 * 
	 */
	private final Map<String,Map<String,Integer>> enzymaticReactionIdToEnzymeIds = new HashMap<>();

	/**
	 * 
	 */
	private final File versionFile;

	/**
	 * 
	 */
	private final File reactionsFile;

	/**
	 * 
	 */
	private final File enzymesFile;

	/**
	 * 
	 */
	private boolean initialised = false;

	/**
	 * 
	 */
	private String organismId;

	/**
	 * 
	 * @param versionFile
	 * @param reactionsFile
	 * @param enzymesFile
	 */
	public MetaCycFactory( final File versionFile, final File reactionsFile, final File enzymesFile )
	{
		this.versionFile = versionFile;
		this.reactionsFile = reactionsFile;
		this.enzymesFile = enzymesFile;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getOrganismId() throws IOException
	{
		if( !initialised )
		{
			init();
		}

		return organismId;
	}

	/**
	 * 
	 * @param id
	 * @return Collection<String>
	 * @throws Exception
	 */
	public synchronized Collection<String> getEcNumbers( final String reactionId ) throws IOException
	{
		if( !initialised )
		{
			init();
		}

		return reactionIdToEcNumbers.get( MetaCycUtils.unencode( reactionId ) );
	}

	/**
	 * 
	 * @param id
	 * @return Map<String,Integer>
	 * @throws Exception
	 */
	public synchronized Map<String,Integer> getEnzymes( final String reactionId ) throws IOException
	{
		if( !initialised )
		{
			init();
		}

		final Map<String,Integer> enzymes = new LinkedHashMap<>();

		final Collection<String> enzymaticReactionIds = reactionIdToEnzymaticReactionIds.get( MetaCycUtils.unencode( reactionId ) );

		if( enzymaticReactionIds != null )
		{
			for( String enzymaticReactionId : enzymaticReactionIds )
			{
				final Map<String,Integer> thisEnzymes = enzymaticReactionIdToEnzymeIds.get( enzymaticReactionId );

				if( thisEnzymes != null )
				{
					enzymes.putAll( thisEnzymes );
				}
			}
		}

		return enzymes;
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private void init() throws IOException
	{
		parseVersion();
		parseReactions();
		parseEnzymes();
		initialised = true;
	}

	/**
	 * 
	 * @throws IOException
	 */
	private void parseVersion() throws IOException
	{
		try ( final BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( versionFile ), Charset.defaultCharset() ) ) )
		{
			if( versionFile != null )
			{
				String line = null;

				while( ( line = reader.readLine() ) != null )
				{
					if( line.startsWith( ORGANISM_ID ) )
					{
						organismId = line.split( "\\t" )[ VALUE ]; //$NON-NLS-1$
						return;
					}
				}
			}
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	private void parseReactions() throws IOException
	{
		Collection<String> enzymaticReactionIds = null;
		Collection<String> ecNumbers = null;

		if( reactionsFile != null )
		{
			try ( final BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( reactionsFile ), Charset.defaultCharset() ) ) )
			{
				String line = null;

				while( ( line = reader.readLine() ) != null )
				{
					if( line.startsWith( UNIQUE_ID ) )
					{
						enzymaticReactionIds = new LinkedHashSet<>();
						reactionIdToEnzymaticReactionIds.put( line.replaceAll( UNIQUE_ID, SubliminalUtils.EMPTY_STRING ), enzymaticReactionIds );

						ecNumbers = new LinkedHashSet<>();
						reactionIdToEcNumbers.put( line.replaceAll( UNIQUE_ID, SubliminalUtils.EMPTY_STRING ), ecNumbers );
					}
					else if( line.startsWith( ENZYMATIC_REACTION ) )
					{
						final String[] terms = line.replaceAll( ENZYMATIC_REACTION, SubliminalUtils.EMPTY_STRING ).split( "\\s|\\)" ); //$NON-NLS-1$

						if( enzymaticReactionIds != null )
						{
							enzymaticReactionIds.add( terms[ ID ] );
						}
					}
					else if( line.startsWith( EC_NUMBER ) )
					{
						final String[] terms = line.replaceAll( EC_NUMBER, SubliminalUtils.EMPTY_STRING ).split( "\\s|\\)" ); //$NON-NLS-1$

						if( ecNumbers != null )
						{
							ecNumbers.add( terms[ ID ] );
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	private void parseEnzymes() throws IOException
	{
		try ( final BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( enzymesFile ), Charset.defaultCharset() ) ) )
		{
			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				if( line.startsWith( ENZRXN ) )
				{
					final String[] terms = line.split( "\\t" ); //$NON-NLS-1$
					final Map<String,Integer> enzymeIds = new LinkedHashMap<>();

					if( terms.length > ENZYMES )
					{
						final String[] enzymes = terms[ ENZYMES ].split( "," ); //$NON-NLS-1$

						for( String enzyme : enzymes )
						{
							final String[] enzymeTerms = enzyme.split( "\\*" ); //$NON-NLS-1$

							if( enzymeTerms.length > ENZYME_ID )
							{
								enzymeIds.put( enzymeTerms[ ENZYME_ID ], Integer.valueOf( enzymeTerms[ STOICHIOMETRY ] ) );
							}
						}

						enzymaticReactionIdToEnzymeIds.put( terms[ ID ], enzymeIds );
					}
				}
			}
		}
	}
}