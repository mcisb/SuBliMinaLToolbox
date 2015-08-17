package org.mcisb.subliminal.metacyc;

import org.junit.*;

/**
 * 
 * @author Neil Swainston
 */
public class MetaCycUtilsTest
{
	/**
	 * 
	 */
	@SuppressWarnings("static-method")
	@Test
	public void unencode()
	{
		Assert.assertTrue( MetaCycUtils.unencode( "CDP__45__GLUCOSE__45__46__45__DEHYDRATASE__45__RXN" ).equals( "CDP-GLUCOSE-46-DEHYDRATASE-RXN" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}
}