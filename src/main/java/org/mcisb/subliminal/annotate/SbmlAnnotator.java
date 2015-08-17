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

import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.uniprot.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlAnnotator extends AbstractSbmlAnnotator
{
	/**
	 * 
	 */
	public final static String IDS = "IDS"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String TERM_ID = "TERM_ID"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String SILENT = "SILENT"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String COMPARTMENT = "compartment"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String SPECIES = "species"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String REACTION = "reaction"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * 
	 */
	private final static int MIN_IDENTIFIER_LENGTH = 3;

	/**
	 * 
	 */
	private final Map<String,Collection<OntologyTerm>> valueToOntologyTerm = new HashMap<>();

	/**
	 * 
	 */
	private final Map<Object,List<OntologySource>> elementIdentifierToOntologySource;

	/**
	 * 
	 * @param elementIdentifierToOntologySource
	 * @param silent
	 */
	public SbmlAnnotator( final Map<Object,List<OntologySource>> elementIdentifierToOntologySource, final boolean silent )
	{
		super( silent );
		this.elementIdentifierToOntologySource = elementIdentifierToOntologySource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.subliminal.annotate.AbstractSbmlAnnotator#find(org.sbml.jsbml
	 * .NamedSBase)
	 */
	@Override
	protected Collection<OntologyTerm> find( final NamedSBase sbase )
	{
		String identifier = sbase.getName();

		if( identifier == null || identifier.length() == 0 )
		{
			identifier = sbase.getId();
		}

		if( identifier != null && !identifier.equals( EMPTY_STRING ) )
		{
			// First attempt to get OntologySource by SBO term:
			List<OntologySource> ontologySources = elementIdentifierToOntologySource.get( Integer.valueOf( sbase.getSBOTerm() ) );

			// If this fails, attempt to get OntologySource by element name:
			if( ontologySources == null )
			{
				ontologySources = elementIdentifierToOntologySource.get( sbase.getElementName() );
			}

			if( ontologySources != null )
			{
				for( final OntologySource ontologySource : ontologySources )
				{
					if( ontologySource instanceof UniProtUtils )
					{
						( (UniProtUtils)ontologySource ).setNcbiTaxonomyTerm( ncbiTaxonomyTerm );
					}

					final Collection<OntologyTerm> ontologyTerms = getOntologyTerms( ontologySource, sbase );

					if( ontologyTerms.size() > 0 )
					{
						return ontologyTerms;
					}
				}
			}
		}

		return null;
	}

	/**
	 * 
	 * @return silent
	 */
	private synchronized boolean isSilent()
	{
		return silent;
	}

	/**
	 * 
	 * @param ontologySource
	 * @param sbase
	 * @return Collection
	 */
	private Collection<OntologyTerm> getOntologyTerms( final OntologySource ontologySource, final NamedSBase sbase )
	{
		final Set<OntologyTerm> ontologyTerms = new HashSet<>();

		String identifier = sbase.getName();

		if( identifier == null || identifier.length() == 0 )
		{
			identifier = sbase.getId();
		}

		if( identifier != null && identifier.length() >= MIN_IDENTIFIER_LENGTH )
		{
			if( valueToOntologyTerm.get( identifier ) != null )
			{
				return valueToOntologyTerm.get( identifier );
			}
			// else
			selectedOntologyTerm = null;

			try
			{
				final Collection<OntologyTerm> searchedOntologyTerms = ontologySource.search( identifier );

				if( searchedOntologyTerms.size() > 0 )
				{
					if( isSilent() )
					{
						outer: for( Iterator<OntologyTerm> iterator = searchedOntologyTerms.iterator(); iterator.hasNext(); )
						{
							final OntologyTerm ontologyTerm = iterator.next();
							final Collection<String> names = new HashSet<>();
							names.add( ontologyTerm.getName() );
							names.addAll( ontologyTerm.getSynonyms() );

							for( Iterator<String> iterator2 = names.iterator(); iterator2.hasNext(); )
							{
								if( matches( identifier, iterator2.next() ) )
								{
									setSelectedOntologyTerm( ontologyTerm );
									break outer;
								}
							}
						}
					}
					else
					{
						final Map<String,Collection<OntologyTerm>> searchResult = new HashMap<>();
						searchResult.put( sbase.getName(), searchedOntologyTerms );
						support.firePropertyChange( IDS, null, searchResult );
					}
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}

			final OntologyTerm ontologyTerm = selectedOntologyTerm;

			if( ontologyTerm != null )
			{
				ontologyTerms.add( ontologyTerm );
				// OntologyUtils.getXrefs( ontologyTerms );
			}
		}

		valueToOntologyTerm.put( identifier, ontologyTerms );
		return ontologyTerms;
	}

	/**
	 * 
	 * @param s1
	 * @param s2
	 * @return boolean
	 */
	private static boolean matches( final String s1, final String s2 )
	{
		return getAlphanumeric( s1 ).equalsIgnoreCase( getAlphanumeric( s2 ) );
	}

	/**
	 * 
	 * @param in
	 * @return String
	 */
	private static String getAlphanumeric( final String in )
	{
		return in == null ? null : in.replaceAll( "[^a-zA-Z0-9]", "" ); //$NON-NLS-1$ //$NON-NLS-2$
	}
}