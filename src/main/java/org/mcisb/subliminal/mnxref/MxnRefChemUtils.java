package org.mcisb.subliminal.mnxref;

import java.io.*;
import java.net.*;
import org.mcisb.subliminal.*;

/**
 * 
 * @author Neil Swainston
 */
public class MxnRefChemUtils extends MxnRefUtils
{
	/**
	 * 
	 */
	private static final int NAME = 1;

	/**
	 * 
	 */
	private static final int FORMULA = 2;

	/**
	 * 
	 */
	private static final int CHARGE = 3;

	/**
	 * 
	 */
	// private static final int MASS = 4;

	/**
	 * 
	 */
	private static final int INCHI = 5;

	/**
	 * 
	 */
	private static final int SMILES = 6;

	/**
	 * 
	 */
	private static final String MXNREF_ID_REG_EXP = "MNXM[0-9]+"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final URL dataUrl = new URL( "http://metanetx.org/cgi-bin/mnxget/mnxref/chem_prop.tsv" ); //$NON-NLS-1$

	/**
	 * 
	 */
	private static MxnRefChemUtils instance = null;

	/**
	 * 
	 * @return MxnRefChemUtils
	 * @throws MalformedURLException
	 */
	public static MxnRefChemUtils getInstance() throws MalformedURLException
	{
		if( instance == null )
		{
			instance = new MxnRefChemUtils();
		}

		return instance;
	}

	/**
	 * 
	 * @throws MalformedURLException
	 */
	private MxnRefChemUtils() throws MalformedURLException
	{
		super( MXNREF_ID_REG_EXP, new URL( "http://metanetx.org/cgi-bin/mnxget/mnxref/chem_xref.tsv" ) ); //$NON-NLS-1$
	}

	/**
	 * 
	 * @param id
	 * @return String
	 * @throws IOException
	 */
	public synchronized String getName( final String id ) throws IOException
	{
		return getData( id, NAME, dataUrl );
	}

	/**
	 * 
	 * @param id
	 * @return String
	 * @throws IOException
	 */
	public synchronized String getFormula( final String id ) throws IOException
	{
		return getData( id, FORMULA, dataUrl );
	}

	/**
	 * 
	 * @param id
	 * @return int
	 * @throws IOException
	 */
	public synchronized int getCharge( final String id ) throws IOException
	{
		final String charge = getData( id, CHARGE, dataUrl );

		if( charge != null && !charge.equals( SubliminalUtils.EMPTY_STRING ) )
		{
			return Integer.parseInt( charge );
		}

		return SubliminalUtils.UNDEFINED_NUMBER;
	}

	/**
	 * 
	 * @param id
	 * @return String
	 * @throws IOException
	 */
	public synchronized String getInchi( final String id ) throws IOException
	{
		return getData( id, INCHI, dataUrl );
	}

	/**
	 * 
	 * @param id
	 * @return String
	 * @throws IOException
	 */
	public synchronized String getSmiles( final String id ) throws IOException
	{
		return getData( id, SMILES, dataUrl );
	}
}