package org.mcisb.subliminal.mnxref;

import java.io.*;
import org.junit.*;

/**
 * 
 * @author Neil Swainston
 */
public class MxnRefReactionUtilsTest
{
	/**
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void getName() throws IOException
	{
		Assert.assertFalse( MxnRefReactionUtils.getInstance().getBalance( "MNXR150" ) ); //$NON-NLS-1$
		Assert.assertFalse( MxnRefReactionUtils.getInstance().getBalance( "bigg:3HAD40" ) ); //$NON-NLS-1$
	}
}