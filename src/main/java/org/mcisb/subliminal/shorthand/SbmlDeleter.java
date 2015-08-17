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
import org.mcisb.ontology.*;
import org.mcisb.sbml.*;
import org.mcisb.subliminal.balance.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlDeleter
{
	/**
	 * 
	 */
	private static final String META_ID_PREFIX = "meta_"; //$NON-NLS-1$

	/**
	 * 
	 * @param document
	 * @throws Exception
	 */
	public static void clean( final SBMLDocument document ) throws Exception
	{
		final int MAX_STOICHIOMETRIC_COEFFICENT = 12;
		final Model model = document.getModel();
		float proportionBalanced = 0;

		while( true )
		{
			final Collection<String> speciesIdsToRemove = new HashSet<>();

			final SbmlReactionBalancerTask task = new SbmlReactionBalancerTask( document, null, MAX_STOICHIOMETRIC_COEFFICENT );
			task.run();

			final Map<String,Integer> metaboliteDegree = SbmlUtils.getMetaboliteDegree( model, true );
			final Map<String,Integer> problematicMetaboliteDegree = task.getProblematicMetaboliteDegree();

			for( Map.Entry<String,Integer> entry : problematicMetaboliteDegree.entrySet() )
			{
				final String speciesId = entry.getKey();
				final int problematicReactions = entry.getValue().intValue();
				final int allReactions = metaboliteDegree.get( speciesId ).intValue();

				if( problematicReactions == allReactions && problematicReactions == 1 )
				{
					speciesIdsToRemove.add( speciesId );
					// System.out.println( speciesId + "\t" + model.getSpecies( speciesId ).getName() ); //$NON-NLS-1$
				}
			}

			deleteSpecies( model, speciesIdsToRemove, true );

			final float numReactions = model.getNumReactions();
			final float newProportionBalanced = ( numReactions - task.getUnbalancedReactions().size() ) / numReactions;

			if( Math.abs( proportionBalanced - newProportionBalanced ) < 1e-8 && speciesIdsToRemove.size() == 0 && !task.hasUpdatedSpecies() )
			{
				System.out.println();
				System.out.println( "Displaying unbalanced reactions, and problematic metabolites..." ); //$NON-NLS-1$
				task.printResult();
				System.out.println();
				break;
			}

			proportionBalanced = newProportionBalanced;
			// System.out.println( proportionBalanced );
			// System.out.println();
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void deleteDuplicateSpecies( final Model model ) throws Exception
	{
		final Map<String,String> tagToSpeciesId = new HashMap<>();

		for( Species species : model.getListOfSpecies() )
		{
			final String tag = getTag( model, species );

			if( tag != null )
			{
				final String speciesMatchId = tagToSpeciesId.get( tag );

				if( speciesMatchId == null )
				{
					tagToSpeciesId.put( tag, species.getId() );
				}
				else
				{
					final String preferredId = getPreferredId( species.getId(), speciesMatchId );

					if( preferredId.equals( speciesMatchId ) )
					{
						System.out.println( "REPLACEMENT:\t" + species.getId() + "\t" + speciesMatchId ); //$NON-NLS-1$ //$NON-NLS-2$
						updateReactions( model.getListOfReactions(), species.getId(), speciesMatchId );
					}
					else
					{
						System.out.println( "REPLACEMENT:\t" + speciesMatchId + "\t" + species.getId() ); //$NON-NLS-1$ //$NON-NLS-2$
						updateReactions( model.getListOfReactions(), speciesMatchId, species.getId() );
						tagToSpeciesId.put( tag, species.getId() );
					}
				}
			}
		}
	}

	/**
	 * 
	 */
	public static void deleteDuplicateReactions( final Model model )
	{
		final Collection<String> reactionsToRemove = new HashSet<>();
		final Map<List<Set<String>>,String> componentsToReactionId = new HashMap<>();

		outer: for( Reaction reaction : model.getListOfReactions() )
		{
			final List<Set<String>> reactionParticipants = getReactionParticipants( reaction );

			String reactionMatchId = componentsToReactionId.get( reactionParticipants );

			if( reactionMatchId == null )
			{
				// Try flipping reaction:
				reactionMatchId = componentsToReactionId.get( flip( reactionParticipants ) );

				if( reactionMatchId == null )
				{
					componentsToReactionId.put( reactionParticipants, reaction.getId() );
					continue outer;
				}
			}

			final String preferredId = getPreferredReaction( reaction, model.getReaction( reactionMatchId ) );

			if( preferredId.equals( reactionMatchId ) )
			{
				System.out.println( "REPLACEMENT:\t" + reaction.getId() + "\t" + reactionMatchId ); //$NON-NLS-1$ //$NON-NLS-2$
				reactionsToRemove.add( reaction.getId() );
			}
			else
			{
				System.out.println( "REPLACEMENT:\t" + reactionMatchId + "\t" + reaction.getId() ); //$NON-NLS-1$ //$NON-NLS-2$
				reactionsToRemove.add( reactionMatchId );
				componentsToReactionId.put( reactionParticipants, reaction.getId() );
			}
		}

		deleteReactions( model, reactionsToRemove );
	}

	/**
	 * 
	 * @param reactionParticipants
	 * @return List<Set<String>>
	 */
	private static List<Set<String>> flip( List<Set<String>> reactionParticipants )
	{
		final int REACTANTS = 0;
		final int PRODUCTS = 1;
		final List<Set<String>> flippedList = new ArrayList<>();
		flippedList.add( reactionParticipants.get( PRODUCTS ) );
		flippedList.add( reactionParticipants.get( REACTANTS ) );
		return flippedList;
	}

	/**
	 * 
	 * @param model
	 */
	public static void deletePointlessReactions( final Model model )
	{
		final int REACTANTS = 0;
		final int PRODUCTS = 1;
		final Collection<String> reactionsToRemove = new HashSet<>();

		for( Reaction reaction : model.getListOfReactions() )
		{
			final List<Set<String>> reactionParticipants = getReactionParticipants( reaction );

			if( reactionParticipants.get( REACTANTS ).equals( reactionParticipants.get( PRODUCTS ) ) )
			{
				reactionsToRemove.add( reaction.getId() );
			}
		}

		deleteReactions( model, reactionsToRemove );
	}

	/**
	 * 
	 * @param model
	 */
	public static void deleteOrphanSpecies( final Model model )
	{
		final Set<String> allSpecies = new TreeSet<>();

		for( Species species : model.getListOfSpecies() )
		{
			allSpecies.add( species.getId() );
		}

		for( Reaction reaction : model.getListOfReactions() )
		{
			for( SpeciesReference ref : reaction.getListOfReactants() )
			{
				allSpecies.remove( ref.getSpecies() );
			}

			for( SpeciesReference ref : reaction.getListOfProducts() )
			{
				allSpecies.remove( ref.getSpecies() );
			}

			for( ModifierSpeciesReference ref : reaction.getListOfModifiers() )
			{
				allSpecies.remove( ref.getSpecies() );
			}
		}

		SbmlDeleter.deleteSpecies( model, allSpecies, false );
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @param deleteReaction
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	public static void deleteSpecies( final Model model, final File file, final boolean deleteReaction ) throws FileNotFoundException, IOException, Exception
	{
		deleteSpecies( model, FileUtils.readList( file ), deleteReaction );
	}

	/**
	 * 
	 * @param model
	 * @param speciesIdRegExpsToRemove
	 * @param deleteReaction
	 */
	public static void deleteSpecies( final Model model, final Collection<String> speciesIdRegExpsToRemove, final boolean deleteReaction )
	{
		final Collection<String> speciesIdsToRemove = new HashSet<>();

		for( String speciesIdRegExpToRemove : speciesIdRegExpsToRemove )
		{
			for( Species species : model.getListOfSpecies() )
			{
				if( species.getId().matches( speciesIdRegExpToRemove ) )
				{
					speciesIdsToRemove.add( species.getId() );
				}
			}
		}

		for( String speciesIdToRemove : speciesIdsToRemove )
		{
			System.out.println( "DELETING:\t" + speciesIdToRemove ); //$NON-NLS-1$
			model.removeSpecies( speciesIdToRemove );
		}

		final Set<String> reactionIdsToRemove = new HashSet<>();

		for( Reaction reaction : model.getListOfReactions() )
		{
			check( reaction, reaction.getListOfReactants(), speciesIdsToRemove, deleteReaction, reactionIdsToRemove );
			check( reaction, reaction.getListOfProducts(), speciesIdsToRemove, deleteReaction, reactionIdsToRemove );
		}

		deleteReactions( model, reactionIdsToRemove );
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	public static void deleteReactions( final Model model, final File file ) throws FileNotFoundException, IOException, Exception
	{
		deleteReactions( model, FileUtils.readList( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param reactionIdPatternsToRemove
	 */
	public static void deleteReactions( final Model model, final Collection<String> reactionIdPatternsToRemove )
	{
		final Collection<String> reactionIdsToRemove = new LinkedHashSet<>();

		for( Reaction reaction : model.getListOfReactions() )
		{
			for( String reactionIdPatternToRemove : reactionIdPatternsToRemove )
			{
				if( reaction.getId().matches( reactionIdPatternToRemove ) )
				{
					reactionIdsToRemove.add( reaction.getId() );
				}
			}
		}

		for( String reactionIdToRemove : reactionIdsToRemove )
		{
			System.out.println( "DELETING:\t" + reactionIdToRemove ); //$NON-NLS-1$
			model.removeReaction( reactionIdToRemove );
		}
	}

	/**
	 * 
	 * @param model
	 */
	public static void deleteModifiers( final Model model )
	{
		final Collection<String> speciesIdsToRemove = new HashSet<>();

		for( Reaction reaction : model.getListOfReactions() )
		{
			for( ModifierSpeciesReference ref : reaction.getListOfModifiers() )
			{
				speciesIdsToRemove.add( ref.getSpecies() );
			}

			reaction.unsetListOfModifiers();
		}

		deleteSpecies( model, speciesIdsToRemove, false );
	}

	/**
	 * 
	 * @param document
	 * @throws XMLStreamException
	 */
	public static void checkMetaIds( final SBMLDocument document ) throws XMLStreamException
	{
		checkMetaId( document );

		final Model model = document.getModel();
		checkMetaId( model );

		for( final UnitDefinition unitDefinition : model.getListOfUnitDefinitions() )
		{
			checkMetaId( unitDefinition );

			for( final Unit unit : unitDefinition.getListOfUnits() )
			{
				checkMetaId( unit );
			}
		}

		for( final SBase sbase : model.getListOfCompartments() )
		{
			checkMetaId( sbase );
		}

		for( final SBase sbase : model.getListOfSpecies() )
		{
			checkMetaId( sbase );
		}

		for( final Reaction reaction : model.getListOfReactions() )
		{
			checkMetaId( reaction );

			final KineticLaw kineticLaw = reaction.getKineticLaw();
			checkMetaId( kineticLaw );

			for( final SBase sbase : kineticLaw.getListOfLocalParameters() )
			{
				checkMetaId( sbase );
			}
		}
	}

	/**
	 * 
	 * @param sbase
	 * @throws XMLStreamException
	 */
	public static void checkMetaId( final SBase sbase ) throws XMLStreamException
	{
		if( sbase.getAnnotationString() == null || sbase.getAnnotationString().trim().length() == 0 )
		{
			sbase.unsetMetaId();
		}
		else if( sbase instanceof NamedSBase )
		{
			sbase.setMetaId( META_ID_PREFIX + ( (NamedSBase)sbase ).getId() );
		}
	}

	/**
	 * 
	 * @param reaction
	 * @param listOfRefs
	 * @param speciesIdsToRemove
	 * @param deleteReaction
	 * @param reactionIdsToRemove
	 */
	private static void check( final Reaction reaction, final ListOf<SpeciesReference> listOfRefs, final Collection<String> speciesIdsToRemove, final boolean deleteReaction, final Set<String> reactionIdsToRemove )
	{
		for( Iterator<SpeciesReference> iterator = listOfRefs.iterator(); iterator.hasNext(); )
		{
			final SpeciesReference ref = iterator.next();

			if( speciesIdsToRemove.contains( ref.getSpecies() ) )
			{
				if( deleteReaction )
				{
					reactionIdsToRemove.add( reaction.getId() );
					return;
				}
				// else
				iterator.remove();
			}
		}
	}

	/**
	 * 
	 * @param reaction
	 * @return List<Set<String>>
	 */
	private static List<Set<String>> getReactionParticipants( final Reaction reaction )
	{
		final String SEPARATOR = "###"; //$NON-NLS-1$
		final Set<String> reactants = new TreeSet<>();
		final Set<String> products = new TreeSet<>();

		for( SpeciesReference ref : reaction.getListOfReactants() )
		{
			final String tag = ref.getSpecies() + SEPARATOR + ref.getStoichiometry();
			reactants.add( tag );
		}

		for( SpeciesReference ref : reaction.getListOfProducts() )
		{
			final String tag = ref.getSpecies() + SEPARATOR + ref.getStoichiometry();
			products.add( tag );
		}

		final List<Set<String>> reactionParticipants = new ArrayList<>();
		reactionParticipants.add( reactants );
		reactionParticipants.add( products );

		return reactionParticipants;
	}

	/**
	 * 
	 * @param model
	 * @param species
	 * @return String
	 * @throws Exception
	 */
	private static String getTag( final Model model, final Species species ) throws Exception
	{
		final String SEPARATOR = "###"; //$NON-NLS-1$
		OntologyTerm ontologyTerm = SbmlUtils.getOntologyTerm( species, Ontology.CHEBI );

		if( ontologyTerm == null )
		{
			ontologyTerm = SbmlUtils.getOntologyTerm( species, Ontology.KEGG_COMPOUND );
		}

		if( ontologyTerm != null )
		{
			return ontologyTerm + SEPARATOR + species.getCompartment() + SEPARATOR + SbmlUtils.getFormula( model, species ) + SEPARATOR + SbmlUtils.getCharge( model, species );
		}

		return null;
	}

	/**
	 * 
	 * @param listOfReactions
	 * @param speciesRefId
	 * @param replacementSpeciesRefId
	 */
	private static void updateReactions( final ListOf<Reaction> listOfReactions, final String speciesRefId, final String replacementSpeciesRefId )
	{
		for( Reaction reaction : listOfReactions )
		{
			for( SpeciesReference ref : reaction.getListOfReactants() )
			{
				if( ref.getSpecies().equals( speciesRefId ) )
				{
					ref.setSpecies( replacementSpeciesRefId );
				}
			}

			for( SpeciesReference ref : reaction.getListOfProducts() )
			{
				if( ref.getSpecies().equals( speciesRefId ) )
				{
					ref.setSpecies( replacementSpeciesRefId );
				}
			}
		}
	}

	/**
	 * Preference is given to shorter, alphabetic names.
	 * 
	 * @param id1
	 * @param id2
	 * @return String
	 */
	private static String getPreferredId( final String id1, final String id2 )
	{
		if( rankAlphabetic( id1 ) > rankAlphabetic( id2 ) )
		{
			return id1;
		}

		return id2;
	}

	/**
	 * 
	 * @param reaction1
	 * @param reaction2
	 * @return String
	 */
	private static String getPreferredReaction( final Reaction reaction1, final Reaction reaction2 )
	{
		/*
		 * final boolean reaction1irreversible = reaction1.isSetReversible() &&
		 * !reaction1.isReversible(); final boolean reaction2irreversible =
		 * reaction2.isSetReversible() && !reaction2.isReversible();
		 * 
		 * if( reaction1irreversible && !reaction2irreversible ) { return
		 * reaction1.getId(); }
		 * 
		 * if( reaction2irreversible && !reaction1irreversible ) { return
		 * reaction2.getId(); }
		 */

		return getPreferredId( reaction1.getId(), reaction2.getId() );
	}

	/**
	 * 
	 * @param id1
	 * @return float
	 */
	private static float rankAlphabetic( final String id )
	{
		float alphabetic = 0;

		for( int i = 0; i < id.length(); i++ )
		{
			if( Character.isAlphabetic( id.charAt( i ) ) )
			{
				alphabetic++;
			}
		}

		return alphabetic / id.length();
	}
}