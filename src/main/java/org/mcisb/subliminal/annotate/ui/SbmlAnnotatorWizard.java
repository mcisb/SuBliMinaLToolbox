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

import java.awt.*;
import java.util.*;
import javax.swing.*;
import org.mcisb.subliminal.annotate.*;
import org.mcisb.ui.util.*;
import org.mcisb.ui.wizard.*;
import org.mcisb.util.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlAnnotatorWizard extends Wizard
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	private static final ResourceBundle resourceBundle = ResourceBundle.getBundle( "org.mcisb.subliminal.annotate.ui.messages" ); //$NON-NLS-1$

	/**
	 * 
	 * @param parent
	 * @param bean
	 * @param task
	 * @param silent
	 */
	public SbmlAnnotatorWizard( final Component parent, final GenericBean bean, final AbstractSbmlAnnotatorTask task, final boolean silent )
	{
		super( bean, task, true, new SbmlAnnotatorProgressPanel( resourceBundle.getString( "SbmlAnnotatorWizard.progressTitle" ), new Dimension( 200, 200 ), silent ) ); //$NON-NLS-1$

		if( task instanceof SbmlAnnotatorTask )
		{
			confirmationPanel.addPropertyChangeListener( task );
		}

		// Create and add WizardComponents:
		final Collection<String> fileExtensions = new ArrayList<>();
		fileExtensions.add( "xml" ); //$NON-NLS-1$

		final JFileChooser fileChooser = new JFileChooser();
		addWizardComponent( new FileChooserWizardComponent( bean, new FileChooserPanel( parent, resourceBundle.getString( "SbmlAnnotatorWizard.importTitle" ), ParameterPanel.DEFAULT_COLUMNS, fileChooser, false, true, false, JFileChooser.FILES_ONLY, fileExtensions ), PropertyNames.IMPORT_FILEPATHS ) ); //$NON-NLS-1$
		addWizardComponent( new FileChooserWizardComponent( bean, new FileChooserPanel( parent, resourceBundle.getString( "SbmlAnnotatorWizard.exportTitle" ), ParameterPanel.DEFAULT_COLUMNS, fileChooser, false, false, false, JFileChooser.FILES_ONLY, fileExtensions ), PropertyNames.EXPORT_FILEPATHS ) ); //$NON-NLS-1$

		init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.ui.wizard.Wizard#dispose()
	 */
	@Override
	protected void dispose()
	{
		super.dispose();
		confirmationPanel.removePropertyChangeListener( (AbstractSbmlAnnotatorTask)task );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.ui.wizard.Wizard#getResultsComponent()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Component getResultsComponent()
	{
		final InformationPanel informationPanel = new InformationPanel( resourceBundle.getString( "SbmlAnnotatorWizard.resultsTitle" ) ); //$NON-NLS-1$
		final String LINE_SEPARATOR = System.getProperty( "line.separator" ); //$NON-NLS-1$
		final StringBuffer buffer = new StringBuffer();

		if( returnValue != null )
		{
			for( Iterator<Collection<String>> iterator = ( (Collection<Collection<String>>)returnValue ).iterator(); iterator.hasNext(); )
			{
				for( Iterator<String> iterator2 = iterator.next().iterator(); iterator2.hasNext(); )
				{
					buffer.append( iterator2.next() );
				}
				buffer.append( LINE_SEPARATOR );
			}
		}

		informationPanel.setText( buffer.toString() );
		return informationPanel;
	}
}