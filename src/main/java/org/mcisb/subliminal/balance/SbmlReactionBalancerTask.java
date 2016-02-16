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

import java.io.*;
import java.util.*;

import org.mcisb.ontology.sbo.*;
import org.mcisb.sbml.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.ui.util.*;
import org.mcisb.util.chem.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlReactionBalancerTask
{
	/**
	 * 
	 */
	private final SBMLDocument document;

	/**
	 * 
	 */
	private final File outFile;

	/**
	 * 
	 */
	private final SbmlReactionBalancer balancer = new SbmlReactionBalancer();

	/**
	 * 
	 */
	private final Map<String,Integer> problematicMetaboliteDegree = new LinkedHashMap<>();

	/**
	 * 
	 */
	private final Collection<String> balancedReactionIds = new LinkedHashSet<>();

	/**
	 * 
	 */
	private final Map<String,Map<String,Double>[]> unbalancedReactions = new LinkedHashMap<>();

	/**
	 * 
	 */
	private boolean updatedSpecies = false;

	/**
	 * 
	 */
	private int maxStoichiometricCoefficient;

	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @param maxStoichiometricCoefficient
	 * @throws Exception
	 */
	public SbmlReactionBalancerTask( final File inFile, final File outFile, final int maxStoichiometricCoefficient ) throws Exception
	{
		this( SBMLReader.read( inFile ), outFile, maxStoichiometricCoefficient );
	}
	
	/**
	 * 
	 * @param is
	 * @param outFile
	 * @param maxStoichiometricCoefficient
	 * @throws Exception
	 */
	public SbmlReactionBalancerTask( final InputStream is, final File outFile, final int maxStoichiometricCoefficient ) throws Exception
	{
		this( SBMLReader.read( is ), outFile, maxStoichiometricCoefficient );
	}

	/**
	 * 
	 * @param document
	 * @param outFile
	 * @param maxStoichiometricCoefficient
	 * @throws Exception
	 */
	public SbmlReactionBalancerTask( final SBMLDocument document, final File outFile, final int maxStoichiometricCoefficient ) throws Exception
	{
		this.document = document;
		this.outFile = outFile;
		this.maxStoichiometricCoefficient = maxStoichiometricCoefficient;
	}

	/**
	 * @return the balancedReactionIds
	 */
	public Collection<String> getBalancedReactionIds()
	{
		return balancedReactionIds;
	}

	/**
	 * @return the unbalancedReactions
	 */
	public Map<String,Map<String,Double>[]> getUnbalancedReactions()
	{
		return unbalancedReactions;
	}

	/**
	 * 
	 * @return Map<String,Integer>
	 */
	public Map<String,Integer> getProblematicMetaboliteDegree()
	{
		return problematicMetaboliteDegree;
	}

	/**
	 * 
	 * @return SBMLDocument
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public SBMLDocument run() throws Exception
	{
		final Model model = document.getModel();

		for( int l = 0; l < model.getNumReactions(); l++ )
		{
			final Reaction reaction = model.getReaction( l );

			try
			{
				final boolean omittedProcess = reaction.getSBOTerm() == SboUtils.OMITTED_PROCESS;

				if( !omittedProcess )
				{
					final Map<String,Formula> speciesIdToOriginalFormula = expandRgroups( model, reaction );
					final Map<String,Double> imbalancedElementIdToCount = balancer.isBalanced( model, reaction, maxStoichiometricCoefficient );

					if( imbalancedElementIdToCount.size() > 0 )
					{
						final Object[] balanceReturnValue = balancer.balance( model, reaction, maxStoichiometricCoefficient );

						if( ( (Boolean)balanceReturnValue[ SbmlReactionBalancer.IS_BALANCED ] ).booleanValue() )
						{
							balancedReactionIds.add( reaction.getId() );

							for( String speciesId : (Collection<String>)balanceReturnValue[ SbmlReactionBalancer.UPDATED_SPECIES ] )
							{
								SbmlUtils.set( model.getSpecies( speciesId ), SbmlUtils.NON_SPECIFIC_FORMULA, speciesIdToOriginalFormula.get( speciesId ) );
								// System.out.println( reaction.getId() + "\t" + speciesId + "\t" + SbmlUtils.getFormula( model, model.getSpecies( speciesId ) ) + "\t" + speciesIdToOriginalFormula.get( speciesId ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								updatedSpecies = true;
							}
						}
						else
						{
							final Map<String,Double> imbalancedElementIdToCountResetStoichiometry = balancer.isBalancedResetStoichiometry( model, reaction, maxStoichiometricCoefficient );

							if( imbalancedElementIdToCountResetStoichiometry.size() > 0 )
							{
								final Object[] balanceReturnValueResetStoichiometry = balancer.balance( model, reaction, maxStoichiometricCoefficient );

								if( ( (Boolean)balanceReturnValue[ SbmlReactionBalancer.IS_BALANCED ] ).booleanValue() )
								{
									balancedReactionIds.add( reaction.getId() );

									for( String speciesId : (Collection<String>)balanceReturnValueResetStoichiometry[ SbmlReactionBalancer.UPDATED_SPECIES ] )
									{
										SbmlUtils.set( model.getSpecies( speciesId ), SbmlUtils.NON_SPECIFIC_FORMULA, speciesIdToOriginalFormula.get( speciesId ) );
										// System.out.println( reaction.getId() + "\t" + speciesId + "\t" + SbmlUtils.getFormula( model, model.getSpecies( speciesId ) ) + "\t" + speciesIdToOriginalFormula.get( speciesId ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										updatedSpecies = true;
									}
								}
								else
								{
									// Revert formula mapping from R-group if
									// unsuccessfully balanced:
									for( Map.Entry<String,Formula> entry : speciesIdToOriginalFormula.entrySet() )
									{
										SbmlUtils.setFormula( model.getSpecies( entry.getKey() ), entry.getValue().toString() );
									}

									unbalancedReactions.put( reaction.getId(), new Map[] { imbalancedElementIdToCount, imbalancedElementIdToCountResetStoichiometry } );
									countProblematicMetabolites( model, reaction );
								}
							}
						}
					}
				}
			}
			catch( NoFormulaException e )
			{
				unbalancedReactions.put( reaction.getId(), null );
				addProblematicMetabolite( e.getSpecies() );
			}
		}

		if( outFile != null )
		{
			XmlFormatter.getInstance().write( document, outFile );
		}

		return document;
	}

	/**
	 * @throws Exception
	 */
	public void printResult() throws Exception
	{
		final int MODEL_STOICHIOMETRIES = 0;
		final int RESET_STOICHIOMETRIES = 1;
		final String TAB = "\t"; //$NON-NLS-1$
		final Model model = document.getModel();

		/*
		 * for( String reactionId : balancedReactionIds ) { System.out.println(
		 * "BALANCED" + TAB + TAB + TAB + getDetail( model, reactionId ) );
		 * //$NON-NLS-1$ }
		 */

		for( Map.Entry<String,Map<String,Double>[]> entry : unbalancedReactions.entrySet() )
		{
			final Map<String,Double>[] imbalancedElementIdToCounts = entry.getValue();
			System.out.println( "UNBALANCED" + TAB + ( imbalancedElementIdToCounts == null ? TAB : imbalancedElementIdToCounts[ MODEL_STOICHIOMETRIES ] ) + TAB + ( imbalancedElementIdToCounts == null ? TAB : imbalancedElementIdToCounts[ RESET_STOICHIOMETRIES ] ) + TAB + getDetail( model, entry.getKey() ) ); //$NON-NLS-1$
		}

		final float numReactions = model.getNumReactions();
		final float numUnbalanced = unbalancedReactions.size();

		System.out.println( ( numReactions - numUnbalanced ) + "/" + numReactions + " " + ( ( numReactions - numUnbalanced ) / numReactions * 100 ) + "% balanced" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		System.out.println();

		final Map<String,Integer> metaboliteDegree = SbmlUtils.getMetaboliteDegree( model, true );

		for( Map.Entry<String,Integer> entry : problematicMetaboliteDegree.entrySet() )
		{
			final String speciesId = entry.getKey();
			final Species species = model.getSpecies( speciesId );
			final int problematicReactions = entry.getValue().intValue();
			final int allReactions = metaboliteDegree.get( speciesId ).intValue();

			System.out.println( speciesId + TAB + species.getName() + TAB + SbmlUtils.getFormula( model, species ) + TAB + SbmlUtils.getNotes( species ).get( "NON_SPECIFIC_FORMULA" ) + TAB + problematicReactions + TAB + ( (float)problematicReactions / allReactions * 100 ) + "%" ); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * 
	 * @return updatedSpecies
	 */
	public boolean hasUpdatedSpecies()
	{
		return updatedSpecies;
	}

	/**
	 * @param reaction
	 */
	private void countProblematicMetabolites( final Model model, final Reaction reaction )
	{
		for( int l = 0; l < reaction.getNumReactants(); l++ )
		{
			addProblematicMetabolite( model.getSpecies( reaction.getReactant( l ).getSpecies() ) );
		}

		for( int l = 0; l < reaction.getNumProducts(); l++ )
		{
			addProblematicMetabolite( model.getSpecies( reaction.getProduct( l ).getSpecies() ) );
		}
	}

	/**
	 * 
	 * @param model
	 * @param species
	 * @throws Exception
	 */
	private void addProblematicMetabolite( final Species species )
	{
		Integer count = problematicMetaboliteDegree.get( species.getId() );

		if( count == null )
		{
			problematicMetaboliteDegree.put( species.getId(), Integer.valueOf( 1 ) );
		}
		else
		{
			problematicMetaboliteDegree.put( species.getId(), Integer.valueOf( count.intValue() + 1 ) );
		}
	}

	/**
	 * 
	 * @param model
	 * @param reactionId
	 * @return String
	 * @throws Exception
	 */
	private static String getDetail( final Model model, final String reactionId ) throws Exception
	{
		final String TAB = "\t"; //$NON-NLS-1$
		final StringBuffer buffer = new StringBuffer();
		buffer.append( reactionId );
		buffer.append( TAB );
		buffer.append( model.getReaction( reactionId ).getName() );
		buffer.append( TAB );
		buffer.append( SbmlUtils.toString( model, reactionId, false ) );
		buffer.append( TAB );
		buffer.append( SbmlUtils.toIdString( model, reactionId ) );
		buffer.append( TAB );
		buffer.append( SbmlUtils.toFormulaString( model, reactionId ) );
		return buffer.toString();
	}

	/**
	 * 
	 * @param model
	 * @param reaction
	 * @return Map<String,Formula>
	 * @throws Exception
	 */
	private static Map<String,Formula> expandRgroups( final Model model, final Reaction reaction ) throws Exception
	{
		final int ONE = 1;
		final int TWO = 2;

		final Map<String,Formula> speciesIdToOriginalFormula = new HashMap<>();

		final Map<Species,Integer> rGroupSpecies = new HashMap<>();
		int reactantRgroups = 0;
		int productRgroups = 0;

		for( SpeciesReference ref : reaction.getListOfReactants() )
		{
			final Species species = model.getSpecies( ref.getSpecies() );
			final Formula formula = Formula.getFormula( SbmlUtils.getFormula( model, species ) );

			if( formula == null )
			{
				return speciesIdToOriginalFormula;
			}

			final int rGroupCount = formula.get( Formula.R_GROUP );

			if( rGroupCount > 0 )
			{
				rGroupSpecies.put( species, Integer.valueOf( rGroupCount ) );
				reactantRgroups += rGroupCount;
			}

			speciesIdToOriginalFormula.put( species.getId(), formula );
		}

		for( SpeciesReference ref : reaction.getListOfProducts() )
		{
			final Species species = model.getSpecies( ref.getSpecies() );
			final Formula formula = Formula.getFormula( SbmlUtils.getFormula( model, species ) );

			if( formula == null )
			{
				return speciesIdToOriginalFormula;
			}

			final int rGroupCount = formula.get( Formula.R_GROUP );

			if( rGroupCount > 0 )
			{
				rGroupSpecies.put( species, Integer.valueOf( rGroupCount ) );
				productRgroups += rGroupCount;
			}

			speciesIdToOriginalFormula.put( species.getId(), formula );
		}

		if( rGroupSpecies.size() == ONE || rGroupSpecies.size() == TWO && Math.abs( reactantRgroups - productRgroups ) == ONE )
		{
			// Attempt to leave a single unknown R group.
			// So... A + XR -> B + C updated to A + X(CH3)(CH2)n -> B + C
			// Or... A + XR2 -> B + CR updated to A + XR(CH3)(CH2)n -> B + CR
			for( Map.Entry<Species,Integer> entry : rGroupSpecies.entrySet() )
			{
				final Species species = entry.getKey();
				final Formula formula = Formula.getFormula( speciesIdToOriginalFormula.get( species.getId() ).toString() );
				Formula.replace( formula, Formula.R_GROUP, Formula.R_GROUP_EXPANSION, entry.getValue().intValue() - Math.min( reactantRgroups, productRgroups ) );
				SbmlUtils.setFormula( species, formula.toString() );
			}
		}

		return speciesIdToOriginalFormula;
	}
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		final int DEFAULT_MAX_STOICHIOMETRIC_COEFFICIENT = 8;
		final File sourceFile = new File( args[ 0 ] );
		final File targetFile = new File( args[ 1 ] );
		final int maxStoichiometricCoefficient = args.length > 2 ? Integer.parseInt( args[ 2 ] ) : DEFAULT_MAX_STOICHIOMETRIC_COEFFICIENT;

		if( sourceFile.isDirectory() && ( !targetFile.exists() || targetFile.isDirectory() ) )
		{
			for( final File sourceSbmlFile : sourceFile.listFiles( new CustomFileFilter( "xml" ) ) ) //$NON-NLS-1$
			{
				final File targetSbmlFile = new File( targetFile, sourceSbmlFile.getName() );
				final SbmlReactionBalancerTask task = new SbmlReactionBalancerTask( sourceSbmlFile, targetSbmlFile, maxStoichiometricCoefficient );
				task.run();
				task.printResult();
			}
		}
		else
		{
			final SbmlReactionBalancerTask task = new SbmlReactionBalancerTask( sourceFile, targetFile, maxStoichiometricCoefficient );
			task.run();
			task.printResult();
		}
	}
}