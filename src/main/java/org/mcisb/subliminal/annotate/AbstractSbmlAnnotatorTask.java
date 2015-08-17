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
package org.mcisb.subliminal.annotate;

import java.beans.*;
import java.io.*;
import org.mcisb.ontology.*;
import org.mcisb.util.task.*;

/**
 * 
 * @author Neil Swainston
 */
public abstract class AbstractSbmlAnnotatorTask extends AbstractGenericBeanTask
{
	/**
	 * 
	 */
	protected final AbstractSbmlAnnotator annotator;

	/**
	 * 
	 * @param annotator
	 */
	public AbstractSbmlAnnotatorTask( final AbstractSbmlAnnotator annotator )
	{
		this.annotator = annotator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.util.Task#cancel()
	 */
	@Override
	public void cancel() throws Exception
	{
		super.cancel();

		if( annotator != null )
		{
			annotator.cancel();
			annotator.removePropertyChangeListener( this );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.util.task.AbstractTask#doTask()
	 */
	@Override
	protected Serializable doTask() throws Exception
	{
		try
		{
			final File importFile = bean.getFile( org.mcisb.util.PropertyNames.IMPORT_FILEPATHS );
			final File exportFile = bean.getFile( org.mcisb.util.PropertyNames.EXPORT_FILEPATHS );
			annotator.addPropertyChangeListener( this );
			return annotator.annotate( importFile, exportFile );
		}
		finally
		{
			if( annotator != null )
			{
				annotator.removePropertyChangeListener( this );
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	@Override
	public void propertyChange( PropertyChangeEvent evt )
	{
		if( evt.getPropertyName().equals( MESSAGE ) )
		{
			setMessage( (String)evt.getNewValue() );
		}
		else if( evt.getPropertyName().equals( PROGRESS ) )
		{
			setProgress( ( (Integer)evt.getNewValue() ).intValue() );
		}
		else if( evt.getPropertyName().equals( SbmlAnnotator.IDS ) || evt.getPropertyName().equals( AbstractSbmlAnnotator.SUCCESS_RATE ) )
		{
			firePropertyChange( evt );
		}
		else if( evt.getPropertyName().equals( SbmlAnnotator.TERM_ID ) )
		{
			try
			{
				annotator.setSelectedOntologyTerm( (OntologyTerm)evt.getNewValue() );
			}
			catch( Exception e )
			{
				exception = e;
			}
		}
		else if( evt.getPropertyName().equals( SbmlAnnotator.SILENT ) )
		{
			annotator.setSilent( ( (Boolean)evt.getNewValue() ).booleanValue() );
		}
	}
}