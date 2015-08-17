/**
 * 
 */
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
public class TransportExtracter extends Extracter
{
	/**
	 * 
	 */
	private static final double DEFAULT_STOICHIOMETRY = 1;

	/**
	 * 
	 */
	private static final Collection<String> media = new LinkedHashSet<>();

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
		final Compartment compartment = SubliminalUtils.addCompartment( model, EXTRACELLULAR_COMPARTMENT_ID, "extracellular" ); //$NON-NLS-1$

		final Collection<Object[]> resources = new ArrayList<>();
		resources.add( new Object[] { "http://identifiers.org/obo.go/GO:0005615", CVTerm.Qualifier.BQB_IS } ); //$NON-NLS-1$
		addResources( compartment, resources );

		addMedia( model );
		addExport( model );
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static void addMedia( final Model model ) throws Exception
	{
		for( String mediaId : media )
		{
			final Species extracellular = addSpecies( model, mediaId, mediaId, EXTRACELLULAR_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );
			extracellular.setBoundaryCondition( true );

			final Species intracellular = addSpecies( model, mediaId, mediaId, DEFAULT_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );

			final String reactionId = "R_" + mediaId + "_in"; //$NON-NLS-1$ //$NON-NLS-2$
			final Reaction reaction = model.createReaction();
			reaction.setId( reactionId );
			reaction.setName( extracellular.getName() + " (in)" ); //$NON-NLS-1$
			reaction.setSBOTerm( SubliminalUtils.SBO_TRANSPORT_REACTION );
			reaction.setReversible( false );

			final SpeciesReference reactant = reaction.createReactant();
			reactant.setSpecies( extracellular.getId() );
			reactant.setStoichiometry( DEFAULT_STOICHIOMETRY );

			final SpeciesReference product = reaction.createProduct();
			product.setSpecies( intracellular.getId() );
			product.setStoichiometry( DEFAULT_STOICHIOMETRY );
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	private static void addExport( final Model model ) throws Exception
	{
		final Collection<Species> intracellularSpecies = new ArrayList<>();

		for( Species species : model.getListOfSpecies() )
		{
			if( species.getSBOTerm() == SubliminalUtils.SBO_SIMPLE_CHEMICAL && species.getCompartment().equals( DEFAULT_COMPARTMENT_ID ) )
			{
				intracellularSpecies.add( species );
			}
		}

		for( Species intracellular : intracellularSpecies )
		{
			final String decompartmentalisedSpeciesId = SubliminalUtils.getDecompartmentalisedId( intracellular.getId(), intracellular.getCompartment() );

			final Species extracellular = addSpecies( model, decompartmentalisedSpeciesId, decompartmentalisedSpeciesId, EXTRACELLULAR_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );
			extracellular.setBoundaryCondition( true );

			final String reactionId = "R_" + decompartmentalisedSpeciesId + "_out"; //$NON-NLS-1$ //$NON-NLS-2$
			final Reaction reaction = model.createReaction();
			reaction.setId( reactionId );
			reaction.setName( extracellular.getName() + " (out)" ); //$NON-NLS-1$
			reaction.setSBOTerm( SubliminalUtils.SBO_TRANSPORT_REACTION );
			reaction.setReversible( false );

			final SpeciesReference reactant = reaction.createReactant();
			reactant.setSpecies( intracellular.getId() );
			reactant.setStoichiometry( DEFAULT_STOICHIOMETRY );

			final SpeciesReference product = reaction.createProduct();
			product.setSpecies( extracellular.getId() );
			product.setStoichiometry( DEFAULT_STOICHIOMETRY );
		}
	}

	/**
	 * 
	 */
	private static void init()
	{
		// Molecular Cell Biology. 4th edition. Lodish H, Berk A, Zipursky SL,
		// et al. New York: W. H. Freeman; 2000.
		media.add( "MNXM99" ); // alpha-D-Glucose //$NON-NLS-1$
		media.add( "MNXM105" ); // beta-D-Glucose //$NON-NLS-1$
		media.add( "MNXM15" ); // NH4+ //$NON-NLS-1$
		media.add( "MNXM27" ); // Na+ //$NON-NLS-1$
		media.add( "MNXM95" ); // K+ //$NON-NLS-1$
		media.add( "MNXM653" ); // Mg2+ //$NON-NLS-1$
		media.add( "MNXM128" ); // Ca2+ //$NON-NLS-1$
		media.add( "MNXM58" ); // SO4- //$NON-NLS-1$
		media.add( "MNXM43" ); // Cl- //$NON-NLS-1$
		media.add( "MNXM9" ); // PO4- //$NON-NLS-1$

		media.add( "MNXM1" ); // H+ //$NON-NLS-1$
		media.add( "MNXM2" ); // H2O //$NON-NLS-1$
		media.add( "MNXM13" ); // CO2 //$NON-NLS-1$
		media.add( "MNXM4" ); // O2 //$NON-NLS-1$
	}
}