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
package org.mcisb.subliminal.merge;

import java.io.*;
import java.util.*;
// import org.mcisb.chem.util.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.OntologyUtils.MatchCriteria;
import org.mcisb.ontology.chebi.*;
import org.mcisb.ontology.sbo.*;
import org.mcisb.sbml.*;
import org.mcisb.util.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class MergeUtils extends SbmlUtils
{
	/**
	 * 
	 */
	private static final int REACTANTS = 0;

	/**
	 * 
	 */
	private static final int PRODUCTS = 1;

	/**
	 * @param targetModel
	 * @param sourceModel
	 * @param compartment
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @return String
	 * @throws Exception
	 */
	static String addCompartment( final Model targetModel, final Compartment compartment, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId ) throws Exception
	{
		final Map<OntologyTerm,Object[]> ontologyTerms = getOntologyTerms( compartment );
		final String newId = getCompartmentId( targetModel, compartment, ontologyTerms, ontologyTermToIds, originalIdToNewId );
		return add( targetModel, compartment, newId, ontologyTermToIds, originalIdToNewId, ontologyTerms );
	}

	/**
	 * 
	 * @param targetModel
	 * @param species
	 * @param smilesToIds
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @param allMatchCriteria
	 * @param reactantToProducts
	 * @return String
	 * @throws Exception
	 */
	static String addSpecies( final Model targetModel, final Species species, final Map<String,Collection<String>> smilesToIds, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId, final Collection<MatchCriteria> allMatchCriteria, final Map<String,Collection<String>> reactantToProducts ) throws Exception
	{
		final Map<OntologyTerm,Object[]> ontologyTerms = getOntologyTerms( species );
		final String newId = getSpeciesId( targetModel, species, ontologyTerms, smilesToIds, ontologyTermToIds, originalIdToNewId, allMatchCriteria, reactantToProducts );
		final String updatedId = add( targetModel, species, newId, ontologyTermToIds, originalIdToNewId, ontologyTerms );

		if( smilesToIds != null && species.getSBOTerm() == SboUtils.SIMPLE_CHEMICAL && updatedId.equals( species.getId() ) )
		{
			final String smiles = getUnifiedSmiles( targetModel, targetModel.getSpecies( updatedId ) );

			if( smiles != null )
			{
				Collection<String> ids = smilesToIds.get( updatedId );

				if( ids == null )
				{
					ids = new HashSet<>();
					smilesToIds.put( smiles, ids );
				}

				ids.add( updatedId );
			}
		}

		return updatedId;
	}

	/**
	 * @param targetModel
	 * @param sourceModel
	 * @param reaction
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @param allReactionDefinitions
	 * @param checkForReversibility
	 * @return String
	 * @throws Exception
	 */
	static String addReaction( final Model targetModel, final Model sourceModel, final Reaction reaction, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId, final Map<List<HashSet<List<Object>>>,String> allReactionDefinitions, final boolean checkForReversibility ) throws Exception
	{
		final Map<OntologyTerm,Object[]> ontologyTerms = getOntologyTerms( reaction );
		final String newId = getReactionId( targetModel, sourceModel, reaction, originalIdToNewId, allReactionDefinitions, checkForReversibility );
		return add( targetModel, reaction, newId, ontologyTermToIds, originalIdToNewId, ontologyTerms );
	}

	/**
	 * @param targetModel
	 * @param unitDefinition
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @return String
	 * @throws Exception
	 */
	static String addUnitDefinition( final Model targetModel, final UnitDefinition unitDefinition, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId ) throws Exception
	{
		final Map<OntologyTerm,Object[]> ontologyTerms = getOntologyTerms( unitDefinition );
		final String newId = getUnitDefinitionId( targetModel, unitDefinition );
		return add( targetModel, unitDefinition, newId, ontologyTermToIds, originalIdToNewId, ontologyTerms );
	}

	/**
	 * @param targetModel
	 * @param parameter
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @return String
	 * @throws Exception
	 */
	static String addParameter( final Model targetModel, final Parameter parameter, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId ) throws Exception
	{
		final Map<OntologyTerm,Object[]> ontologyTerms = getOntologyTerms( parameter );
		final String newId = getParameterId( targetModel, parameter );
		return add( targetModel, parameter, newId, ontologyTermToIds, originalIdToNewId, ontologyTerms );
	}

	/**
	 * 
	 * @param model
	 * @return Map<String,Collection<String>>
	 */
	static Map<String,Collection<String>> getReactantToProducts( final Model model )
	{
		final Map<String,Collection<String>> reactantToProducts = new TreeMap<>();

		for( int i = 0; i < model.getNumReactions(); i++ )
		{
			final Reaction reaction = model.getReaction( i );

			for( int j = 0; j < reaction.getNumReactants(); j++ )
			{
				final String reactantId = reaction.getReactant( j ).getSpecies();
				Collection<String> products = reactantToProducts.get( reactantId );

				if( products == null )
				{
					products = new TreeSet<>();
					reactantToProducts.put( reactantId, products );
				}

				for( int k = 0; k < reaction.getNumProducts(); k++ )
				{
					final String productId = reaction.getProduct( k ).getSpecies();

					if( !productId.equals( reactantId ) )
					{
						products.add( productId );

						Collection<String> reactants = reactantToProducts.get( productId );

						if( reactants == null )
						{
							reactants = new TreeSet<>();
							reactantToProducts.put( productId, reactants );
						}

						reactants.add( reactantId );
					}
				}
			}

		}

		return reactantToProducts;
	}

	/**
	 * @param targetModel
	 * @param sbase
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @param allReactionDefinitions
	 * @param speciesIdentical
	 * @param checkForReversibility
	 * @return String
	 */
	private static String add( final Model targetModel, final NamedSBase sbase, final String newId, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId, final Map<OntologyTerm,Object[]> ontologyTerms )
	{
		if( newId == null )
		{
			return sbase.getId();
		}

		final boolean add = newId.equals( sbase.getId() );

		if( add )
		{
			originalIdToNewId.put( newId, newId );

			for( Map.Entry<OntologyTerm,Object[]> entry : ontologyTerms.entrySet() )
			{
				final Object[] predicates = entry.getValue();

				if( predicates[ 0 ] == CVTerm.Type.BIOLOGICAL_QUALIFIER && predicates[ 1 ] == CVTerm.Qualifier.BQB_IS )
				{
					final OntologyTerm ontologyTerm = entry.getKey();
					Collection<String> ids = ontologyTermToIds.get( ontologyTerm );

					if( ids == null )
					{
						ids = new HashSet<>();
						ontologyTermToIds.put( ontologyTerm, ids );
					}

					ids.add( newId );
				}
			}

			SbmlUtils.add( targetModel, sbase );
		}
		else if( !sbase.getId().equals( newId ) )
		{
			System.out.println( "Duplicate " + sbase.getElementName() + " " + sbase.getId() + ". Already exists as " + newId + "." ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		return newId;
	}

	/**
	 * 
	 * @param model
	 * @param compartment
	 * @param ontologyTerms
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @return String
	 */
	private static String getCompartmentId( final Model model, final Compartment compartment, final Map<OntologyTerm,Object[]> ontologyTerms, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId )
	{
		for( Map.Entry<OntologyTerm,Object[]> entry : ontologyTerms.entrySet() )
		{
			final Object[] predicates = entry.getValue();

			if( predicates[ 0 ] == CVTerm.Type.BIOLOGICAL_QUALIFIER && predicates[ 1 ] == CVTerm.Qualifier.BQB_IS )
			{
				final OntologyTerm ontologyTerm = entry.getKey();
				final Collection<String> ids = ontologyTermToIds.get( ontologyTerm );

				if( ids != null )
				{
					for( String id : ids )
					{
						addOntologyTerms( model.getCompartment( id ), ontologyTerms );
						originalIdToNewId.put( compartment.getId(), id );

						if( id.equals( compartment.getId() ) )
						{
							// Already exists in model with the same id:
							return null;
						}

						// else, exists in model, but with different id:
						return id;
					}
				}
			}
		}

		originalIdToNewId.put( compartment.getId(), compartment.getId() );
		return model.getCompartment( compartment.getId() ) == null ? compartment.getId() : null;
	}

	/**
	 * 
	 * @param model
	 * @param unitDefinition
	 * @return String
	 */
	private static String getUnitDefinitionId( final Model model, final UnitDefinition unitDefinition )
	{
		for( int l = 0; l < model.getNumUnitDefinitions(); l++ )
		{
			final String id = model.getUnitDefinition( l ).getId();

			if( id.equals( unitDefinition.getId() ) )
			{
				// Already exists in model with the same id:
				return null;
			}
		}

		return unitDefinition.getId();
	}

	/**
	 * 
	 * @param model
	 * @param parameter
	 * @return String
	 */
	private static String getParameterId( final Model model, final Parameter parameter )
	{
		for( int l = 0; l < model.getNumParameters(); l++ )
		{
			final String id = model.getParameter( l ).getId();

			if( id.equals( parameter.getId() ) )
			{
				// Already exists in model with the same id:
				return null;
			}
		}

		return parameter.getId();
	}

	/**
	 * 
	 * @param model
	 * @param species
	 * @param ontologyTerms
	 * @param smilesToIds
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @param allMatchCriteria
	 * @param reactantToProducts
	 * @return String
	 * @throws Exception
	 */
	private static String getSpeciesId( final Model model, final Species species, final Map<OntologyTerm,Object[]> ontologyTerms, final Map<String,Collection<String>> smilesToIds, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId, final Collection<MatchCriteria> allMatchCriteria, final Map<String,Collection<String>> reactantToProducts ) throws Exception
	{
		species.setCompartment( originalIdToNewId.get( species.getCompartment() ) );

		boolean hasIsTerm = false;

		for( Map.Entry<OntologyTerm,Object[]> entry : ontologyTerms.entrySet() )
		{
			final Object[] predicates = entry.getValue();

			if( predicates[ 0 ] == CVTerm.Type.BIOLOGICAL_QUALIFIER && predicates[ 1 ] == CVTerm.Qualifier.BQB_IS )
			{
				final OntologyTerm ontologyTerm = entry.getKey();

				try
				{
					final String smiles = species.getSBOTerm() == SboUtils.SIMPLE_CHEMICAL ? getUnifiedSmiles( model, species ) : null;
					final Collection<String> ids = getSpeciesId( species.getId(), smilesToIds != null ? smiles : null, ontologyTerm, smilesToIds, ontologyTermToIds, allMatchCriteria, reactantToProducts );

					if( ids != null )
					{
						for( String id : ids )
						{
							if( model.getSpecies( id ).getCompartment().equals( species.getCompartment() ) )
							{
								originalIdToNewId.put( species.getId(), id );
								addOntologyTerms( model.getSpecies( id ), ontologyTerms );

								if( id.equals( species.getId() ) )
								{
									// Already exists in model with the same id:
									return null;
								}

								// else, exists in model, but with different id:
								return id;
							}
						}
					}
				}
				catch( FileNotFoundException e )
				{
					// Invalid KEGG terms...
					e.printStackTrace();
				}

				hasIsTerm = true;
			}
		}

		if( !hasIsTerm )
		{
			for( Map.Entry<OntologyTerm,Object[]> entry : ontologyTerms.entrySet() )
			{
				final Object[] predicates = entry.getValue();

				if( predicates[ 0 ] == CVTerm.Type.BIOLOGICAL_QUALIFIER && predicates[ 1 ] == CVTerm.Qualifier.BQB_IS_ENCODED_BY )
				{
					final OntologyTerm ontologyTerm = entry.getKey();
					final Collection<String> ids = ontologyTermToIds.get( ontologyTerm );

					if( ids != null )
					{
						for( String id : ids )
						{
							if( model.getSpecies( id ).getCompartment().equals( species.getCompartment() ) )
							{
								originalIdToNewId.put( species.getId(), id );
								addOntologyTerms( model.getSpecies( id ), ontologyTerms );

								if( id.equals( species.getId() ) )
								{
									// Already exists in model with the same id:
									return null;
								}

								// else, exists in model, but with different id:
								return id;
							}
						}
					}
				}
			}
		}

		originalIdToNewId.put( species.getId(), species.getId() );
		return model.getSpecies( species.getId() ) == null ? species.getId() : null;
	}

	/**
	 * 
	 * @param originalSpeciesId
	 * @param smiles
	 * @param ontologyTerm
	 * @param smilesToIds
	 * @param ontologyTermToIds
	 * @param allMatchCriteria
	 * @param reactantToProducts
	 * @return Collection<String>
	 * @throws Exception
	 */
	private static Collection<String> getSpeciesId( final String originalSpeciesId, final String smiles, final OntologyTerm ontologyTerm, final Map<String,Collection<String>> smilesToIds, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Collection<MatchCriteria> allMatchCriteria, final Map<String,Collection<String>> reactantToProducts ) throws Exception
	{
		if( allMatchCriteria.contains( MatchCriteria.IDENTICAL ) )
		{
			Collection<String> ids = ontologyTermToIds.get( ontologyTerm );

			if( ids != null )
			{
				return retainIsomeraseParticipants( originalSpeciesId, ids, reactantToProducts );
			}
		}

		if( smiles != null )
		{
			Collection<String> ids = smilesToIds.get( smiles );

			if( ids != null )
			{
				return retainIsomeraseParticipants( originalSpeciesId, ids, reactantToProducts );
			}
		}

		if( ontologyTerm instanceof ChebiTerm && ( (ChebiTerm)ontologyTerm ).getFormula() != null )
		{
			for( final Map.Entry<OntologyTerm,Collection<String>> entry : ontologyTermToIds.entrySet() )
			{
				if( OntologyUtils.getInstance().areEquivalent( ontologyTerm, entry.getKey(), allMatchCriteria ) )
				{
					return retainIsomeraseParticipants( originalSpeciesId, entry.getValue(), reactantToProducts );
				}
			}
		}

		return retainIsomeraseParticipants( originalSpeciesId, ontologyTermToIds.get( ontologyTerm ), reactantToProducts );
	}

	/**
	 * 
	 * @param originalSpeciesId
	 * @param speciesIds
	 * @param reactantToProducts
	 * @return Collection<String>
	 */
	private static Collection<String> retainIsomeraseParticipants( final String originalSpeciesId, final Collection<String> speciesIds, final Map<String,Collection<String>> reactantToProducts )
	{
		if( speciesIds == null )
		{
			return null;
		}

		final Collection<String> filteredSpeciesIds = new HashSet<>( speciesIds );
		final Collection<String> productIds = reactantToProducts.get( originalSpeciesId );

		if( productIds != null )
		{
			for( Iterator<String> iterator = filteredSpeciesIds.iterator(); iterator.hasNext(); )
			{
				if( productIds.contains( iterator.next() ) )
				{
					iterator.remove();
				}
			}
		}

		return filteredSpeciesIds;
	}

	/**
	 * 
	 * @param targetModel
	 * @param sourceModel
	 * @param reaction
	 * @param originalIdToNewId
	 * @param allReactionDefinitions
	 * @param checkForReversibility
	 * @return String
	 * @throws Exception
	 */
	private static String getReactionId( final Model targetModel, final Model sourceModel, final Reaction reaction, final Map<String,String> originalIdToNewId, final Map<List<HashSet<List<Object>>>,String> allReactionDefinitions, final boolean checkForReversibility ) throws Exception
	{
		final List<HashSet<List<Object>>> reactionDefinition = getReactionDefinition( targetModel, reaction, originalIdToNewId );

		if( reactionDefinition.get( REACTANTS ).equals( reactionDefinition.get( PRODUCTS ) ) )
		{
			return null;
		}

		Reaction thisReaction = null;
		String reactionId = getExistingReactionId( targetModel, allReactionDefinitions, reactionDefinition, checkForReversibility );

		if( reactionId == null )
		{
			if( targetModel.getReaction( reaction.getId() ) != null )
			{
				System.out.println( originalIdToNewId.get( sourceModel.getReaction( reaction.getId() ).getReactant( 0 ).getSpecies() ) );
				System.out.println( "Conflicting definition of reaction " + reaction.getId() + ". Existing: " + toString( targetModel, reaction.getId(), false ) + ". New: " + toString( sourceModel, reaction.getId(), false ) + "." ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}

			reactionId = targetModel.getReaction( reaction.getId() ) == null ? reaction.getId() : reaction.getId() + StringUtils.getUniqueId();
			reaction.setId( reactionId );
			allReactionDefinitions.put( reactionDefinition, reactionId );
			thisReaction = reaction;
		}
		else
		{
			thisReaction = targetModel.getReaction( reactionId );

			if( !reaction.getId().equals( reactionId ) )
			{
				System.out.println( "Duplicate " + reaction.getElementName() + " " + reaction.getId() + ". Already exists as " + reactionId + "." ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				addOntologyTerms( thisReaction, getOntologyTerms( reaction ) );
			}

			reactionId = null;
		}

		outer: for( int l = 0; l < reaction.getNumModifiers(); l++ )
		{
			final ModifierSpeciesReference speciesReference = reaction.getModifier( l );
			speciesReference.setSpecies( originalIdToNewId.get( speciesReference.getSpecies() ) );

			for( int m = 0; m < thisReaction.getNumModifiers(); m++ )
			{
				final ModifierSpeciesReference existingReactionModifier = thisReaction.getModifier( m );

				if( existingReactionModifier.getSpecies().equals( speciesReference.getSpecies() ) )
				{
					continue outer;
				}
			}

			thisReaction.addModifier( speciesReference.clone() );
		}

		return reactionId;
	}

	/**
	 * 
	 * @param model
	 * @param allReactionDefinitions
	 * @param newReactionDefinition
	 * @param checkForReversibility
	 * @return String
	 */
	private static String getExistingReactionId( final Model model, final Map<List<HashSet<List<Object>>>,String> allReactionDefinitions, final List<HashSet<List<Object>>> newReactionDefinition, final boolean checkForReversibility )
	{
		String reactionId = allReactionDefinitions.get( newReactionDefinition );

		if( checkForReversibility && reactionId == null )
		{
			// Try "flipping" the reaction:
			final List<HashSet<List<Object>>> flippedReactionDefinition = new ArrayList<>();
			flippedReactionDefinition.add( newReactionDefinition.get( PRODUCTS ) );
			flippedReactionDefinition.add( newReactionDefinition.get( REACTANTS ) );

			reactionId = allReactionDefinitions.get( flippedReactionDefinition );

			if( reactionId != null )
			{
				// By "flipping" the reaction, we have seen that the existing,
				// model reaction is reversible:
				model.getReaction( reactionId ).setReversible( true );
			}
		}

		return reactionId;
	}

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @param originalIdToNewId
	 * @return List<HashSet<List<Object>>>
	 */
	private static List<HashSet<List<Object>>> getReactionDefinition( final Model model, final Reaction reaction, final Map<String,String> originalIdToNewId )
	{
		final List<HashSet<List<Object>>> reactionDefinition = Arrays.asList( new HashSet<List<Object>>(), new HashSet<List<Object>>() );

		for( int l = 0; l < reaction.getNumReactants(); l++ )
		{
			final SpeciesReference speciesReference = reaction.getReactant( l );
			speciesReference.setSpecies( originalIdToNewId.get( speciesReference.getSpecies() ) );
			final List<Object> speciesIdCompartmentIdStoichiometry = Arrays.asList( new Object[] { speciesReference.getSpecies(), model.getSpecies( speciesReference.getSpecies() ).getCompartment(), Double.valueOf( speciesReference.getStoichiometry() ) } );
			reactionDefinition.get( REACTANTS ).add( speciesIdCompartmentIdStoichiometry );
		}
		for( int l = 0; l < reaction.getNumProducts(); l++ )
		{
			final SpeciesReference speciesReference = reaction.getProduct( l );
			speciesReference.setSpecies( originalIdToNewId.get( speciesReference.getSpecies() ) );
			final List<Object> speciesIdCompartmentIdStoichiometry = Arrays.asList( new Object[] { speciesReference.getSpecies(), model.getSpecies( speciesReference.getSpecies() ).getCompartment(), Double.valueOf( speciesReference.getStoichiometry() ) } );
			reactionDefinition.get( PRODUCTS ).add( speciesIdCompartmentIdStoichiometry );
		}

		return reactionDefinition;
	}

	/**
	 * 
	 * @param model
	 * @param species
	 * @return String
	 * @throws Exception
	 */
	private static String getUnifiedSmiles( final Model model, final Species species ) throws Exception
	{
		try
		{
			String smiles = SbmlUtils.getSmiles( model, species );

			if( smiles != null )
			{
				try
				{
					// smiles = SmilesUtils.getHydrogenatedDechiralisedSmiles(
					// smiles );
				}
				catch( Exception e )
				{
					// No action.
				}
			}

			return smiles;
		}
		catch( FileNotFoundException e )
		{
			// Typically due to an invalid KEGG entry. Ignore.
			return null;
		}
	}
}
