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
import org.gnu.glpk.*;
import org.mcisb.math.linearprogramming.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.OntologyUtils.MatchCriteria;
import org.mcisb.ontology.chebi.*;
import org.mcisb.ontology.sbo.*;
import org.mcisb.sbml.*;
import org.mcisb.util.*;
import org.mcisb.util.chem.*;
import org.mcisb.util.math.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class ReactionBalancer
{
	/**
	 * 
	 */
	private static final int REPEATING_UNIT_MAX_STOICHIOMETRIC_COEFFICIENT = Integer.MAX_VALUE;

	/**
	 * 
	 */
	private static final int COFACTOR_MAX_STOICHIOMETRIC_COEFFICIENT = Integer.MAX_VALUE;

	/**
	 * 
	 */
	private static final Integer ZERO = Integer.valueOf( 0 );

	/**
	 * 		
	 */
	private static final int FIRST = 0;

	/**
	 * 
	 */
	private static final String CHARGE = "CHARGE"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final int DEFAULT_CHARGE = 0;

	/**
	 * 
	 */
	private static final String PROTON = "CHEBI:24636"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String HYDRON = "CHEBI:15378"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final int TIMEOUT = 1000;

	/**
	 * 
	 */
	private final static AbstractLinearProgrammingSolver solver = new MixedIntegerLinearProgrammingSolver( TIMEOUT, false );

	/**
	 * 
	 */
	private final Map<Set<Object>,String> ontologyTermAndCompartmentIdToSpeciesId = new HashMap<>();

	/**
	 * 
	 */
	private final List<List<Integer>> matrix = new ArrayList<>();

	/**
	 * 
	 */
	private final List<String> elementIds = new ArrayList<>();

	/**
	 * 
	 */
	private final List<String> moleculeIds = new ArrayList<>();

	/**
	 * 
	 */
	private final List<ChebiTerm> reactantCofactors = new ArrayList<>();

	/**
	 * 
	 */
	private final List<ChebiTerm> productCofactors = new ArrayList<>();

	/**
	 * 
	 */
	private final List<SpeciesReference> allSpeciesReferences = new ArrayList<>();

	/**
	 * 
	 */
	private final List<ChebiTerm> allCofactors = new ArrayList<>();

	/**
	 * 
	 */
	private final List<Boolean> isReactants = new ArrayList<>();

	/**
	 * 
	 */
	private final List<Boolean> isRepeatingUnits = new ArrayList<>();

	/**
	 * 
	 */
	private final List<Double> lowerBounds = new ArrayList<>();

	/**
	 * 
	 */
	private final List<Double> upperBounds = new ArrayList<>();

	/**
	 * 
	 */
	private final Model model;

	/**
	 * 
	 */
	private final Reaction reaction;

	/**
	 * 
	 */
	private final int maxStoichiometricCoefficient;

	/**
	 * 
	 */
	private Set<String> updatedSpeciesIds = new HashSet<>();

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @param cofactors
	 * @param maxStoichiometricCoefficient
	 * @throws Exception
	 */
	public ReactionBalancer( final Model model, final Reaction reaction, final Set<ChebiTerm> cofactors, final int maxStoichiometricCoefficient ) throws Exception
	{
		this.model = model;
		this.reaction = reaction;
		this.maxStoichiometricCoefficient = maxStoichiometricCoefficient;
		reactantCofactors.addAll( cofactors );
		productCofactors.addAll( cofactors );
		fillMatrix();
	}

	/**
	 * 
	 * @return Map<String,Double>
	 */
	public Map<String,Double> isBalanced()
	{
		final double[] stoichiometries = new double[ moleculeIds.size() ];

		final int numReactants = reaction.getNumReactants();

		for( int l = 0; l < numReactants; l++ )
		{
			stoichiometries[ l ] = reaction.getReactant( l ).getStoichiometry();
		}

		for( int l = 0; l < reaction.getNumProducts(); l++ )
		{
			stoichiometries[ l + numReactants ] = reaction.getProduct( l ).getStoichiometry();
		}

		return isBalanced( stoichiometries );
	}

	/**
	 * 
	 * @return Map<String,Double>
	 */
	public Map<String,Double> isBalancedResetStoichiometry()
	{
		final double[] stoichiometries = new double[ moleculeIds.size() ];

		for( int l = 0; l < stoichiometries.length; l++ )
		{
			if( allCofactors.get( l ) == null )
			{
				stoichiometries[ l ] = 1;
			}
		}

		return isBalanced( stoichiometries );
	}

	/**
	 * 
	 * @return boolean
	 * @throws Exception
	 */
	public boolean balanceMinimiseStoichiometries() throws Exception
	{
		// Minimise only the sum of the non-repeating unit components:
		final double[] objectiveCoefficients = new double[ isRepeatingUnits.size() ];

		for( int i = 0; i < isRepeatingUnits.size(); i++ )
		{
			if( !isRepeatingUnits.get( i ).booleanValue() )
			{
				objectiveCoefficients[ i ] = 1;
			}
		}

		final double[][] transposedMatrix = MathUtils.transpose( matrix );
		final double[] stoichiometries = solver.solve( transposedMatrix, moleculeIds.toArray( new String[ moleculeIds.size() ] ), new double[ transposedMatrix.length ], CollectionUtils.toDoubleArray( lowerBounds ), CollectionUtils.toDoubleArray( upperBounds ), objectiveCoefficients );

		if( stoichiometries != null )
		{
			return fixReaction( stoichiometries );
		}

		return false;
	}

	/**
	 * 
	 * @return boolean
	 * @throws Exception
	 */
	public boolean balance() throws Exception
	{
		final String EMPTY_STRING = ""; //$NON-NLS-1$
		final double[][] transposedMatrix = MathUtils.transpose( matrix );
		final int m = transposedMatrix.length;
		final int n = m > 0 ? transposedMatrix[ ZERO.intValue() ].length : ZERO.intValue();
		final double[][] expandedMatrix = new double[ m + n ][];

		// Deep copy:
		for( int i = 0; i < m; i++ )
		{
			expandedMatrix[ i ] = Arrays.copyOf( transposedMatrix[ i ], 3 * n );
		}

		// Add actual, positive and negative coefficients (i.e. x + p - n):
		for( int i = m; i < expandedMatrix.length; i++ )
		{
			final double[] row = new double[ 3 * n ];
			row[ i - m ] = 1;
			row[ i - m + n ] = 1;
			row[ i - m + 2 * n ] = -1;
			expandedMatrix[ i ] = row;
		}

		// Specify reactants / products to be integer, errors to be continuous:
		final int[] columnKinds = new int[ 3 * n ];
		Arrays.fill( columnKinds, GLPKConstants.GLP_IV );

		final double[] objectiveCoefficients = new double[ 3 * n ];

		for( int i = n; i < 3 * n; i++ )
		{
			moleculeIds.add( EMPTY_STRING );
			lowerBounds.add( Double.valueOf( ZERO.intValue() ) );
			upperBounds.add( Double.valueOf( Integer.MAX_VALUE ) );
			objectiveCoefficients[ i ] = isRepeatingUnits.get( i % n ).booleanValue() ? 0 : 1;
			columnKinds[ i ] = GLPKConstants.GLP_CV;
		}

		final double[] rowBounds = new double[ n + m ];

		for( int i = 0; i < n; i++ )
		{
			final SpeciesReference ref = allSpeciesReferences.get( i );
			rowBounds[ i + m ] = ref == null || isRepeatingUnits.get( i ).booleanValue() ? 0 : ref.getStoichiometry();
		}

		final double[] stoichiometries = solver.solve( expandedMatrix, moleculeIds.toArray( new String[ moleculeIds.size() ] ), columnKinds, rowBounds, CollectionUtils.toDoubleArray( lowerBounds ), CollectionUtils.toDoubleArray( upperBounds ), objectiveCoefficients );

		if( stoichiometries != null )
		{
			return fixReaction( stoichiometries );
		}

		return false;
	}

	/**
	 * 
	 * @return updatedSpeciesIds
	 */
	public Set<String> getUpdatedSpeciesIds()
	{
		return updatedSpeciesIds;
	}

	/**
	 * 
	 * @return List<List<Integer>>
	 * @throws Exception
	 */
	private void fillMatrix() throws Exception
	{
		int columns = 0;

		final boolean isTransport = SbmlUtils.isTransport( model, reaction );

		for( int l = 0; l < reaction.getNumReactants(); l++ )
		{
			columns = Math.max( columns, addSpeciesReference( isTransport, reaction.getReactant( l ), true ) );
		}

		for( int l = 0; l < reaction.getNumProducts(); l++ )
		{
			columns = Math.max( columns, addSpeciesReference( isTransport, reaction.getProduct( l ), false ) );
		}

		for( ChebiTerm cofactor : reactantCofactors )
		{
			columns = Math.max( columns, addCofactor( cofactor, true, reactantCofactors ) );
		}

		for( ChebiTerm cofactor : productCofactors )
		{
			columns = Math.max( columns, addCofactor( cofactor, false, productCofactors ) );
		}

		for( List<Integer> row : matrix )
		{
			for( int i = row.size(); i < columns; i++ )
			{
				row.add( ZERO );
			}
		}
	}

	/**
	 * 
	 * @param isTransport
	 * @param speciesReference
	 * @param isReactant
	 * @return int
	 * @throws Exception
	 */
	private int addSpeciesReference( final boolean isTransport, final SpeciesReference speciesReference, final boolean isReactant ) throws Exception
	{
		final String speciesId = speciesReference.getSpecies();
		final Species species = model.getSpecies( speciesId );
		String formula = SbmlUtils.getFormula( model, species );
		int charge = getCharge( model, species );

		if( formula == null )
		{
			throw new NoFormulaException( "MISSING FORMULA: " + species.getId(), species ); //$NON-NLS-1$
		}

		final int LOWER_BOUND_INDEX = 0;
		final int UPPER_BOUND_INDEX = 1;
		final List<ChebiTerm> potentialCofactors = isReactant ? reactantCofactors : productCofactors;
		final double[] bounds = getBounds( speciesReference, isTransport, potentialCofactors );
		return addParticipant( species.getName(), formula, charge, speciesReference, null, isReactant, bounds[ LOWER_BOUND_INDEX ], bounds[ UPPER_BOUND_INDEX ], potentialCofactors );
	}

	/**
	 * 
	 * @param speciesReference
	 * @param isTransport
	 * @param potentialCofactors
	 * @return double[]
	 * @throws Exception
	 */
	private double[] getBounds( final SpeciesReference speciesReference, final boolean isTransport, final Collection<ChebiTerm> potentialCofactors ) throws Exception
	{
		// Initialise with default values for non-cofactor:
		double lowerBound = 1;
		double upperBound = maxStoichiometricCoefficient;

		final Species species = model.getSpecies( speciesReference.getSpecies() );
		final Collection<OntologyTerm> ontologyTerms = SbmlUtils.getOntologyTerms( species ).keySet();

		for( Iterator<ChebiTerm> iterator = potentialCofactors.iterator(); iterator.hasNext(); )
		{
			final OntologyTerm cofactorTerm = iterator.next();

			for( OntologyTerm ontologyTerm : ontologyTerms )
			{
				// Just added a cofactor...
				if( OntologyUtils.getInstance().areEquivalent( ontologyTerm, cofactorTerm, MatchCriteria.ANY ) )
				{
					iterator.remove();

					final boolean protonPump = isTransport && ( cofactorTerm.getId().equals( PROTON ) || cofactorTerm.getId().equals( HYDRON ) );
					lowerBound = protonPump ? (int)speciesReference.getStoichiometry() : 0;
					upperBound = COFACTOR_MAX_STOICHIOMETRIC_COEFFICIENT;
					return new double[] { lowerBound, upperBound };
				}
			}
		}

		// Added a non-cofactor:
		return new double[] { lowerBound, upperBound };
	}

	/**
	 * 
	 * @param cofactor
	 * @param isReactant
	 * @param potentialCofactors
	 * @return int
	 * @throws Exception
	 */
	private int addCofactor( final ChebiTerm cofactor, final boolean isReactant, final List<ChebiTerm> potentialCofactors ) throws Exception
	{
		return addParticipant( cofactor.getName(), cofactor.getFormula(), cofactor.getCharge(), null, cofactor, isReactant, 0, COFACTOR_MAX_STOICHIOMETRIC_COEFFICIENT, potentialCofactors );
	}

	/**
	 * 
	 * @param name
	 * @param formula
	 * @param charge
	 * @param speciesReference
	 * @param cofactor
	 * @param isReactant
	 * @param lowerBound
	 * @param upperBound
	 * @param potentialCofactors
	 * @return int
	 */
	private int addParticipant( final String name, final String formula, final int charge, final SpeciesReference speciesReference, final ChebiTerm cofactor, final boolean isReactant, final double lowerBound, final double upperBound, final List<ChebiTerm> potentialCofactors )
	{
		return addParticipant( name, Formula.getFormula( formula ), charge, speciesReference, cofactor, isReactant, false, lowerBound, upperBound, potentialCofactors );
	}

	/**
	 * 
	 * @param name
	 * @param formula
	 * @param charge
	 * @param speciesReference
	 * @param cofactor
	 * @param isReactant
	 * @param isRepeatingUnit
	 * @param lowerBound
	 * @param upperBound
	 * @param potentialCofactors
	 * @return int
	 */
	private int addParticipant( final String name, final Formula formula, final int charge, final SpeciesReference speciesReference, final ChebiTerm cofactor, final boolean isReactant, final boolean isRepeatingUnit, final double lowerBound, final double upperBound, final List<ChebiTerm> potentialCofactors )
	{
		final Map<String,Integer> elementMap = formula.getElementMap();
		elementMap.put( CHARGE, Integer.valueOf( charge ) );

		final List<Integer> row = new ArrayList<>();
		final int size = matrix.size() == 0 ? 0 : matrix.get( matrix.size() - 1 ).size();

		for( int i = 0; i < size; i++ )
		{
			row.add( ZERO );
		}

		for( Map.Entry<String,Integer> entry : elementMap.entrySet() )
		{
			final Integer elementCount = Integer.valueOf( ( isReactant ? 1 : -1 ) * entry.getValue().intValue() );
			int elementIndex = elementIds.indexOf( entry.getKey() );

			if( elementIndex == -1 )
			{
				elementIds.add( entry.getKey() );
				row.add( elementCount );
			}
			else
			{
				row.set( elementIndex, elementCount );
			}
		}

		matrix.add( row );
		moleculeIds.add( name );
		isReactants.add( Boolean.valueOf( isReactant ) );
		isRepeatingUnits.add( Boolean.valueOf( isRepeatingUnit ) );
		allSpeciesReferences.add( speciesReference );
		allCofactors.add( cofactor );
		lowerBounds.add( Double.valueOf( lowerBound ) );
		upperBounds.add( Double.valueOf( upperBound ) );

		for( Formula repeatingUnit : formula.getRepeatingUnits() )
		{
			addParticipant( repeatingUnit.toString(), repeatingUnit, 0, speciesReference, cofactor, isReactant, true, 0, REPEATING_UNIT_MAX_STOICHIOMETRIC_COEFFICIENT, potentialCofactors );
		}

		return row.size();
	}

	/**
	 * 
	 * @return Map<String,Double>
	 */
	private Map<String,Double> isBalanced( final double[] stoichiometries )
	{
		final Map<String,Double> imbalancedElementIdToCount = new HashMap<>();
		final double[][] transposedMatrix = MathUtils.transpose( matrix );
		final double[] product = MathUtils.multiply( transposedMatrix, stoichiometries );

		for( int i = 0; i < product.length; i++ )
		{
			if( Math.abs( product[ i ] ) > 1e-8 )
			{
				imbalancedElementIdToCount.put( elementIds.get( i ), Double.valueOf( product[ i ] ) );
			}
		}

		return imbalancedElementIdToCount;
	}

	/**
	 * 
	 * @param stoichiometries
	 * @return boolean
	 * @throws Exception
	 */
	private boolean fixReaction( final double[] stoichiometries ) throws Exception
	{
		// Solution found: update the reaction.
		final List<String> reactantsToRemove = new ArrayList<>();
		final List<String> productsToRemove = new ArrayList<>();

		for( int i = 0; i < matrix.size(); i++ )
		{
			final double stoichiometry = stoichiometries[ i ];
			final boolean isReactant = isReactants.get( i ).booleanValue();
			final boolean isRepeatingUnit = isRepeatingUnits.get( i ).booleanValue();
			final SpeciesReference speciesReference = allSpeciesReferences.get( i );
			final ChebiTerm cofactor = allCofactors.get( i );

			if( isRepeatingUnit )
			{
				// Find parent:
				final Species species = model.getSpecies( speciesReference.getSpecies() );

				// Generate updated formula and update species:
				final Formula originalFormula = Formula.getFormula( SbmlUtils.getFormula( model, species ) );
				updatedSpeciesIds.add( species.getId() );

				final int updatedStoichiometry = isInteger( stoichiometry / speciesReference.getStoichiometry() );

				// Generate updated formula and update species:
				updatedSpeciesIds.add( species.getId() );

				if( updatedStoichiometry == Integer.MIN_VALUE )
				{
					return false;
				}

				final Formula updatedFormula = Formula.expand( originalFormula, updatedStoichiometry );
				SbmlUtils.setFormula( species, updatedFormula.toString() );
			}
			else if( stoichiometry == 0 )
			{
				if( speciesReference != null )
				{
					final List<String> remove = isReactant ? reactantsToRemove : productsToRemove;
					remove.add( speciesReference.getSpecies() );
				}
			}
			else if( cofactor != null )
			{
				add( cofactor, ( isReactant ? 1 : -1 ) * stoichiometry );
			}
			else if( speciesReference != null )
			{
				speciesReference.setStoichiometry( stoichiometry );
			}
		}

		for( String reactantToRemove : reactantsToRemove )
		{
			reaction.removeReactant( reactantToRemove );
		}

		for( String productToRemove : productsToRemove )
		{
			reaction.removeProduct( productToRemove );
		}

		return true;
	}

	/**
	 * 
	 * @param value
	 * @return int
	 */
	private static int isInteger( final double value )
	{
		if( ( value == Math.floor( value ) ) && !Double.isInfinite( value ) )
		{
			return (int)Math.floor( value );
		}

		return Integer.MIN_VALUE;
	}

	/**
	 * 
	 * @param ontologyTerm
	 * @param stoichiometry
	 * @throws Exception
	 */
	private void add( final ChebiTerm ontologyTerm, final double stoichiometry ) throws Exception
	{
		final char UNDERSCORE = '_';
		String compartmentId = null;
		String speciesId = null;

		if( stoichiometry != 0 )
		{
			if( stoichiometry > 0 && reaction.getListOfReactants().size() > 0 )
			{
				compartmentId = model.getSpecies( reaction.getListOfReactants().get( FIRST ).getSpecies() ).getCompartment();
			}
			else if( reaction.getListOfProducts().size() > 0 )
			{
				compartmentId = model.getSpecies( reaction.getListOfProducts().get( FIRST ).getSpecies() ).getCompartment();
			}

			final List<Object> ontologyTermAndCompartmentIdList = new ArrayList<>();
			ontologyTermAndCompartmentIdList.add( ontologyTerm );
			ontologyTermAndCompartmentIdList.add( compartmentId );
			final Set<Object> ontologyTermAndCompartmentId = new HashSet<>( ontologyTermAndCompartmentIdList );
			speciesId = ontologyTermAndCompartmentIdToSpeciesId.get( ontologyTermAndCompartmentId );

			if( speciesId == null )
			{
				final String formula = ontologyTerm.getFormula();
				final int charge = ontologyTerm.getCharge();

				for( int l = 0; l < model.getNumSpecies(); l++ )
				{
					final Species species = model.getSpecies( l );

					if( species.isSetSBOTerm() && species.getSBOTerm() == SboUtils.SIMPLE_CHEMICAL && species.getCompartment().equals( compartmentId ) )
					{
						if( SbmlUtils.getOntologyTerms( species ).containsKey( ontologyTerm ) )
						{
							speciesId = species.getId();
							break;
						}
						// else
						// WARNING! Comparing formulae inherently dodgy, BUT we
						// are only interested in H+, H20 and CO2 so should be
						// unique.
						final String currentFormula = SbmlUtils.getFormula( model, species );
						final int currentCharge = SbmlUtils.getCharge( model, species );

						if( currentFormula != null && currentFormula.equals( formula ) && currentCharge == charge )
						{
							speciesId = species.getId();
							break;
						}
					}
				}

				if( speciesId == null )
				{
					final Species species = model.createSpecies();
					species.setId( StringUtils.getUniqueId() + UNDERSCORE + compartmentId );
					species.setName( ontologyTerm.getName() );
					species.setCompartment( compartmentId );
					species.setSBOTerm( SboUtils.SIMPLE_CHEMICAL );
					SbmlUtils.addOntologyTerm( species, ontologyTerm, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS );
					speciesId = species.getId();
				}

				ontologyTermAndCompartmentIdToSpeciesId.put( ontologyTermAndCompartmentId, speciesId );
			}
		}

		add( reaction, speciesId, stoichiometry );
	}

	/**
	 * 
	 * @param model
	 * @param species
	 * @return int
	 * @throws Exception
	 */
	private static int getCharge( final Model model, final Species species ) throws Exception
	{
		int charge = SbmlUtils.getCharge( model, species );

		if( charge == NumberUtils.UNDEFINED )
		{
			charge = DEFAULT_CHARGE;
		}

		return charge;
	}

	/**
	 * 
	 * @param reaction
	 * @param speciesId
	 * @param stoichiometry
	 */
	private static void add( final Reaction reaction, final String speciesId, final double stoichiometry )
	{
		SpeciesReference speciesReference = null;

		if( stoichiometry != 0 )
		{
			if( stoichiometry > 0 )
			{
				for( int l = 0; l < reaction.getNumReactants(); l++ )
				{
					if( reaction.getReactant( l ).getSpecies().equals( speciesId ) )
					{
						speciesReference = reaction.getReactant( l );
						break;
					}
				}

				if( speciesReference == null )
				{
					speciesReference = reaction.createReactant();
				}
			}
			else
			{
				for( int l = 0; l < reaction.getNumProducts(); l++ )
				{
					if( reaction.getProduct( l ).getSpecies().equals( speciesId ) )
					{
						speciesReference = reaction.getProduct( l );
						break;
					}
				}

				if( speciesReference == null )
				{
					speciesReference = reaction.createProduct();
				}
			}

			speciesReference.setSpecies( speciesId );
			speciesReference.setStoichiometry( Math.abs( stoichiometry ) );
		}
	}
}