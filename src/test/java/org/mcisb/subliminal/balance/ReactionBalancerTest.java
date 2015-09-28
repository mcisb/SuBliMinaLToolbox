/**
 * 
 */
package org.mcisb.subliminal.balance;

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import org.junit.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.chebi.*;
import org.mcisb.sbml.*;
import org.sbml.jsbml.*;

/**
 * @author neilswainston
 * 
 */
public class ReactionBalancerTest
{
	/**
	 * 
	 */
	private static final int MAX_STOICHIOMETRIC_COEFFICIENT = 8;

	/**
	 * 
	 */
	private final Set<ChebiTerm> cofactors = new LinkedHashSet<>();

	/**
	 * 
	 * @throws Exception
	 */
	public ReactionBalancerTest() throws Exception
	{
		final String PROTON = "CHEBI:24636"; //$NON-NLS-1$
		final String WATER = "CHEBI:15377"; //$NON-NLS-1$
		final OntologyUtils ontologyUtils = OntologyUtils.getInstance();
		cofactors.add( (ChebiTerm)ontologyUtils.getOntologyTerm( Ontology.CHEBI, PROTON ) );
		cofactors.add( (ChebiTerm)ontologyUtils.getOntologyTerm( Ontology.CHEBI, WATER ) );
	}

	/**
	 * @throws Exception
	 * 
	 */
	@Test
	public void balanceRepeatingUnits() throws Exception
	{
		final SBMLDocument document = new SBMLDocument( 2, 4 );
		final Model model = document.createModel();

		final Compartment compartment = model.createCompartment();

		final Species species1 = createSpecies( model, "SPECIES_1", compartment, "CH" ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species species2 = createSpecies( model, "SPECIES_2", compartment, "(C)nH" ); //$NON-NLS-1$ //$NON-NLS-2$

		final String reactionId = "reaction"; //$NON-NLS-1$
		final Reaction reaction = model.createReaction( reactionId );
		reaction.createReactant( species1 ).setStoichiometry( 5 );
		reaction.createProduct( species2 ).setStoichiometry( 4 );

		balance( model, reaction );

		Assert.assertTrue( reaction.getReactant( 0 ).getStoichiometry() == 4.0 );
		Assert.assertTrue( reaction.getProduct( 0 ).getStoichiometry() == 4.0 );
	}

	/**
	 * @throws Exception
	 * 
	 */
	@Test
	public void balanceFractional() throws Exception
	{
		final SBMLDocument document = new SBMLDocument( 2, 4 );
		document.setNamespace("http://www.w3.org/1999/xhtml"); //$NON-NLS-1$
		final Model model = document.createModel();

		final Compartment compartment = model.createCompartment();

		final Species species1 = createSpecies( model, "SPECIES_1", compartment, "C" ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species species2 = createSpecies( model, "SPECIES_2", compartment, "C" ); //$NON-NLS-1$ //$NON-NLS-2$

		final String reactionId = "reaction"; //$NON-NLS-1$
		final Reaction reaction = model.createReaction( reactionId );
		reaction.createReactant( species1 ).setStoichiometry( 0.5 );
		reaction.createProduct( species2 ).setStoichiometry( 0.5 );

		balance( model, reaction );

		Assert.assertTrue( reaction.getReactant( 0 ).getStoichiometry() == 1 );
		Assert.assertTrue( reaction.getProduct( 0 ).getStoichiometry() == 1 );
	}

	/**
	 * @throws Exception
	 * 
	 */
	@Test
	public void balanceFaox() throws Exception
	{
		final SBMLDocument document = new SBMLDocument( 2, 4 );
		final Model model = document.createModel();

		final Compartment compartment = model.createCompartment( "c" ); //$NON-NLS-1$

		final Species h2o = createSpecies( model, "M_h2o_m", compartment, "H2O" ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species coa = createSpecies( model, "M_coa_m", compartment, "C21H32N7O16P3S", -4 ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species nad = createSpecies( model, "M_nad_m", compartment, "C21H26N7O14P2", -1 ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species fad = createSpecies( model, "M_fad_m", compartment, "C27H33N9O15P2" ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species tdcoa = createSpecies( model, "M_tdcoa_m", compartment, "C35H58N7O17P3S", -4 ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species h = createSpecies( model, "M_h_m", compartment, "H", 1 ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species nadh = createSpecies( model, "M_nadh_m", compartment, "C21H27N7O14P2", -2 ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species accoa = createSpecies( model, "M_accoa_m", compartment, "C23H34N7O17P3S", -4 ); //$NON-NLS-1$ //$NON-NLS-2$
		final Species fadh = createSpecies( model, "M_fadh2_m", compartment, "C27H33N9O15P2", -2 ); //$NON-NLS-1$ //$NON-NLS-2$

		final String PROTON = "CHEBI:24636"; //$NON-NLS-1$
		SbmlUtils.addOntologyTerm( h, OntologyUtils.getInstance().getOntologyTerm( Ontology.CHEBI, PROTON ), CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS );

		final String reactionId = "reaction"; //$NON-NLS-1$
		final Reaction reaction = model.createReaction( reactionId );
		reaction.createReactant( h2o ).setStoichiometry( 6 );
		reaction.createReactant( coa ).setStoichiometry( 6 );
		reaction.createReactant( nad ).setStoichiometry( 6 );
		reaction.createReactant( fad ).setStoichiometry( 6 );
		reaction.createReactant( tdcoa ).setStoichiometry( 1 );
		reaction.createProduct( h ).setStoichiometry( 6 );
		reaction.createProduct( nadh ).setStoichiometry( 6 );
		reaction.createProduct( accoa ).setStoichiometry( 7 );
		reaction.createProduct( fadh ).setStoichiometry( 8 );

		balance( model, reaction );

		Assert.assertTrue( reaction.getReactant( 0 ).getStoichiometry() == 6.0 );
		Assert.assertTrue( reaction.getReactant( 1 ).getStoichiometry() == 6.0 );
		Assert.assertTrue( reaction.getReactant( 2 ).getStoichiometry() == 6.0 );
		Assert.assertTrue( reaction.getReactant( 3 ).getStoichiometry() == 6.0 );
		Assert.assertTrue( reaction.getReactant( 4 ).getStoichiometry() == 1.0 );
		Assert.assertTrue( reaction.getProduct( 0 ).getStoichiometry() == 18.0 );
		Assert.assertTrue( reaction.getProduct( 1 ).getStoichiometry() == 6.0 );
		Assert.assertTrue( reaction.getProduct( 2 ).getStoichiometry() == 7.0 );
		Assert.assertTrue( reaction.getProduct( 3 ).getStoichiometry() == 6.0 );
	}

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @throws Exception
	 */
	private void balance( final Model model, final Reaction reaction ) throws Exception
	{
		final ReactionBalancer reactionBalancer = new ReactionBalancer( model, reaction, cofactors, MAX_STOICHIOMETRIC_COEFFICIENT );
		Assert.assertTrue( reactionBalancer.balance() );
	}

	/**
	 * 
	 * @param model
	 * @param id
	 * @param compartment
	 * @param formula
	 * @return Species
	 * @throws UnsupportedEncodingException
	 * @throws XMLStreamException
	 */
	private static Species createSpecies( final Model model, final String id, final Compartment compartment, final String formula ) throws UnsupportedEncodingException, XMLStreamException
	{
		final int DEFAULT_CHARGE = 0;
		return createSpecies( model, id, compartment, formula, DEFAULT_CHARGE );
	}

	/**
	 * 
	 * @param model
	 * @param id
	 * @param compartment
	 * @param formula
	 * @param charge
	 * @return Species
	 * @throws UnsupportedEncodingException
	 * @throws XMLStreamException
	 */
	private static Species createSpecies( final Model model, final String id, final Compartment compartment, final String formula, final int charge ) throws UnsupportedEncodingException, XMLStreamException
	{
		final Species species = model.createSpecies( id, compartment );
		species.setName( id );
		SbmlUtils.setFormula( species, formula );
		SbmlUtils.setCharge( species, charge );
		return species;
	}
}