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

import java.util.*;
import javax.swing.*;
import org.mcisb.subliminal.annotate.*;
import org.mcisb.ui.app.*;
import org.mcisb.ui.util.*;
import org.mcisb.ui.wizard.*;
import org.mcisb.util.*;

/**
 * 
 * @author Neil Swainston
 */
public abstract class AbstractSbmlAnnotatorApp extends App
{
	/**
	 * 
	 */
	private final ResourceBundle resourceBundle = ResourceBundle.getBundle( "org.mcisb.subliminal.annotate.ui.messages" ); //$NON-NLS-1$

	/**
	 * 
	 */
	protected final boolean silent;

	/**
	 * 
	 * @param frame
	 * @param elementIdentifierToOntologySources
	 * @param silent
	 */
	public AbstractSbmlAnnotatorApp( final JFrame frame, final boolean silent )
	{
		super( frame, new GenericBean() );
		this.silent = silent;
		init( resourceBundle.getString( "SbmlAnnotatorApp.title" ), resourceBundle.getString( "SbmlAnnotatorApp.error" ), new ResourceFactory().getImageIcon( resourceBundle.getString( "SbmlAnnotatorApp.icon" ) ).getImage() ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.ui.app.App#getWizard()
	 */
	@Override
	protected Wizard getWizard()
	{
		return new SbmlAnnotatorWizard( window, bean, getTask(), silent );
	}

	/**
	 * 
	 * @return AbstractSbmlAnnotatorTask
	 */
	protected abstract AbstractSbmlAnnotatorTask getTask();
}