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
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import org.mcisb.subliminal.*;

/**
 * @author Neil Swainston
 */
class MetaCycDownloader
{
	/**
	 * 
	 */
	private final static String TARBALL = "&nbsp;[Download tarball]&nbsp;"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final static String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * 
	 */
	private final URL source;

	/**
	 * 
	 */
	private final String organismName;

	/**
	 * 
	 */
	private final File destination;

	/**
	 * 
	 * @param directory
	 * @param organismName
	 * @return File
	 * @throws Exception
	 */
	public static File getMetaCycSource( final File metacycDirectory, final String organismName ) throws Exception
	{
		if( SubliminalUtils.find( metacycDirectory, "metabolic-reactions.sbml" ) == null ) //$NON-NLS-1$
		{
			final URL source = new URL( System.getProperty( "org.mcisb.subliminal.metacyc.MetaCycSource" ) ); //$NON-NLS-1$
			final String username = System.getProperty( "org.mcisb.subliminal.metacyc.MetaCycUsername" ); //$NON-NLS-1$
			final String password = System.getProperty( "org.mcisb.subliminal.metacyc.MetaCycPassword" ); //$NON-NLS-1$
			final MetaCycDownloader downloader = new MetaCycDownloader( source, username, password, organismName, metacycDirectory );
			downloader.doTask();
		}

		return metacycDirectory;
	}

	/**
	 * 
	 * @param source
	 * @param organismName
	 * @param destination
	 */
	private MetaCycDownloader( final URL source, final String username, final String password, final String organismName, final File destination )
	{
		this.source = source;
		this.organismName = organismName;
		this.destination = destination;

		Authenticator.setDefault( new PasswordAuthenticator( username, password ) );
	}

	/**
	 * 
	 * @return String[]
	 * @throws Exception
	 */
	protected String[] getOrganisms() throws Exception
	{
		final Collection<String> organisms = new LinkedHashSet<>();
		BufferedReader reader = null;

		try
		{
			// Install the custom authenticator
			reader = new BufferedReader( new InputStreamReader( source.openStream(), Charset.defaultCharset() ) );
			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				final String stripped = SubliminalUtils.stripTags( line );

				if( stripped.contains( TARBALL ) )
				{
					organisms.add( stripped.replace( TARBALL, EMPTY_STRING ).trim() );
				}
			}

			return organisms.toArray( new String[ organisms.size() ] );
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	protected Serializable doTask() throws Exception
	{
		final Collection<String> organisms = new LinkedHashSet<>();
		final String ZIP_REGEXP = "(?=.*)http://.*\\.tar.gz(?=.*)"; //$NON-NLS-1$
		BufferedReader reader = null;

		try
		{
			// Install the custom authenticator
			reader = new BufferedReader( new InputStreamReader( source.openStream(), Charset.defaultCharset() ) );
			String line = null;
			boolean found = false;

			while( ( line = reader.readLine() ) != null )
			{
				String organism = null;

				final String stripped = SubliminalUtils.stripTags( line );

				if( stripped.contains( TARBALL ) )
				{
					organism = stripped.replace( TARBALL, EMPTY_STRING ).trim();
				}

				if( organism != null )
				{
					organisms.add( organism );

					if( !found && ( organism.equals( organismName ) || organismName.contains( organism ) ) )
					{
						found = true;
					}

					if( found )
					{
						System.out.println( "MetaCyc: Downloading " + organism ); //$NON-NLS-1$
						final Matcher matcher = Pattern.compile( ZIP_REGEXP ).matcher( line );

						while( matcher.find() )
						{
							final String zipUrl = line.substring( matcher.start(), matcher.end() );
							SubliminalUtils.untar( new URL( zipUrl ), destination );
							return null;
						}
					}
				}
			}

			throw new UnsupportedOperationException( "MetaCyc data for " + organismName + " unavailable. Supported organisms are " + Arrays.toString( organisms.toArray( new String[ organisms.size() ] ) ) + "." ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}
	}

	/**
	 * 
	 * @author Neil Swainston
	 */
	private class PasswordAuthenticator extends Authenticator
	{
		/**
		 * 
		 */
		private final String username;

		/**
		 * 
		 */
		private final String password;

		/**
		 * 
		 * @param username
		 * @param password
		 */
		public PasswordAuthenticator( final String username, final String password )
		{
			this.username = username;
			this.password = password;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.net.Authenticator#getPasswordAuthentication()
		 */
		@Override
		protected PasswordAuthentication getPasswordAuthentication()
		{
			return new PasswordAuthentication( username, password.toCharArray() );
		}
	}
}