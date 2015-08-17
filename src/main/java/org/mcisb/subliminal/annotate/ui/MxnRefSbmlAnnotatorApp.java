/*******************************************************************************
 * Manchester Centre for Integrative Systems Biology
 * University of Manchester
 * Manchester M1 7ND
 * United Kingdom
 * 
 * Copyright (C) 2007 University of Manchester
 * 
 * This program is released under the Academic Free License ("AFL") v3.0.
 * (http://www.opensource.org/licenses/academic.php)
 *******************************************************************************/
package org.mcisb.subliminal.annotate.ui;

import javax.swing.*;
import org.mcisb.subliminal.annotate.*;

/**
 * 
 * @author Neil Swainston
 */
public class MxnRefSbmlAnnotatorApp extends AbstractSbmlAnnotatorApp
{
	/**
	 * 
	 * @param frame
	 * @param silent
	 */
	public MxnRefSbmlAnnotatorApp( final JFrame frame, final boolean silent )
	{
		super( frame, silent );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.subliminal.annotate.ui.AbstractSbmlAnnotatorApp#getTask()
	 */
	@Override
	public AbstractSbmlAnnotatorTask getTask()
	{
		return new MxnRefSbmlAnnotatorTask( silent );
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		final JFrame app = new JFrame();
		new MxnRefSbmlAnnotatorApp( app, args.length > 0 ? Boolean.parseBoolean( args[ 0 ] ) : false ).show();
	}
}