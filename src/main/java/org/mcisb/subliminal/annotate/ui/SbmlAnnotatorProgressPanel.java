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
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.mcisb.ontology.*;
import org.mcisb.subliminal.annotate.*;
import org.mcisb.ui.ontology.*;
import org.mcisb.ui.util.*;
import org.mcisb.util.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlAnnotatorProgressPanel extends TextProgressPanel implements ChangeListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	private final static ResourceBundle resourceBundle = ResourceBundle.getBundle( "org.mcisb.subliminal.annotate.ui.messages" ); //$NON-NLS-1$

	/**
	 * 
	 */
	private JProgressBar matchedProgressBar;

	/**
	 * 
	 */
	private JCheckBox silentCheckBox;

	/**
	 * 
	 * @param title
	 * @param textAreaPreferredSize
	 * @param silent
	 */
	public SbmlAnnotatorProgressPanel( final String title, final Dimension textAreaPreferredSize, final boolean silent )
	{
		super( title, textAreaPreferredSize, true );

		matchedProgressBar = new JProgressBar();
		matchedProgressBar.setBackground( Color.RED );
		matchedProgressBar.setForeground( Color.GREEN );
		matchedProgressBar.setIndeterminate( false );
		matchedProgressBar.setStringPainted( true );

		progressBar.setIndeterminate( false );
		progressBar.setStringPainted( true );

		silentCheckBox = new JCheckBox( resourceBundle.getString( "SbmlAnnotatorProgressPanel.silentCheckBoxText" ) ); //$NON-NLS-1$
		silentCheckBox.setSelected( silent );
		silentCheckBox.addChangeListener( this );

		add( display, 0, 0, true, true, true, false, GridBagConstraints.BOTH );
		add( matchedProgressBar, 0, 1, true, true, false, false, GridBagConstraints.HORIZONTAL );
		add( progressBar, 0, 2, true, true, false, false, GridBagConstraints.HORIZONTAL );
		add( silentCheckBox, 0, 3, true, true, false, true, GridBagConstraints.HORIZONTAL );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.ui.util.ProgressPanel#propertyChange(java.beans.PropertyChangeEvent
	 * )
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange( PropertyChangeEvent e )
	{
		super.propertyChange( e );

		if( !silentCheckBox.isSelected() && e.getPropertyName().equals( SbmlAnnotator.IDS ) )
		{
			final Map<String,Collection<OntologyTerm>> searchResults = (Map<String,Collection<OntologyTerm>>)e.getNewValue();
			final Map.Entry<String,Collection<OntologyTerm>> searchResult = CollectionUtils.getFirst( searchResults.entrySet() );
			final Collection<OntologyTerm> ontologyTerms = searchResult.getValue();
			final Container topLevelAncestor = getTopLevelAncestor();
			final JDialog owner = ( topLevelAncestor instanceof Frame ) ? new JDialog( (Frame)topLevelAncestor ) : new JDialog();
			owner.setModal( true );

			try
			{
				final int PAUSE = 1000;
				final OntologyTermDialog ontologyTermDialog = new OntologyTermDialog( owner, resourceBundle.getString( "SbmlAnnotatorProgressPanel.title" ), true, resourceBundle.getString( "SbmlAnnotatorProgressPanel.message" ) + searchResult.getKey(), searchResult.getKey() ); //$NON-NLS-1$ //$NON-NLS-2$
				ontologyTermDialog.setOntologyTerms( ontologyTerms );
				ComponentUtils.setLocationCentral( ontologyTermDialog );
				ontologyTermDialog.setVisible( true );
				firePropertyChange( SbmlAnnotator.TERM_ID, null, ontologyTermDialog.getOntologyTerm() );
				Thread.sleep( PAUSE );
			}
			catch( Exception ex )
			{
				final JDialog errorDialog = new ExceptionComponentFactory( true ).getExceptionDialog( getParent(), ExceptionUtils.toString( ex ), ex );
				ComponentUtils.setLocationCentral( errorDialog );
				errorDialog.setVisible( true );
			}
		}
		else if( e.getPropertyName().equals( AbstractSbmlAnnotator.SUCCESS_RATE ) )
		{
			matchedProgressBar.setValue( ( (Integer)e.getNewValue() ).intValue() );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
	 * )
	 */
	@Override
	public void stateChanged( final ChangeEvent event )
	{
		firePropertyChange( SbmlAnnotator.SILENT, !silentCheckBox.isSelected(), silentCheckBox.isSelected() );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.ui.util.ProgressPanel#dispose()
	 */
	@Override
	public void dispose() throws Exception
	{
		super.dispose();
		silentCheckBox.removeChangeListener( this );
	}
}