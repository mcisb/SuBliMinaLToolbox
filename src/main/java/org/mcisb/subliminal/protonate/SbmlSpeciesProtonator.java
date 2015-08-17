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
package org.mcisb.subliminal.protonate;

import java.io.*;
import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.chebi.*;
import org.mcisb.ontology.sbo.*;
import org.mcisb.sbml.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.util.*;
import org.mcisb.util.chem.*;
import org.sbml.jsbml.*;

/**
 * @author Neil Swainston
 */
public class SbmlSpeciesProtonator
{
	/**
	 * 
	 */
	private static final String DEFAULT = "DEFAULT"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String R = "R"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String R_GROUP_REGEX = "(?=.*)\\[\\*\\](?=.*)"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final Map<String,Float> goTermTopH = new HashMap<>();

	/**
	 * 
	 */
	private final Map<Float,Map<String,Object[]>> pHtoSmilesToProtonatedFormulaAndCharge = new TreeMap<>();

	/**
	 * @param pH
	 */
	public SbmlSpeciesProtonator( final float pH )
	{
		setpH( DEFAULT, pH );
	}

	/**
	 * 
	 * @param goTermTopH
	 */
	public SbmlSpeciesProtonator( final Map<String,Float> goTermTopH )
	{
		for( Map.Entry<String,Float> entry : goTermTopH.entrySet() )
		{
			setpH( entry.getKey(), entry.getValue().floatValue() );
		}
	}

	/**
	 * 
	 * @param goTermId
	 * @param pH
	 */
	private void setpH( final String goTermId, final float pH )
	{
		final float MIN_PH = 0.0f;
		final float MAX_PH = 14.0f;

		if( pH < MIN_PH || pH > MAX_PH )
		{
			throw new IllegalArgumentException( "pH " + pH ); //$NON-NLS-1$
		}

		goTermTopH.put( goTermId, Float.valueOf( pH ) );
	}

	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @throws Exception
	 */
	public void run( final File inFile, final File outFile ) throws Exception
	{
		final int FORMULA = 0;
		final int CHARGE = 1;

		final SBMLDocument document = SBMLReader.read( inFile );
		final Model model = document.getModel();

		for( int l = 0; l < model.getNumSpecies(); l++ )
		{
			final Species species = model.getSpecies( l );

			if( species.getSBOTerm() == SboUtils.SIMPLE_CHEMICAL )
			{
				try
				{
					final OntologyTerm goTerm = SbmlUtils.getOntologyTerm( model.getCompartment( species.getCompartment() ), Ontology.GO );
					float pH = NumberUtils.UNDEFINED;

					if( goTerm != null )
					{
						Float potentialpH = goTermTopH.get( goTerm.getId().replace( OntologyTerm.ENCODED_COLON, OntologyTerm.COLON ) );

						if( potentialpH != null )
						{
							pH = potentialpH.floatValue();
						}
					}

					if( pH == NumberUtils.UNDEFINED )
					{
						pH = goTermTopH.get( DEFAULT ).floatValue();
					}

					final String smiles = SbmlUtils.getSmiles( model, species );

					if( smiles != null && !smiles.equals( "[H+]" ) && !smiles.equals( "[1H+]" ) ) //$NON-NLS-1$ //$NON-NLS-2$
					{
						final Object[] protonatedFormulaAndCharge = getProtonatedFormulaAndCharge( pH, smiles );
						final int protonatedCharge = ( (Integer)protonatedFormulaAndCharge[ CHARGE ] ).intValue();
						final String protonatedFormula = (String)protonatedFormulaAndCharge[ FORMULA ];

						SbmlUtils.setFormula( species, protonatedFormula );
						SbmlUtils.setCharge( species, protonatedCharge );

						final ChebiTerm chebiTerm = (ChebiTerm)CollectionUtils.getFirst( OntologyUtils.getInstance().getXrefs( SbmlUtils.getOntologyTerms( species ).keySet(), Ontology.CHEBI ) );

						if( chebiTerm != null )
						{
							if( protonatedCharge < chebiTerm.getCharge() )
							{
								checkAnnotation( true, chebiTerm, protonatedCharge, protonatedFormula, species );
							}
							else if( protonatedCharge > chebiTerm.getCharge() )
							{
								checkAnnotation( false, chebiTerm, protonatedCharge, protonatedFormula, species );
							}
						}
					}
				}
				catch( FileNotFoundException e )
				{
					// Invalid KEGG terms...
					// e.printStackTrace();
				}
				catch( Exception e )
				{
					e.printStackTrace();
				}
			}
		}

		XmlFormatter.getInstance().write( document, outFile );
	}

	/**
	 * 
	 * @param pH
	 * @param smiles
	 * @return Object[]
	 * @throws Exception
	 */
	private Object[] getProtonatedFormulaAndCharge( final float pH, final String smiles ) throws Exception
	{
		final MajorMicrospeciesPlugin plugin = new MajorMicrospeciesPlugin();
		final Map<String,Object[]> smilesToProtonatedFormulaAndCharge = pHtoSmilesToProtonatedFormulaAndCharge.get( Float.valueOf( pH ) );

		if( smilesToProtonatedFormulaAndCharge == null )
		{
			smilesToProtonatedFormulaAndCharge = new TreeMap<>();
			pHtoSmilesToProtonatedFormulaAndCharge.put( Float.valueOf( pH ), smilesToProtonatedFormulaAndCharge );
		}

		Object[] protonatedFormulaAndCharge = smilesToProtonatedFormulaAndCharge.get( smiles );

		if( protonatedFormulaAndCharge == null )
		{
			plugin.setpH( pH );
			plugin.setMolecule( MolImporter.importMol( smiles ) );
			plugin.run();

			String protonatedFormula = plugin.getMajorMicrospecies().getFormula();
			final int rGroups = RegularExpressionUtils.getAllMatches( smiles, R_GROUP_REGEX ).size();
			protonatedFormula = protonatedFormula + ( ( rGroups == 0 ) ? EMPTY_STRING : R + rGroups );
			protonatedFormula = Formula.getFormula( protonatedFormula ).toString();

			protonatedFormulaAndCharge = new Object[] { protonatedFormula, Integer.valueOf( plugin.getMajorMicrospecies().getFormalCharge() ) };
			smilesToProtonatedFormulaAndCharge.put( smiles, protonatedFormulaAndCharge );
		}

		return protonatedFormulaAndCharge;
	}

	/**
	 * 
	 * @param isBase
	 * @param chebiTerm
	 * @param protonatedCharge
	 * @param protonatedFormula
	 * @param species
	 * @throws Exception
	 */
	private static void checkAnnotation( final boolean isBase, final ChebiTerm chebiTerm, final int protonatedCharge, final String protonatedFormula, final Species species ) throws Exception
	{
		for( Map.Entry<ChebiTerm,String> childEntry : chebiTerm.getChildren().entrySet() )
		{
			final ChebiTerm key = childEntry.getKey();

			// Update annotation with new ChEBI term if appropriate:
			if( childEntry.getValue().equals( "is conjugate " + ( isBase ? "base" : "acid" ) + " of" ) ) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			{
				if( key.getCharge() == protonatedCharge && protonatedFormula.equals( key.getFormula() ) )
				{
					final Map<OntologyTerm,Object[]> ontologyTerms = SbmlUtils.getOntologyTerms( species );
					species.unsetAnnotation();

					for( Iterator<Map.Entry<OntologyTerm,Object[]>> iterator = ontologyTerms.entrySet().iterator(); iterator.hasNext(); )
					{
						Map.Entry<OntologyTerm,Object[]> entry = iterator.next();

						if( entry.getValue()[ 0 ] == CVTerm.Type.BIOLOGICAL_QUALIFIER && entry.getValue()[ 1 ] == CVTerm.Qualifier.BQB_IS )
						{
							iterator.remove();
						}
					}

					ontologyTerms.put( key, new Object[] { CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS } );
					SbmlUtils.addOntologyTerms( species, ontologyTerms );

					System.out.println( "Replacing " + species.getName() + " with " + key.getName() ); //$NON-NLS-1$ //$NON-NLS-2$

					species.setName( key.getName() );
					SbmlUtils.setSmiles( species, key.getSmiles() );
				}
				else
				{
					checkAnnotation( isBase, key, protonatedCharge, protonatedFormula, species );
				}
			}
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public static void main( String[] args ) throws NumberFormatException, IOException, Exception
	{
		if( NumberUtils.isDecimal( args[ 2 ] ) )
		{
			new SbmlSpeciesProtonator( Float.parseFloat( args[ 2 ] ) ).run( new File( args[ 0 ] ), new File( args[ 1 ] ) );
		}
		else
		{
			final Map<String,Float> goTermTopH = new HashMap<>();
			final String[] terms = args[ 2 ].split( ";" ); //$NON-NLS-1$

			for( String term : terms )
			{
				String pH = null;
				String goTermId = null;

				if( term.contains( "," ) ) //$NON-NLS-1$
				{
					final String[] subterms = term.split( "," ); //$NON-NLS-1$
					goTermId = subterms[ 0 ];
					pH = subterms[ 1 ];
				}
				else
				{
					goTermId = DEFAULT;
					pH = term.trim();
				}

				goTermTopH.put( goTermId, Float.valueOf( Float.parseFloat( pH ) ) );
			}

			new SbmlSpeciesProtonator( goTermTopH ).run( new File( args[ 0 ] ), new File( args[ 1 ] ) );
		}
	}
}