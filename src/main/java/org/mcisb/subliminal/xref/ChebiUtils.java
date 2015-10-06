/**
 * 
 */
package org.mcisb.subliminal.xref;

import java.io.*;
import java.text.*;
import uk.ac.manchester.libchebi.*;

/**
 * @author neilswainston
 * 
 */
public class ChebiUtils
{
	/**
	 * 
	 * @param id
	 * @return boolean
	 * @throws IOException
	 * @throws ParseException
	 * @throws NumberFormatException
	 * @throws ChebiException
	 */
	public static synchronized boolean isPrimary( final String id ) throws IOException, NumberFormatException, ParseException, ChebiException
	{
		return new ChebiEntity( Integer.parseInt( id ) ).getParentId() == ChebiEntity.UNDEFINED_VALUE;
	}
}