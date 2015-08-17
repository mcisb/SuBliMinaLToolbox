package org.mcisb.subliminal.annotate;

import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.subliminal.mnxref.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author neilswainston
 */
public class MxnRefSbmlAnnotator extends AbstractSbmlAnnotator
{
	/**
	 * 
	 * @param silent
	 */
	public MxnRefSbmlAnnotator( final boolean silent )
	{
		super( silent );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.subliminal.annotate.AbstractSbmlAnnotator#find(org.sbml.jsbml
	 * .NamedSBase)
	 */
	@Override
	protected Collection<OntologyTerm> find( final NamedSBase sbase ) throws Exception
	{
		final String id = sbase.getId();

		if( sbase instanceof Compartment )
		{
			// Not implemented.
		}
		else if( sbase instanceof Species )
		{
			final String formattedId = id.replaceAll( "^M", "bigg" ).replaceAll( "_[a-z]", "" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			final String mxnRefId = MxnRefChemUtils.getInstance().getMxnRefId( formattedId );

			if( mxnRefId != null )
			{
				final Collection<OntologyTerm> ontologyTerms = new ArrayList<>();

				for( String chebiId : MxnRefChemUtils.getInstance().getXrefIds( mxnRefId, "chebi" ) ) //$NON-NLS-1$
				{
					ontologyTerms.add( OntologyUtils.getInstance().getOntologyTerm( Ontology.CHEBI, chebiId ) );
				}

				return ontologyTerms;
			}
		}
		else if( sbase instanceof Reaction )
		{
			// Not implemented.
		}

		return null;
	}
}