/*******************************************************************************
 * Manchester Centre for Integrative Systems Biology
 * University of Manchester
 * Manchester M1 7ND
 * United Kingdom
 * 
 * Copyright (C) 2008 University of Manchester
 * 
 * This program is released under the Academic Free License ("AFL") v3.0.
 * (http://www.opensource.org/licenses/academic.php)
 *******************************************************************************/
package org.mcisb.subliminal.shorthand;

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.transform.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.chebi.*;
import org.mcisb.ontology.kegg.*;
import org.mcisb.ontology.sbo.*;
import org.mcisb.sbml.*;
import org.mcisb.subliminal.*;
import org.mcisb.util.*;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.CVTerm.Type;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlCreator
{
	/**
	 * 
	 * @param model
	 * @param compartmentIdsToUri
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void createCompartments( final Model model, final File compartmentIdsToUri ) throws TransformerFactoryConfigurationError, Exception
	{
		createCompartments( model, FileUtils.readMap( compartmentIdsToUri ) );
	}

	/**
	 * 
	 * @param model
	 * @param compartmentIdsToUri
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void createCompartments( final Model model, final Map<String,String> compartmentIdsToUri ) throws TransformerFactoryConfigurationError, Exception
	{
		for( Map.Entry<String,String> entry : compartmentIdsToUri.entrySet() )
		{
			final String compartmentId = entry.getKey();
			final Compartment compartment = model.createCompartment( compartmentId );
			compartment.setSBOTerm( SboUtils.COMPARTMENT );
			compartment.setSize( 1.0 );
			addOntologyTerm( compartment, "BQB_IS", OntologyUtils.getInstance().getOntologyTerm( entry.getValue() ) ); //$NON-NLS-1$
		}
	}

	/**
	 * 
	 * @param model
	 * @param speciesIdsToUri
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void createSpecies( final Model model, final File speciesIdsToUri ) throws TransformerFactoryConfigurationError, Exception
	{
		createSpecies( model, FileUtils.readMap( speciesIdsToUri ) );
	}

	/**
	 * 
	 * @param model
	 * @param speciesIdsToUri
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void createSpecies( final Model model, final Map<String,String> speciesIdsToUri ) throws TransformerFactoryConfigurationError, Exception
	{
		for( Map.Entry<String,String> entry : speciesIdsToUri.entrySet() )
		{
			final String speciesId = entry.getKey();
			final Species species = model.createSpecies( speciesId, model.getCompartment( speciesId.substring( speciesId.lastIndexOf( '_' ) + 1 ) ) );
			species.setSBOTerm( SboUtils.SIMPLE_CHEMICAL );
			species.setInitialConcentration( 0.0 );

			if( species.getCompartment().equals( "b" ) ) //$NON-NLS-1$
			{
				species.setBoundaryCondition( true );
			}

			if( entry.getValue() != null )
			{
				addOntologyTerm( species, "BQB_IS", OntologyUtils.getInstance().getOntologyTerm( entry.getValue() ) ); //$NON-NLS-1$
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @param reactionDefinitions
	 * @throws Exception
	 * @throws IOException
	 * @throws TransformerFactoryConfigurationError
	 * @throws FileNotFoundException
	 */
	public static void createReactions( final Model model, final File reactionDefinitions ) throws FileNotFoundException, TransformerFactoryConfigurationError, IOException, Exception
	{
		createReactions( model, FileUtils.readLists( reactionDefinitions ) );
	}

	/**
	 * 
	 * @param model
	 * @param reactionDefinitions
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void createReactions( final Model model, final Collection<String[]> reactionDefinitions ) throws TransformerFactoryConfigurationError, Exception
	{
		final int ID = 0;
		final int NAME = 1;
		final int DEFINITION = 2;

		for( String[] reactionDefinition : reactionDefinitions )
		{
			final Reaction reaction = model.createReaction( reactionDefinition[ ID ] );
			reaction.setName( reactionDefinition[ NAME ] );
			setReaction( model, reaction, reactionDefinition[ DEFINITION ] );
		}
	}

	/**
	 * 
	 * @param reaction
	 * @param definition
	 * @throws XMLStreamException
	 * @throws UnsupportedEncodingException
	 */
	public static void setReaction( final Model model, final Reaction reaction, final String definition ) throws UnsupportedEncodingException, XMLStreamException
	{
		final double DEFAULT_STOICHIOMETRY = 1.0;
		final String EMPTY_STRING = ""; //$NON-NLS-1$
		final String FORWARD = "-{1,2}\\>"; //$NON-NLS-1$
		final String REVERSE = "\\<-{1,2}"; //$NON-NLS-1$
		final String REVERSIBLE = "\\<={1,2}\\>"; //$NON-NLS-1$

		SbmlUtils.set( reaction, SbmlUtils.GENE_ASSOCIATION, EMPTY_STRING );

		boolean reactant = true;
		boolean flip = false;
		double stoichiometry = DEFAULT_STOICHIOMETRY;

		final Set<String> compartments = new HashSet<>();

		for( String token : definition.split( "\\s+(\\+\\s+)*" ) ) //$NON-NLS-1$
		{
			if( token.matches( FORWARD ) )
			{
				SbmlUpdater.updateReversibility( reaction, false );
				reaction.setReversible( false );
				reactant = false;
			}
			else if( token.matches( REVERSE ) )
			{
				SbmlUpdater.updateReversibility( reaction, false );
				reactant = false;
				flip = true;
			}
			else if( token.matches( REVERSIBLE ) )
			{
				SbmlUpdater.updateReversibility( reaction, true );
				reactant = false;
			}
			else
			{
				try
				{
					stoichiometry = Double.parseDouble( token );
				}
				catch( NumberFormatException e )
				{
					final Species species = model.getSpecies( SubliminalUtils.getNormalisedId( token ) );
					final SpeciesReference reference = reactant ? reaction.createReactant( species ) : reaction.createProduct( species );
					reference.setStoichiometry( stoichiometry );
					stoichiometry = DEFAULT_STOICHIOMETRY;
					compartments.add( species.getCompartment() );
				}
			}
		}

		reaction.setSBOTerm( compartments.size() == 1 ? SboUtils.BIOCHEMICAL_REACTION : SboUtils.TRANSPORT_REACTION );

		if( flip )
		{
			SbmlUpdater.reverse( reaction );
		}
	}

	/**
	 * 
	 * @param sbase
	 * @param predicate
	 * @param ontologyTerm
	 * @throws UnsupportedEncodingException
	 * @throws XMLStreamException
	 * @throws Exception
	 */
	public static void addOntologyTerm( final NamedSBase sbase, final String predicate, final OntologyTerm ontologyTerm ) throws UnsupportedEncodingException, XMLStreamException, Exception
	{
		sbase.unsetAnnotation();

		SbmlUtils.addOntologyTerm( sbase, ontologyTerm, predicate.startsWith( "BQB" ) ? Type.BIOLOGICAL_QUALIFIER : Type.MODEL_QUALIFIER, Qualifier.valueOf( predicate ) ); //$NON-NLS-1$
		sbase.setName( ontologyTerm.getName() );

		if( ontologyTerm instanceof ChebiTerm && sbase instanceof Species )
		{
			final ChebiTerm chebiTerm = (ChebiTerm)ontologyTerm;

			SbmlUtils.setFormula( (Species)sbase, chebiTerm.getFormula() );
			final int chebiCharge = chebiTerm.getCharge();
			SbmlUtils.setCharge( (Species)sbase, chebiCharge == NumberUtils.UNDEFINED ? 0 : chebiCharge );

			/*
			 * if( chebiTerm.getSmiles() != null ) { SbmlUtils.setSmiles(
			 * (Species)sbase, chebiTerm.getSmiles() ); }
			 */

			if( chebiTerm.getInchi() != null )
			{
				SbmlUtils.addOntologyTerm( sbase, OntologyUtils.getInstance().getOntologyTerm( Ontology.INCHI, chebiTerm.getInchi() ), predicate.startsWith( "BQB" ) ? Type.BIOLOGICAL_QUALIFIER : Type.MODEL_QUALIFIER, Qualifier.valueOf( predicate ) ); //$NON-NLS-1$
			}
		}
		else if( ontologyTerm instanceof KeggCompoundTerm )
		{
			SbmlUtils.setFormula( (Species)sbase, ( (KeggCompoundTerm)ontologyTerm ).getFormula() );
			SbmlUtils.setCharge( (Species)sbase, 0 );
		}
	}
}