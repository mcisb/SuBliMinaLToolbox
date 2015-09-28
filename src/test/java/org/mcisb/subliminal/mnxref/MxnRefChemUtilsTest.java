package org.mcisb.subliminal.mnxref;

import java.io.*;
import org.junit.*;
import org.mcisb.subliminal.mnxref.MxnRefUtils.Evidence;

/**
 * 
 * @author Neil Swainston
 */
public class MxnRefChemUtilsTest
{
	/**
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void getId() throws IOException
	{
		Assert.assertTrue( MxnRefChemUtils.getInstance().getMxnRefId( "kegg_C00011" ).equals( "MNXM13" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( MxnRefChemUtils.getInstance().getMxnRefId( "bigg_co2" ).equals( "MNXM13" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( MxnRefChemUtils.getInstance().getMxnRefId( "kegg:C00011" ).equals( "MNXM13" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( MxnRefChemUtils.getInstance().getMxnRefId( "bigg:co2" ).equals( "MNXM13" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void getName() throws IOException
	{
		Assert.assertTrue( MxnRefChemUtils.getInstance().getName( "MNXM400" ).equals( "creatine" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( MxnRefChemUtils.getInstance().getName( "bigg:creat" ).equals( "creatine" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( MxnRefChemUtils.getInstance().getName( "MNXM9247" ).equals( "WHWLQLKPGQPMY" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void getInchi() throws IOException
	{
		Assert.assertTrue( MxnRefChemUtils.getInstance().getSmiles( "MNXM1" ).equals( "[H+]" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void getEvidence() throws IOException
	{
		Assert.assertTrue( MxnRefChemUtils.getInstance().getEvidence( "bigg:5apru" ).equals( Evidence.inferred ) ); //$NON-NLS-1$
	}
}