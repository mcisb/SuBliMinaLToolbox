package org.mcisb.subliminal.metacyc;

import java.util.*;
import java.util.regex.*;
import org.mcisb.subliminal.*;

/**
 * 
 * @author neilswainston
 */
public class MetaCycUtils
{
	/**
	 * 
	 */
	private static final int ENCODE_LENGTH = 2;

	/**
	 * 
	 */
	private static final String UNDERSCORE_PREFIX = "^_"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String ENCODED_CHAR = "(?<=__)\\d+(?=__)"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final Pattern encodeCharPattern = Pattern.compile( ENCODED_CHAR );

	/**
	 * 
	 */
	private static final Map<String,String> encodedCharToChar = new HashMap<>();

	/**
	 * 
	 * @param id
	 * @return String
	 */
	public static String unencode( final String id )
	{
		StringBuffer encodedId = new StringBuffer( id );

		Matcher matcher = encodeCharPattern.matcher( encodedId );

		while( matcher.find() )
		{
			final int start = matcher.start();
			final int end = matcher.end();
			final String encodedChar = encodedId.substring( start, end );
			encodedId.replace( start - ENCODE_LENGTH, end + ENCODE_LENGTH, getChar( encodedChar ) );
			matcher = encodeCharPattern.matcher( encodedId );
		}

		return encodedId.toString().replaceAll( UNDERSCORE_PREFIX, SubliminalUtils.EMPTY_STRING );
	}

	/**
	 * 
	 * @param encodedChar
	 * @return String
	 */
	private static String getChar( final String encodedChar )
	{
		String c = encodedCharToChar.get( encodedChar );

		if( c == null )
		{
			c = Character.valueOf( (char)Integer.parseInt( encodedChar ) ).toString();
			encodedCharToChar.put( encodedChar, c );
		}

		return c;
	}
}