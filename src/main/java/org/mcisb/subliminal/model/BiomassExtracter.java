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
package org.mcisb.subliminal.model;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.stream.*;
import org.mcisb.subliminal.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class BiomassExtracter extends Extracter
{
	/**
	 * 
	 */
	private static final double DEFAULT_BIOMASS_STOICHIOMETRY = 1;

	/**
	 * 
	 */
	private static final double DEFAULT_REACTANT_STOICHIOMETRY = 1;

	/**
	 * 
	 */
	private static final double DEFAULT_PRODUCT_STOICHIOMETRY = 1;

	/**
	 * 
	 */
	private static final double DEFAULT_AMINO_ACID_STOICHIOMETRY = 0.1;

	/**
	 * 
	 */
	private static final double DEFAULT_RNA_PRECURSOR_STOICHIOMETRY = 0.05;

	/**
	 * 
	 */
	private static final double DEFAULT_DNA_PRECURSOR_STOICHIOMETRY = 0.003;

	/**
	 * 
	 */
	private static final double DEFAULT_CARBOHYDRATE_STOICHIOMETRY = 2;

	/**
	 * 
	 */
	private static final double DEFAULT_ATP_REACTANT_STOICHIOMETRY = 20;

	/**
	 * 
	 */
	private static final double DEFAULT_ATP_PRODUCT_STOICHIOMETRY = 20;

	/**
	 * 
	 */
	private static final Collection<String> aminoAcids = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> rnaPrecursors = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> dnaPrecursors = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> carbohydrates = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> atpReactants = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> atpProducts = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> biomassComponents = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> biomassReactants = new LinkedHashSet<>();

	/**
	 * 
	 */
	private static final Collection<String> biomassProducts = new LinkedHashSet<>();

	/**
	 * 
	 */
	static
	{
		init();
	}

	/**
	 * 
	 * @param document
	 * @throws Exception
	 */
	public static void run( final SBMLDocument document ) throws Exception
	{
		final Model model = document.getModel();
		SubliminalUtils.addCompartment( model, BIOMASS_COMPARTMENT_ID, BIOMASS_COMPARTMENT_ID );
		addBiomassPrecursors( model );
		addBiomassReaction( model );
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static void addBiomassPrecursors( final Model model ) throws MalformedURLException, IOException, XMLStreamException, Exception
	{
		for( String biomassComponentId : biomassComponents )
		{
			final Species precursor = addSpecies( model, biomassComponentId, biomassComponentId, DEFAULT_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );
			final Species precursorBiomass = addSpecies( model, biomassComponentId, biomassComponentId, BIOMASS_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );

			final String reactionId = "R_" + biomassComponentId + "_" + BIOMASS_COMPARTMENT_ID; //$NON-NLS-1$ //$NON-NLS-2$
			final Reaction reaction = model.createReaction();
			reaction.setId( reactionId );
			reaction.setName( precursor.getName() + " (biomass precursor)" ); //$NON-NLS-1$
			reaction.setSBOTerm( SubliminalUtils.SBO_OMITTED_PROCESS );
			reaction.setReversible( true );

			final SpeciesReference reactant = reaction.createReactant();
			reactant.setSpecies( precursor.getId() );
			reactant.setStoichiometry( DEFAULT_REACTANT_STOICHIOMETRY );

			final SpeciesReference product = reaction.createProduct();
			product.setSpecies( precursorBiomass.getId() );
			product.setStoichiometry( DEFAULT_REACTANT_STOICHIOMETRY );
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	private static void addBiomassReaction( final Model model ) throws Exception
	{
		final String BIOMASS = "biomass"; //$NON-NLS-1$
		addReaction( model, SubliminalUtils.BIOMASS_REACTION, SubliminalUtils.BIOMASS_REACTION, biomassReactants, DEFAULT_BIOMASS_STOICHIOMETRY, biomassProducts, DEFAULT_BIOMASS_STOICHIOMETRY, BIOMASS, BIOMASS );

		final Species biomassSpecies = model.getSpecies( SubliminalUtils.getCompartmentalisedId( BIOMASS, BIOMASS_COMPARTMENT_ID ) );
		biomassSpecies.setBoundaryCondition( true );
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private static void addBiomassReactionPiecewise( final Model model ) throws Exception
	{
		final String AMINO_ACIDS_REQUIREMENT = "amino_acids_requirement"; //$NON-NLS-1$
		final String RNA_PRECURSORS_REQUIREMENT = "rna_precursors_requirement"; //$NON-NLS-1$
		final String DNA_PRECURSORS_REQUIREMENT = "dna_precursors_requirement"; //$NON-NLS-1$
		final String CARBOHYDRATES_REQUIREMENT = "carbohydrates_requirement"; //$NON-NLS-1$
		final String ATP_REQUIREMENT = "atp_requirement"; //$NON-NLS-1$
		final String BIOMASS_REQUIREMENT = "biomass_requirement"; //$NON-NLS-1$

		addReaction( model, "R_amino_acids_requirement", "R_amino_acids_requirement", aminoAcids, DEFAULT_AMINO_ACID_STOICHIOMETRY, AMINO_ACIDS_REQUIREMENT, AMINO_ACIDS_REQUIREMENT ); //$NON-NLS-1$ //$NON-NLS-2$
		addReaction( model, "R_rna_precursors_requirement", "R_rna_precursors_requirement", rnaPrecursors, DEFAULT_RNA_PRECURSOR_STOICHIOMETRY, RNA_PRECURSORS_REQUIREMENT, RNA_PRECURSORS_REQUIREMENT ); //$NON-NLS-1$ //$NON-NLS-2$
		addReaction( model, "R_dna_precursors_requirement", "R_dna_precursors_requirement", dnaPrecursors, DEFAULT_DNA_PRECURSOR_STOICHIOMETRY, DNA_PRECURSORS_REQUIREMENT, DNA_PRECURSORS_REQUIREMENT ); //$NON-NLS-1$ //$NON-NLS-2$
		addReaction( model, "R_carbohydrates_requirement", "R_carbohydrates_requirement", carbohydrates, DEFAULT_CARBOHYDRATE_STOICHIOMETRY, CARBOHYDRATES_REQUIREMENT, CARBOHYDRATES_REQUIREMENT ); //$NON-NLS-1$ //$NON-NLS-2$
		addReaction( model, "R_atp_requirement", "R_atp_requirement", atpReactants, DEFAULT_ATP_REACTANT_STOICHIOMETRY, atpProducts, DEFAULT_ATP_PRODUCT_STOICHIOMETRY, ATP_REQUIREMENT, ATP_REQUIREMENT ); //$NON-NLS-1$ //$NON-NLS-2$

		final Collection<String> reactantIds = new ArrayList<String>();
		reactantIds.add( AMINO_ACIDS_REQUIREMENT );
		reactantIds.add( RNA_PRECURSORS_REQUIREMENT );
		reactantIds.add( DNA_PRECURSORS_REQUIREMENT );
		reactantIds.add( CARBOHYDRATES_REQUIREMENT );
		reactantIds.add( ATP_REQUIREMENT );

		addReaction( model, SubliminalUtils.BIOMASS_REACTION, SubliminalUtils.BIOMASS_REACTION, reactantIds, DEFAULT_BIOMASS_STOICHIOMETRY, BIOMASS_REQUIREMENT, BIOMASS_REQUIREMENT );

		final Species biomassSpecies = model.getSpecies( SubliminalUtils.getCompartmentalisedId( BIOMASS_REQUIREMENT, BIOMASS_COMPARTMENT_ID ) );
		biomassSpecies.setBoundaryCondition( true );
	}

	/**
	 * 
	 * @param model
	 * @param reactionId
	 * @param reactionName
	 * @param reactantIds
	 * @param reactantStoichiometry
	 * @param productId
	 * @param productName
	 * @return Reaction
	 * @throws Exception
	 */
	private static Reaction addReaction( final Model model, final String reactionId, final String reactionName, final Collection<String> reactantIds, final double reactantStoichiometry, final String productId, final String productName ) throws Exception
	{
		return addReaction( model, reactionId, reactionName, reactantIds, reactantStoichiometry, new ArrayList<String>(), SubliminalUtils.UNDEFINED_NUMBER, productId, productName );
	}

	/**
	 * 
	 * @param model
	 * @param reactionId
	 * @param reactionName
	 * @param reactantIds
	 * @param reactantStoichiometry
	 * @param productIds
	 * @param productStoichiometry
	 * @param productId
	 * @param productName
	 * @return Reaction
	 * @throws Exception
	 */
	private static Reaction addReaction( final Model model, final String reactionId, final String reactionName, final Collection<String> reactantIds, final double reactantStoichiometry, final Collection<String> productIds, final double productStoichiometry, final String productId, final String productName ) throws Exception
	{
		final Reaction reaction = model.createReaction();
		reaction.setId( reactionId );
		reaction.setName( reactionName );
		reaction.setReversible( false );
		reaction.setSBOTerm( SubliminalUtils.SBO_OMITTED_PROCESS );

		addBiomassSpecies( model, reaction, reactantIds, reactantStoichiometry, true );
		addBiomassSpecies( model, reaction, productIds, productStoichiometry, false );

		final Species productSpecies = model.createSpecies();
		final String compartmentalisedProductId = SubliminalUtils.getCompartmentalisedId( productId, BIOMASS_COMPARTMENT_ID );
		productSpecies.setId( compartmentalisedProductId );
		productSpecies.setName( productName );
		productSpecies.setCompartment( BIOMASS_COMPARTMENT_ID );
		productSpecies.setInitialConcentration( SubliminalUtils.DEFAULT_INITIAL_CONCENTRATION );

		final SpeciesReference productSpeciesReference = reaction.createProduct();
		productSpeciesReference.setSpecies( compartmentalisedProductId );
		productSpeciesReference.setStoichiometry( DEFAULT_PRODUCT_STOICHIOMETRY );

		return reaction;
	}

	/**
	 * 
	 * @param reaction
	 * @param participantIds
	 * @param stoichiometry
	 * @param isReactant
	 * @throws Exception
	 */
	private static void addBiomassSpecies( final Model model, final Reaction reaction, final Collection<String> participantIds, final double stoichiometry, final boolean isReactant ) throws Exception
	{
		for( String participantId : participantIds )
		{
			final Species species = addSpecies( model, participantId, participantId, BIOMASS_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );
			final SpeciesReference outReactant = isReactant ? reaction.createReactant() : reaction.createProduct();
			outReactant.setSpecies( species.getId() );
			outReactant.setStoichiometry( stoichiometry );
		}
	}

	/**
	 * 
	 */
	private static void init()
	{
		aminoAcids.add( "MNXM29" ); // glycine //$NON-NLS-1$
		aminoAcids.add( "MNXM76" ); // L-tyrosine //$NON-NLS-1$
		aminoAcids.add( "MNXM42" ); // L-aspartate //$NON-NLS-1$
		aminoAcids.add( "MNXM78" ); // L-lysine //$NON-NLS-1$
		aminoAcids.add( "MNXM142" ); // L-threonine //$NON-NLS-1$
		aminoAcids.add( "MNXM231" ); // L-isoleucine //$NON-NLS-1$
		aminoAcids.add( "MNXM140" ); // L-leucine //$NON-NLS-1$
		aminoAcids.add( "MNXM199" ); // L-valine //$NON-NLS-1$
		aminoAcids.add( "MNXM70" ); // L-arginine //$NON-NLS-1$
		aminoAcids.add( "MNXM147" ); // L-asparagine //$NON-NLS-1$
		aminoAcids.add( "MNXM89557" ); // L-glutamate //$NON-NLS-1$
		aminoAcids.add( "MNXM37" ); // L-glutamine //$NON-NLS-1$
		aminoAcids.add( "MNXM114" ); // L-proline //$NON-NLS-1$
		aminoAcids.add( "MNXM134" ); // L-histidine //$NON-NLS-1$
		aminoAcids.add( "MNXM32" ); // L-alanine //$NON-NLS-1$
		aminoAcids.add( "MNXM53" ); // L-serine //$NON-NLS-1$
		aminoAcids.add( "MNXM55" ); // L-cysteine //$NON-NLS-1$
		aminoAcids.add( "MNXM61" ); // L-methionine //$NON-NLS-1$
		aminoAcids.add( "MNXM97" ); // L-phenylalanine //$NON-NLS-1$
		aminoAcids.add( "MNXM94" ); // L-tryptophan //$NON-NLS-1$

		rnaPrecursors.add( "MNXM14" ); // AMP //$NON-NLS-1$
		rnaPrecursors.add( "MNXM113" ); // GMP //$NON-NLS-1$
		rnaPrecursors.add( "MNXM31" ); // CMP //$NON-NLS-1$
		rnaPrecursors.add( "MNXM80" ); // UMP //$NON-NLS-1$

		dnaPrecursors.add( "MNXM432" ); // dAMP //$NON-NLS-1$
		dnaPrecursors.add( "MNXM546" ); // dGMP //$NON-NLS-1$
		dnaPrecursors.add( "MNXM266" ); // dCMP //$NON-NLS-1$
		dnaPrecursors.add( "MNXM257" ); // dTMP //$NON-NLS-1$

		carbohydrates.add( "MNXM95017" ); // Glycogen //$NON-NLS-1$

		atpReactants.add( "MNXM3" ); // ATP //$NON-NLS-1$

		atpProducts.add( "MNXM7" ); // ADP //$NON-NLS-1$
		atpProducts.add( "MNXM9" ); // Phosphate //$NON-NLS-1$

		biomassComponents.addAll( aminoAcids );
		biomassComponents.addAll( rnaPrecursors );
		biomassComponents.addAll( dnaPrecursors );
		biomassComponents.addAll( carbohydrates );
		biomassComponents.addAll( atpReactants );
		biomassComponents.addAll( atpProducts );

		biomassReactants.addAll( aminoAcids );
		biomassReactants.addAll( rnaPrecursors );
		biomassReactants.addAll( dnaPrecursors );
		biomassReactants.addAll( carbohydrates );
		biomassReactants.addAll( atpReactants );
		biomassProducts.addAll( atpProducts );
	}
}