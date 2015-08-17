/**
 * 
 */
package org.mcisb.subliminal.shorthand;

import java.io.*;
import java.util.*;

/**
 * @author Neil Swainston
 */
public abstract class FileUtils
{
	/**
	 * 
	 * @param file
	 * @return List<String>
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static List<String> readList( final File file ) throws FileNotFoundException, IOException
	{
		final List<String> values = new ArrayList<>();

		try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
		{
			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				if( line.startsWith( "#" ) || line.trim().length() == 0 ) //$NON-NLS-1$
				{
					continue;
				}

				values.add( line.trim() );
			}
		}

		return values;
	}

	/**
	 * 
	 * @param file
	 * @return Map<String,String>
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static Map<String,String> readMap( final File file ) throws FileNotFoundException, IOException
	{
		final int KEY = 0;
		final int VALUE = 1;
		final Map<String,String> values = new LinkedHashMap<>();

		try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
		{
			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				if( line.startsWith( "#" ) || line.trim().length() == 0 ) //$NON-NLS-1$
				{
					continue;
				}

				final String[] tokens = line.trim().split( "\\t" ); //$NON-NLS-1$
				values.put( tokens[ KEY ], tokens.length > VALUE ? tokens[ VALUE ] : null );
			}
		}

		return values;
	}

	/**
	 * 
	 * @param file
	 * @return List<String[]>
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static List<String[]> readLists( final File file ) throws FileNotFoundException, IOException
	{
		final List<String[]> values = new ArrayList<>();

		try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
		{
			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				if( line.startsWith( "#" ) || line.trim().length() == 0 ) //$NON-NLS-1$
				{
					continue;
				}

				values.add( line.trim().split( "\\t" ) ); //$NON-NLS-1$
			}
		}

		return values;
	}
}