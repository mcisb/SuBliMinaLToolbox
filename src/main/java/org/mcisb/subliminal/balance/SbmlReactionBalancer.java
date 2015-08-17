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
package org.mcisb.subliminal.balance;

import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.chebi.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlReactionBalancer
{
	/**
	 * 
	 */
	public final static int IS_BALANCED = 0;

	/**
	 * 
	 */
	public final static int UPDATED_SPECIES = 1;

	/**
	 * 
	 */
	private final Set<ChebiTerm> cofactors = new LinkedHashSet<>();

	/**
	 * 
	 * @throws Exception
	 */
	public SbmlReactionBalancer() throws Exception
	{
		final String PROTON = "CHEBI:24636"; //$NON-NLS-1$
		final String WATER = "CHEBI:15377"; //$NON-NLS-1$
		final OntologyUtils ontologyUtils = OntologyUtils.getInstance();
		cofactors.add( (ChebiTerm)ontologyUtils.getOntologyTerm( Ontology.CHEBI, PROTON ) );
		cofactors.add( (ChebiTerm)ontologyUtils.getOntologyTerm( Ontology.CHEBI, WATER ) );
	}

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @param maxStoichiometricCoefficient
	 * @return Map<String,Double>
	 * @throws Exception
	 */
	public Map<String,Double> isBalanced( final Model model, final Reaction reaction, final int maxStoichiometricCoefficient ) throws Exception
	{
		return new ReactionBalancer( model, reaction, cofactors, maxStoichiometricCoefficient ).isBalanced();
	}

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @param maxStoichiometricCoefficient
	 * @return Map<String,Double>
	 * @throws Exception
	 */
	public Map<String,Double> isBalancedResetStoichiometry( final Model model, final Reaction reaction, final int maxStoichiometricCoefficient ) throws Exception
	{
		return new ReactionBalancer( model, reaction, cofactors, maxStoichiometricCoefficient ).isBalancedResetStoichiometry();
	}

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @param maxStoichiometricCoefficient
	 * @return boolean[]
	 * @throws Exception
	 */
	public Object[] balance( final Model model, final Reaction reaction, final int maxStoichiometricCoefficient ) throws Exception
	{
		final ReactionBalancer balancer = new ReactionBalancer( model, reaction, cofactors, maxStoichiometricCoefficient );
		return new Object[] { Boolean.valueOf( balancer.balance() ), balancer.getUpdatedSpeciesIds() };
	}

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @param maxStoichiometricCoefficient
	 * @return boolean[]
	 * @throws Exception
	 */
	public Object[] balanceMinimiseStoichiometries( final Model model, final Reaction reaction, final int maxStoichiometricCoefficient ) throws Exception
	{
		final ReactionBalancer balancer = new ReactionBalancer( model, reaction, cofactors, maxStoichiometricCoefficient );
		return new Object[] { Boolean.valueOf( balancer.balanceMinimiseStoichiometries() ), balancer.getUpdatedSpeciesIds() };
	}
}