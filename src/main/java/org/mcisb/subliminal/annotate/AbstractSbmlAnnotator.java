/**
 * 
 */
package org.mcisb.subliminal.annotate;

import java.beans.*;
import java.io.*;
import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.taxonomy.*;
import org.mcisb.sbml.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.util.task.*;
import org.sbml.jsbml.*;

/**
 * @author neilswainston
 * 
 */
public abstract class AbstractSbmlAnnotator
{
	/**
	 * 
	 */
	public static final String SUCCESS_RATE = "SUCCESS_RATE"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final static int MAX_LIST_OF_SIZE = Integer.MAX_VALUE;

	/**
	 * 
	 */
	protected final ArrayList<Collection<String>> unannotatedSBases = new ArrayList<>();

	/**
	 * 
	 */
	protected final PropertyChangeSupport support = new PropertyChangeSupport( this );

	/**
	 * 
	 */
	protected SBMLDocument document = null;

	/**
	 * 
	 */
	protected String ncbiTaxonomyTerm = null;

	/**
	 * 
	 */
	protected int progress = 0;

	/**
	 * 
	 */
	protected int successRate = 0;

	/**
	 * 
	 */
	protected long sbaseCount = 0;

	/**
	 * 
	 */
	protected long numberOfSBases = 0;

	/**
	 * 
	 */
	protected long numberOfMatchedSBases = 0;

	/**
	 * 
	 */
	protected OntologyTerm selectedOntologyTerm = null;

	/**
	 * 
	 */
	protected boolean cancelled = false;

	/**
	 * 
	 */
	protected boolean silent = false;

	/**
	 * 
	 * @param silent
	 */
	public AbstractSbmlAnnotator( final boolean silent )
	{
		this.silent = silent;
	}

	/**
	 * 
	 * @param inputFile
	 * @param outputFile
	 * @return Serializable
	 * @throws Exception
	 */
	public Serializable annotate( final File inputFile, final File outputFile ) throws Exception
	{
		try ( final InputStream is = new FileInputStream( inputFile ); final OutputStream os = new FileOutputStream( outputFile ) )
		{
			return annotate( is, os );
		}
	}

	/**
	 * 
	 * @param is
	 * @param os
	 * @return Serializable
	 * @throws Exception
	 */
	public Serializable annotate( final InputStream is, final OutputStream os ) throws Exception
	{
		try
		{
			document = SBMLReader.read( is );

			final Model model = document.getModel();

			final OntologyTerm ontologyTerm = SbmlUtils.getOntologyTerm( model, Ontology.TAXONOMY );

			if( ontologyTerm instanceof TaxonomyTerm )
			{
				ncbiTaxonomyTerm = ontologyTerm.getId();
			}

			// Annotate compartments:
			resetCounters();
			numberOfSBases = model.getNumCompartments();
			annotateSBases( model.getListOfCompartments() );

			// Annotate species:
			resetCounters();
			numberOfSBases = model.getNumSpecies();
			annotateSBases( model.getListOfSpecies() );

			// Annotate reactions:
			resetCounters();
			numberOfSBases = model.getNumReactions();
			annotateSBases( model.getListOfReactions() );
		}
		finally
		{
			if( document != null )
			{
				XmlFormatter.getInstance().write( document, os );
			}
		}

		return unannotatedSBases;
	}

	/**
	 * 
	 */
	public void cancel()
	{
		cancelled = true;
	}

	/**
	 * 
	 * @param selectedOntologyTerm
	 */
	public void setSelectedOntologyTerm( final OntologyTerm selectedOntologyTerm )
	{
		this.selectedOntologyTerm = selectedOntologyTerm;
	}

	/**
	 * @param silent
	 */
	public synchronized void setSilent( final boolean silent )
	{
		this.silent = silent;
	}

	/**
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener( final PropertyChangeListener listener )
	{
		support.addPropertyChangeListener( listener );
	}

	/**
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener( final PropertyChangeListener listener )
	{
		support.removePropertyChangeListener( listener );
	}

	/**
	 * 
	 * 
	 * @param listOf
	 * @throws Exception
	 */
	protected void annotateSBases( final ListOf<?> listOf ) throws Exception
	{
		if( listOf.size() < MAX_LIST_OF_SIZE )
		{
			for( int l = 0; !cancelled; l++ )
			{
				final Object sbase = listOf.get( l );

				if( sbase == null )
				{
					break;
				}

				if( sbase instanceof NamedSBase )
				{
					annotateSBase( (NamedSBase)sbase );
				}
			}
		}
	}

	/**
	 * 
	 * @param sbase
	 * @throws Exception
	 */
	protected void annotateSBase( final NamedSBase sbase ) throws Exception
	{
		boolean annotated = false;

		for( Iterator<Object[]> iterator = SbmlUtils.getOntologyTerms( sbase ).values().iterator(); iterator.hasNext(); )
		{
			final Object[] cvParams = iterator.next();

			if( ( cvParams[ 0 ] == CVTerm.Type.BIOLOGICAL_QUALIFIER && cvParams[ 1 ] == CVTerm.Qualifier.BQB_IS ) || ( cvParams[ 0 ] == CVTerm.Type.MODEL_QUALIFIER && cvParams[ 1 ] == CVTerm.Qualifier.BQM_IS ) )
			{
				annotated = true;
				break;
			}
		}

		if( !annotated )
		{
			Collection<OntologyTerm> ontologyTerms = find( sbase );

			if( ontologyTerms != null && ontologyTerms.size() > 0 )
			{
				SbmlUtils.addOntologyTerms( sbase, ontologyTerms, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS );
				numberOfMatchedSBases++;
			}
			else
			{
				final String identifier = "id: " + sbase.getId() + ", name: " + sbase.getName(); //$NON-NLS-1$ //$NON-NLS-2$
				support.firePropertyChange( Task.MESSAGE, null, "NO MATCH: " + identifier ); //$NON-NLS-1$
				unannotatedSBases.add( new ArrayList<>( Arrays.asList( identifier ) ) );
			}
		}
		else
		{
			numberOfMatchedSBases++;
		}

		sbaseCount++;

		final int oldProgress = progress;
		final int oldSuccessRate = successRate;
		progress = (int)( (float)sbaseCount / numberOfSBases * 100 );
		successRate = (int)( (float)numberOfMatchedSBases / sbaseCount * 100 );
		support.firePropertyChange( Task.PROGRESS, oldProgress, progress );
		support.firePropertyChange( SUCCESS_RATE, oldSuccessRate, successRate );
	}

	/**
	 * 
	 * @param sbase
	 * @return Collection<OntologyTerm>
	 * @throws Exception
	 */
	protected abstract Collection<OntologyTerm> find( final NamedSBase sbase ) throws Exception;

	/**
	 * 
	 */
	protected void resetCounters()
	{
		final int oldProgress = progress;
		final int oldSuccessRate = successRate;

		progress = 0;
		successRate = 0;
		sbaseCount = 0;
		numberOfSBases = 0;
		numberOfMatchedSBases = 0;

		support.firePropertyChange( Task.PROGRESS, oldProgress, progress );
		support.firePropertyChange( SUCCESS_RATE, oldSuccessRate, successRate );
	}
}