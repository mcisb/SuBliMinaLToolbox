package org.mcisb.subliminal;

import java.io.*;
import java.util.*;
import org.mcisb.subliminal.mnxref.*;
import org.mcisb.subliminal.sbml.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public abstract class Extracter
{
	/**
	 * 
	 */
	private static final int RESOURCE = 0;

	/**
	 * 
	 */
	private static final int QUALIFIER = 1;

	/**
	 * 
	 */
	private static final int ID = 0;

	/**
	 * 
	 */
	private static final int NAME = 1;

	/**
	 * 
	 */
	protected static final String DEFAULT_COMPARTMENT_ID = "i"; //$NON-NLS-1$

	/**
	 * 
	 */
	protected static final String EXTRACELLULAR_COMPARTMENT_ID = "e"; //$NON-NLS-1$

	/**
	 * 
	 */
	protected static final String BIOMASS_COMPARTMENT_ID = "bm"; //$NON-NLS-1$

	/**
	 * 
	 * @param taxonomyId
	 * @return SBMLDocument
	 * @throws IOException
	 */
	protected static SBMLDocument initDocument( final String taxonomyId ) throws IOException
	{
		final SBMLDocument document = new SBMLDocument( SubliminalUtils.DEFAULT_LEVEL, SubliminalUtils.DEFAULT_VERSION );
		final Model model = document.createModel( SubliminalUtils.getUniqueId() );
		model.setName( SubliminalUtils.getTaxonomyName( taxonomyId ) );

		addDefaultCompartment( model );

		final Collection<Object[]> resources = new ArrayList<>();
		resources.add( new Object[] { "http://identifiers.org/taxonomy/" + taxonomyId, CVTerm.Qualifier.BQB_OCCURS_IN } ); //$NON-NLS-1$
		addResources( model, resources );

		SubliminalUtils.addHistory( document );

		return document;
	}

	/**
	 * 
	 * @param model
	 */
	protected static void addDefaultCompartment( final Model model )
	{
		final String DEFAULT_NAME = "intracellular"; //$NON-NLS-1$
		final Compartment compartment = SubliminalUtils.addCompartment( model, DEFAULT_COMPARTMENT_ID, DEFAULT_NAME );

		final String DEFAULT_GO_TERM_RESOURCE = "http://identifiers.org/obo.go/GO:0005622"; //$NON-NLS-1$
		final Collection<Object[]> resources = new ArrayList<>();
		resources.add( new Object[] { DEFAULT_GO_TERM_RESOURCE, CVTerm.Qualifier.BQB_IS } );
		addResources( compartment, resources );
	}

	/**
	 * 
	 * @param model
	 * @param speciesId
	 * @param name
	 * @param compartmentId
	 * @param sboTerm
	 * @return Species
	 * @throws Exception
	 */
	protected static Species addSpecies( final Model model, final String speciesId, final String name, final String compartmentId, final int sboTerm ) throws Exception
	{
		return addSpecies( model, speciesId, name, compartmentId, sboTerm, new ArrayList<Object[]>() );
	}

	/**
	 * 
	 * @param model
	 * @param speciesId
	 * @param name
	 * @param sboTerm
	 * @param resources
	 * @return Species
	 * @throws Exception
	 */
	protected static Species addSpecies( final Model model, final String speciesId, final String name, final String compartmentId, final int sboTerm, final Collection<Object[]> resources ) throws Exception
	{
		final String compartmentalisedSpeciesId = SubliminalUtils.getCompartmentalisedId( speciesId, compartmentId );
		Species species = model.getSpecies( compartmentalisedSpeciesId );

		if( species == null )
		{
			species = SbmlFactory.getInstance().getSpecies( speciesId, compartmentId );

			if( species != null )
			{
				if( model.getSpecies( species.getId() ) == null )
				{
					model.addSpecies( species );
				}
			}
			else
			{
				final String normalisedId = SubliminalUtils.getNormalisedId( compartmentalisedSpeciesId );
				species = model.getSpecies( normalisedId );

				if( species == null )
				{
					species = model.createSpecies();
					species.setId( normalisedId );
					species.setName( name );
					species.setCompartment( model.getCompartment( compartmentId ) );
					species.setInitialConcentration( SubliminalUtils.DEFAULT_INITIAL_CONCENTRATION );
					species.setSBOTerm( sboTerm );
					addResources( species, resources );
				}
			}
		}

		return species;
	}

	/**
	 * 
	 * @param model
	 * @param reactionId
	 * @return Reaction
	 * @throws Exception
	 */
	protected static Reaction addReaction( final Model model, final String reactionId, final String compartmentId ) throws Exception
	{
		Reaction reaction = SbmlFactory.getInstance().getReaction( reactionId, compartmentId );

		if( reaction != null && model.getReaction( reaction.getId() ) == null )
		{
			// Add reactants and products:
			for( SpeciesReference reactant : reaction.getListOfReactants() )
			{
				final String speciesId = SubliminalUtils.getDecompartmentalisedId( reactant.getSpecies(), compartmentId );
				addSpecies( model, speciesId, MxnRefChemUtils.getInstance().getName( speciesId ), compartmentId, SubliminalUtils.SBO_SIMPLE_CHEMICAL );
			}

			for( SpeciesReference product : reaction.getListOfProducts() )
			{
				final String speciesId = SubliminalUtils.getDecompartmentalisedId( product.getSpecies(), compartmentId );
				addSpecies( model, speciesId, MxnRefChemUtils.getInstance().getName( speciesId ), compartmentId, SubliminalUtils.SBO_SIMPLE_CHEMICAL );
			}

			model.addReaction( reaction );
		}

		return reaction;
	}

	/**
	 * 
	 * @param reaction
	 * @param uniProtResults
	 * @param geneId
	 * @param geneName
	 * @param resources
	 * @throws Exception
	 */
	protected static void addEnzymes( final Reaction reaction, final List<String[]> uniProtResults, final String geneId, final String geneName, final Collection<Object[]> resources ) throws Exception
	{
		if( uniProtResults.size() > 0 )
		{
			int i = 0;

			for( String[] result : uniProtResults )
			{
				final String updatedGeneId = geneId + ( ( ++i > 1 ) ? ( "_" + i ) : "" ); //$NON-NLS-1$ //$NON-NLS-2$

				final String uniProtResource = "http://identifiers.org/uniprot/" + result[ ID ]; //$NON-NLS-1$
				final Collection<Object[]> updatedResources = new ArrayList<>( resources );
				updatedResources.add( new Object[] { uniProtResource, CVTerm.Qualifier.BQB_IS } );
				addEnzyme( reaction, updatedGeneId, result[ NAME ], updatedResources );
			}
		}
		else
		{
			addEnzyme( reaction, geneId, geneName, resources );
		}
	}

	/**
	 * 
	 * @param reaction
	 * @param id
	 * @param name
	 * @param resources
	 * @throws Exception
	 */
	protected static void addEnzyme( final Reaction reaction, final String id, final String name, final Collection<Object[]> resources ) throws Exception
	{
		final Species species = addSpecies( reaction.getModel(), id, name, DEFAULT_COMPARTMENT_ID, SubliminalUtils.SBO_POLYPEPTIDE_CHAIN, resources );
		species.setBoundaryCondition( true );

		final String speciesId = species.getId();
		final String modifierId = reaction.getId() + SubliminalUtils.UNDERSCORE + speciesId;

		if( reaction.getModifier( modifierId ) == null )
		{
			final ModifierSpeciesReference modifier = reaction.createModifier();
			modifier.setId( modifierId );
			modifier.setSpecies( SubliminalUtils.getNormalisedId( speciesId ) );
		}
	}

	/**
	 * 
	 * @param sbase
	 * @param resources
	 */
	protected static void addResources( final SBase sbase, final Collection<Object[]> resources )
	{
		for( Object[] resource : resources )
		{
			SubliminalUtils.addCVTerm( sbase, (String)resource[ RESOURCE ], CVTerm.Type.BIOLOGICAL_QUALIFIER, (CVTerm.Qualifier)resource[ QUALIFIER ] );
		}
	}
}