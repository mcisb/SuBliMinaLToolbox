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
import java.util.Map.Entry;
import java.util.regex.*;
import javax.xml.stream.*;
import javax.xml.transform.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.chebi.*;
import org.mcisb.ontology.kegg.*;
import org.mcisb.ontology.sbo.*;
import org.mcisb.sbml.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.util.*;
import org.mcisb.util.chem.*;
import org.sbml.jsbml.*;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.CVTerm.Type;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlUpdater
{
	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void checkAnnotationValidity( final Model model ) throws Exception
	{
		for( Species species : model.getListOfSpecies() )
		{
			for( CVTerm cvTerm : species.getAnnotation().getListOfCVTerms() )
			{
				for( Iterator<String> iterator = cvTerm.getResources().iterator(); iterator.hasNext(); )
				{
					final String resource = iterator.next();
					final OntologyTerm ontologyTerm = OntologyUtils.getInstance().getOntologyTerm( resource );

					if( ontologyTerm == null || !ontologyTerm.isValid() )
					{
						iterator.remove();
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void checkAnnotationAccuracy( final Model model ) throws Exception
	{
		for( Species species : model.getListOfSpecies() )
		{
			final Map<OntologyTerm,Object[]> ontologyTerms = SbmlUtils.getOntologyTerms( species );
			final ChebiTerm chebiTerm = (ChebiTerm)SbmlUtils.getOntologyTerm( species, Ontology.CHEBI );
			final OntologyTerm inchiTerm = SbmlUtils.getOntologyTerm( species, Ontology.INCHI );
			species.unsetAnnotation();

			if( chebiTerm != null )
			{
				final String chebiTermFormula = chebiTerm.getFormula();
				final int chebiTermCharge = chebiTerm.getCharge() == NumberUtils.UNDEFINED ? 0 : chebiTerm.getCharge();
				final String speciesFormula = SbmlUtils.getFormula( model, species );
				final String originalSpeciesFormula = SbmlUtils.getNonSpecificFormula( species );
				final int speciesCharge = SbmlUtils.getCharge( model, species );

				if( chebiTermFormula == null || speciesFormula == null )
				{
					// Formula null: update.
					addOntologyTerms( species, chebiTerm, inchiTerm, Qualifier.BQB_IS_VERSION_OF );
					System.out.println( "UPDATE ANNOTATION: " + species.getId() + "\t" + chebiTerm.toUri() + "\t" + chebiTermFormula + "\t" + speciesFormula ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				else if( Formula.match( chebiTermFormula, speciesFormula, false ) && chebiTermCharge == speciesCharge )
				{
					// Full match: re-add.
					addOntologyTerms( species, chebiTerm, inchiTerm, Qualifier.BQB_IS );
				}
				else if( Formula.match( Formula.neutralise( chebiTermFormula, chebiTermCharge ), Formula.neutralise( speciesFormula, speciesCharge ), false ) )
				{
					// Neutral charge match: update qualifier.
					addOntologyTerms( species, chebiTerm, inchiTerm, Qualifier.BQB_IS_VERSION_OF );
					System.out.println( "UPDATE ANNOTATION: " + species.getId() + "\t" + chebiTerm.toUri() + "\t" + chebiTermFormula + "\t" + speciesFormula ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				else if( originalSpeciesFormula != null && Formula.match( chebiTermFormula, originalSpeciesFormula, false ) && chebiTermCharge == speciesCharge )
				{
					// Full match to original non-specific formula: update qualifier.
					addOntologyTerms( species, chebiTerm, inchiTerm, Qualifier.BQB_IS_VERSION_OF );
					System.out.println( "UPDATE ANNOTATION: " + species.getId() + "\t" + chebiTerm.toUri() + "\t" + chebiTermFormula + "\t" + speciesFormula ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				else if( originalSpeciesFormula != null && Formula.match( Formula.neutralise( chebiTermFormula, chebiTermCharge ), Formula.neutralise( originalSpeciesFormula, speciesCharge ), false ) )
				{
					// Neutral charge match to original non-specific formula:
					// update qualifier.
					addOntologyTerms( species, chebiTerm, inchiTerm, Qualifier.BQB_IS_VERSION_OF );
					System.out.println( "UPDATE ANNOTATION: " + species.getId() + "\t" + chebiTerm.toUri() + "\t" + chebiTermFormula + "\t" + speciesFormula ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				else if( originalSpeciesFormula != null && Formula.match( Formula.replace( Formula.getFormula( chebiTermFormula ), Formula.R_GROUP, Formula.R_GROUP_EXPANSION ).toString(), originalSpeciesFormula, false ) && chebiTermCharge == speciesCharge )
				{
					// Partial match to original non-specific formula: update
					// qualifier. (R -> CH3(CH2)n case).
					addOntologyTerms( species, chebiTerm, inchiTerm, Qualifier.BQB_IS_VERSION_OF );
					System.out.println( "UPDATE ANNOTATION: " + species.getId() + "\t" + chebiTerm.toUri() + "\t" + chebiTermFormula + "\t" + speciesFormula ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					SbmlUtils.set( species, SbmlUtils.NON_SPECIFIC_FORMULA, chebiTermFormula );
				}
				else
				{
					// No match: update.
					addOntologyTerms( species, chebiTerm, inchiTerm, Qualifier.BQB_IS_VERSION_OF );
					System.out.println( "UPDATE ANNOTATION: " + species.getId() + "\t" + chebiTerm.toUri() + "\t" + chebiTermFormula + "\t" + speciesFormula ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
			}
			else
			{
				SbmlUtils.addOntologyTerms( species, ontologyTerms );
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void checkSpeciesConsistency( final Model model ) throws Exception
	{
		final Map<String,String> speciesIdStemToName = new HashMap<>();
		final Map<String,String> speciesIdStemToNotes = new HashMap<>();
		final Map<String,Map<OntologyTerm,Object[]>> speciesIdStemToAnnotations = new HashMap<>();

		for( Species species : model.getListOfSpecies() )
		{
			final String speciesIdStem = species.getId().substring( 0, species.getId().lastIndexOf( '_' ) );
			final String existingName = speciesIdStemToName.get( speciesIdStem );
			final String existingNotes = speciesIdStemToNotes.get( speciesIdStem );
			final Map<OntologyTerm,Object[]> existingAnnotations = speciesIdStemToAnnotations.get( speciesIdStem );
			final Map<OntologyTerm,Object[]> annotations = SbmlUtils.getOntologyTerms( species );

			if( existingName == null )
			{
				speciesIdStemToName.put( speciesIdStem, species.getName() );
			}
			else if( !existingName.equals( species.getName() ) )
			{
				species.setName( existingName );
			}
			
			if( existingNotes == null )
			{
				speciesIdStemToNotes.put( speciesIdStem, species.getNotesString() );
			}
			else if( !existingNotes.equals( species.getNotesString() ) )
			{
				species.setNotes( existingNotes );
			}

			if( existingAnnotations == null )
			{
				speciesIdStemToAnnotations.put( speciesIdStem, annotations );
			}
			else if( !equals( existingAnnotations, annotations ) )
			{
				species.unsetAnnotation();
				SbmlUtils.addOntologyTerms( species, existingAnnotations );
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void updateCharges( final Model model ) throws Exception
	{
		final int DEFAULT_CHARGE = 0;

		for( Species species : model.getListOfSpecies() )
		{
			try
			{
				if( SbmlUtils.getCharge( model, species ) == NumberUtils.UNDEFINED )
				{
					SbmlUtils.setCharge( species, DEFAULT_CHARGE );
				}
			}
			catch( uk.ac.manchester.libchebi.ChebiException e )
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void updateFormulae( final Model model ) throws Exception
	{
		for( Species species : model.getListOfSpecies() )
		{
			final String formula = SbmlUtils.getFormula( model, species );

			if( formula != null )
			{
				SbmlUtils.setFormula( species, Formula.getFormula( formula ).toString() );
			}
		}
	}

	/**
	 * 
	 * @param sbases
	 * @param sboTerm
	 */
	public static void updateSBOTerms( final List<SBase> sbases, final int sboTerm )
	{
		for( SBase sbase : sbases )
		{
			sbase.setSBOTerm( sboTerm );
		}
	}

	/**
	 * 
	 * @param model
	 */
	public static void unblockReactions( final Model model )
	{
		for( Reaction reaction : model.getListOfReactions() )
		{
			final double lowerBound = FluxBoundsGenerater.getParameterValue( reaction, FluxBoundsGenerater.LOWER_BOUND );
			final double upperBound = FluxBoundsGenerater.getParameterValue( reaction, FluxBoundsGenerater.UPPER_BOUND );

			if( lowerBound == 0 && upperBound == 0 )
			{
				updateReversibility( reaction, reaction.getReversible() );
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @param reversibilities
	 * @throws Exception
	 */
	public static void updateReversibilities( final Model model, final File reversibilities ) throws Exception
	{
		updateReversibilities( model, FileUtils.readMap( reversibilities ) );
	}

	/**
	 * 
	 * @param model
	 * @param reversibilities
	 */
	public static void updateReversibilities( final Model model, final Map<String,String> reversibilities )
	{
		for( Map.Entry<String,String> entry : reversibilities.entrySet() )
		{
			for( Reaction reaction : model.getListOfReactions() )
			{
				if( reaction.getId().matches( entry.getKey() ) )
				{
					for( String key : entry.getValue().toLowerCase().split( "\\|" ) ) //$NON-NLS-1$
					{
						switch( key )
						{
							case "true": //$NON-NLS-1$
							{
								updateReversibility( reaction, true );
								break;
							}
							case "false": //$NON-NLS-1$
							{
								updateReversibility( reaction, false );
								break;
							}
							case "reverse": //$NON-NLS-1$
							{
								reverse( reaction );
								break;
							}
							default:
							{
								// Take no action.
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void updateCharges( final Model model, final File file ) throws TransformerFactoryConfigurationError, Exception
	{
		updateCharges( model, FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param charges
	 * @throws Exception
	 */
	public static void updateCharges( final Model model, final Map<String,String> charges ) throws Exception
	{
		updateTerm( model, "CHARGE", charges ); //$NON-NLS-1$
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws TransformerFactoryConfigurationError
	 * @throws Exception
	 */
	public static void updateInchis( final Model model, final File file ) throws TransformerFactoryConfigurationError, Exception
	{
		updateInchis( model, FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param charges
	 * @throws Exception
	 */
	public static void updateInchis( final Model model, final Map<String,String> charges ) throws Exception
	{
		updateTerm( model, "INCHI", charges ); //$NON-NLS-1$
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws NumberFormatException
	 * @throws SBMLException
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 * @throws TransformerFactoryConfigurationError
	 * @throws IOException
	 */
	public static void updateFormulae( final Model model, final File file ) throws NumberFormatException, SBMLException, FileNotFoundException, XMLStreamException, TransformerFactoryConfigurationError, IOException
	{
		updateFormulae( model, FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param formulae
	 * @throws NumberFormatException
	 * @throws SBMLException
	 * @throws XMLStreamException
	 * @throws TransformerFactoryConfigurationError
	 * @throws IOException
	 */
	public static void updateFormulae( final Model model, final Map<String,String> formulae ) throws NumberFormatException, SBMLException, XMLStreamException, TransformerFactoryConfigurationError, IOException
	{
		updateTerm( model, "FORMULA", formulae ); //$NON-NLS-1$
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws Exception
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateAnnotations( final Model model, final File file ) throws FileNotFoundException, IOException, Exception
	{
		updateAnnotations( model, FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param annotations
	 * @throws Exception
	 */
	public static void updateAnnotations( final Model model, final Map<String,String> idToAnnotations ) throws Exception
	{
		for( Entry<String,String> idToAnnotation : idToAnnotations.entrySet() )
		{
			final String id = idToAnnotation.getKey();
			final String annotation = idToAnnotation.getValue();

			for( Species species : model.getListOfSpecies() )
			{
				if( species.getId().matches( id ) )
				{
					if( annotation.equals( "null" ) ) //$NON-NLS-1$
					{
						species.unsetAnnotation();
					}
					else
					{
						final OntologyTerm ontologyTerm = OntologyUtils.getInstance().getOntologyTerm( annotation );

						if( ontologyTerm != null && ontologyTerm.isValid() )
						{
							if( !SbmlUtils.getOntologyTerms( species ).keySet().contains( ontologyTerm ) )
							{
								species.unsetAnnotation();
								SbmlCreator.addOntologyTerm( species, "BQB_IS", ontologyTerm ); //$NON-NLS-1$
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void updateFromAnnotations( final Model model ) throws Exception
	{
		for( Species species : model.getListOfSpecies() )
		{
			try
			{
				final ChebiTerm chebiTerm = (ChebiTerm)SbmlUtils.getOntologyTerm( species, Ontology.CHEBI );

				if( chebiTerm != null )
				{
					if( chebiTerm.getFormula() != null )
					{
						SbmlUtils.setFormula( species, chebiTerm.getFormula() );
						SbmlUtils.setCharge( species, chebiTerm.getCharge() == NumberUtils.UNDEFINED ? 0 : chebiTerm.getCharge() );
					}

					if( chebiTerm.getSmiles() != null )
					{
						SbmlUtils.setSmiles( species, chebiTerm.getSmiles() );
					}

					if( chebiTerm.getInchi() != null )
					{
						SbmlUtils.set( species, "INCHI", chebiTerm.getInchi() ); //$NON-NLS-1$
					}
				}
				else
				{
					final KeggCompoundTerm keggCompoundTerm = (KeggCompoundTerm)SbmlUtils.getOntologyTerm( species, Ontology.KEGG_COMPOUND );
					final Map<OntologyTerm,Object[]> ontologyTerms = SbmlUtils.getOntologyTerms( species );

					if( keggCompoundTerm != null )
					{
						try
						{
							boolean chebiTermXref = false;

							/*
							 * for( Map.Entry<OntologyTerm,Object[]> xref :
							 * keggCompoundTerm.getXrefs().entrySet() ) { if(
							 * xref.getKey().getOntologyName().equals(
							 * Ontology.CHEBI ) ) { ontologyTerms.clear();
							 * ontologyTerms.put( xref.getKey(), xref.getValue()
							 * ); chebiTermXref = true; } }
							 */

							if( !chebiTermXref )
							{
								if( keggCompoundTerm.getFormula() != null )
								{
									SbmlUtils.setFormula( species, keggCompoundTerm.getFormula() );
									SbmlUtils.setCharge( species, 0 );
								}

								if( keggCompoundTerm.getSmiles() != null )
								{
									SbmlUtils.setSmiles( species, keggCompoundTerm.getSmiles() );
								}
							}
						}
						catch( FileNotFoundException e )
						{
							ontologyTerms.remove( keggCompoundTerm );
						}
						finally
						{
							species.unsetAnnotation();
							SbmlUtils.addOntologyTerms( species, ontologyTerms );
						}
					}
				}
			}
			catch( uk.ac.manchester.libchebi.ChebiException e )
			{
				e.printStackTrace();
				species.unsetAnnotation();
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void updateBoundaryConditions( final Model model ) throws Exception
	{
		for( Species species : model.getListOfSpecies() )
		{
			if( species.getId().endsWith( "_b" ) ) //$NON-NLS-1$
			{
				Species nonBoundary = null;

				for( char c = 'a'; c <= 'z'; c++ )
				{
					nonBoundary = model.getSpecies( species.getId().replaceAll( "_b$", "_" + c ) ); //$NON-NLS-1$ //$NON-NLS-2$

					if( nonBoundary != null )
					{
						break;
					}
				}

				if( nonBoundary != null )
				{
					SbmlUtils.setCharge( species, SbmlUtils.getCharge( model, nonBoundary ) );
				}
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws Exception
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateReactions( final Model model, final File file ) throws FileNotFoundException, IOException, Exception
	{
		updateReactions( model, FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param updates
	 * @throws Exception
	 */
	public static void updateReactions( final Model model, final Map<String,String> updates ) throws Exception
	{
		for( Map.Entry<String,String> entry : updates.entrySet() )
		{
			final Reaction reaction = model.getReaction( entry.getKey() );
			reaction.unsetListOfReactants();
			reaction.unsetListOfProducts();
			SbmlCreator.setReaction( model, reaction, entry.getValue() );
		}
	}

	/**
	 * 
	 * @param model
	 */
	public static void updateReactionSBOTerms( final Model model )
	{
		for( Reaction reaction : model.getListOfReactions() )
		{
			reaction.setSBOTerm( SbmlUtils.isTransport( model, reaction ) ? SboUtils.TRANSPORT_REACTION : SboUtils.BIOCHEMICAL_REACTION );
		}
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	public static void updateGeneAssociations( final Model model, final File file ) throws FileNotFoundException, IOException, XMLStreamException
	{
		updateGeneAssociations( model, FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param geneAssociations
	 * @throws UnsupportedEncodingException
	 * @throws XMLStreamException
	 */
	public static void updateGeneAssociations( final Model model, final Map<String,String> geneAssociations ) throws UnsupportedEncodingException, XMLStreamException
	{
		final String EMPTY_STRING = ""; //$NON-NLS-1$

		for( Reaction reaction : model.getListOfReactions() )
		{
			final String geneAssociation = geneAssociations.get( reaction.getId() );
			SbmlUtils.set( reaction, SbmlUtils.GENE_ASSOCIATION, geneAssociation == null ? EMPTY_STRING : geneAssociation );

			if( !geneAssociations.containsKey( reaction.getId() ) )
			{
				System.out.println( "MISSING " + reaction.getId() ); //$NON-NLS-1$
			}
		}

		for( Entry<String,String> entry : geneAssociations.entrySet() )
		{
			if( model.getReaction( entry.getKey() ) == null )
			{
				System.out.println( "SUPERFLUOUS " + entry.getKey() ); //$NON-NLS-1$
			}
		}
	}

	/**
	 * 
	 * @param reaction
	 */
	public static void reverse( final Reaction reaction )
	{
		final Collection<SpeciesReference> newProducts = new ArrayList<>( reaction.getListOfReactants() );
		final Collection<SpeciesReference> newReactants = new ArrayList<>( reaction.getListOfProducts() );
		reaction.unsetListOfReactants();
		reaction.unsetListOfProducts();

		for( SpeciesReference ref : newReactants )
		{
			reaction.addReactant( ref.clone() );
		}

		for( SpeciesReference ref : newProducts )
		{
			reaction.addProduct( ref.clone() );
		}

		final LocalParameter lowerBound = FluxBoundsGenerater.getLocalParameter( reaction, FluxBoundsGenerater.LOWER_BOUND );
		final LocalParameter upperBound = FluxBoundsGenerater.getLocalParameter( reaction, FluxBoundsGenerater.UPPER_BOUND );
		final double newLowerBound = upperBound.getValue() == 0.0 ? 0.0 : -1 * upperBound.getValue();
				
		upperBound.setValue( lowerBound.getValue() == 0.0 ? 0.0 : -1 * lowerBound.getValue() );
		lowerBound.setValue( newLowerBound );

		reaction.setReversible( !reaction.isSetReversible() || reaction.isReversible() );
	}
	
	/**
	 * 
	 * @param model
	 * @param file
	 * @throws Exception 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateSpeciesIds( final Model model ) throws Exception
	{
		final Map<String,Set<String>> annotationToStems = new HashMap<>();
		
		for( Species species : model.getListOfSpecies() )
		{
			final String stem = species.getId().substring( 0, species.getId().lastIndexOf( '_' ) + 1 );
			
			for( Entry<OntologyTerm,Object[]> entry : SbmlUtils.getOntologyTerms( species ).entrySet() )
			{
				final String key = entry.getKey().toUri() + "\t" + Arrays.toString( entry.getValue() ) + "\t" + SbmlUtils.getFormula( model, species ) + "\t" + SbmlUtils.getCharge( model, species ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				Set<String> stems = annotationToStems.get( key );
				
				if( stems == null )
				{
					stems = new HashSet<>();
					annotationToStems.put( key, stems );
				}
				
				stems.add( stem );
			}
		}

		final Map<String,String> speciesIds = new HashMap<>();
		
		for( Entry<String,Set<String>> entry : annotationToStems.entrySet() )
		{
			if( entry.getValue().size() > 1 )
			{
				final String[] ids = entry.getValue().toArray( new String[ entry.getValue().size() ] );
				final String preferredId = SbmlDeleter.getPreferredId( ids );
				
				for( String id : ids )
				{
					speciesIds.put( id + "(?=[a-z])", preferredId ); //$NON-NLS-1$
				}
			}
		}
		
		updateSpeciesIds( model, speciesIds );
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateSpeciesIds( final Model model, final File file ) throws FileNotFoundException, IOException
	{
		updateSpeciesIds( model, FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param speciesIds
	 */
	public static void updateSpeciesIds( final Model model, final Map<String,String> speciesIds )
	{
		updateIds( model.getListOfSpecies(), speciesIds, true );

		for( Map.Entry<String,String> entry : speciesIds.entrySet() )
		{
			final String regExpId = entry.getKey();
			final Pattern pattern = Pattern.compile( regExpId );
			final String replacement = entry.getValue();

			for( Reaction reaction : model.getListOfReactions() )
			{
				for( SpeciesReference ref : reaction.getListOfReactants() )
				{
					final String species = ref.getSpecies();
					ref.setSpecies( pattern.matcher( species ).replaceAll( replacement ) );
				}

				for( SpeciesReference ref : reaction.getListOfProducts() )
				{
					final String species = ref.getSpecies();
					ref.setSpecies( pattern.matcher( species ).replaceAll( replacement ) );
				}
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void updateReactionIds( final Model model, final File file ) throws FileNotFoundException, IOException
	{
		updateIds( model.getListOfReactions(), FileUtils.readMap( file ), false );
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateSpeciesNames( final Model model, final File file ) throws FileNotFoundException, IOException
	{
		updateNames( model.getListOfSpecies(), FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param model
	 * @param file
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateReactionNames( final Model model, final File file ) throws FileNotFoundException, IOException
	{
		updateNames( model.getListOfReactions(), FileUtils.readMap( file ) );
	}

	/**
	 * 
	 * @param list
	 * @param ids
	 * @param regex
	 */
	private static void updateIds( final ListOf<? extends NamedSBase> list, final Map<String,String> ids, final boolean regex )
	{
		for( Map.Entry<String,String> entry : ids.entrySet() )
		{
			final String id = entry.getKey();
			
			if( regex )
			{
				final Pattern pattern = Pattern.compile( id );
	
				for( NamedSBase sbase : list )
				{
					sbase.setId( pattern.matcher( sbase.getId() ).replaceAll( entry.getValue() ) );
				}
			}
			else
			{
				for( NamedSBase sbase : list )
				{
					if( sbase.getId().equals( id ) )
					{
						sbase.setId( entry.getValue() );
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param list
	 * @param speciesIds
	 */
	private static void updateNames( final ListOf<? extends NamedSBase> list, final Map<String,String> names )
	{
		for( Map.Entry<String,String> entry : names.entrySet() )
		{
			final String regExpNames = entry.getKey();
			final Pattern pattern = Pattern.compile( regExpNames );

			for( NamedSBase sbase : list )
			{
				if( pattern.matcher( sbase.getId() ).matches() )
				{
					sbase.setName( entry.getValue() );
				}
			}
		}
	}

	/**
	 * 
	 * @param species
	 * @param chebiTerm
	 * @param inchiTerm
	 * @param qualifier
	 * @throws Exception 
	 */
	private static void addOntologyTerms( final Species species, final OntologyTerm chebiTerm, final OntologyTerm inchiTerm, final Qualifier qualifier ) throws Exception
	{
		final Object[] predicates = new Object[] { Type.BIOLOGICAL_QUALIFIER, qualifier };
		final Map<OntologyTerm,Object[]> ontologyTerms = new TreeMap<>();
		ontologyTerms.put( chebiTerm, predicates );

		if( inchiTerm != null )
		{
			ontologyTerms.put( inchiTerm, predicates );
		}

		species.setName( chebiTerm.getName() );
		SbmlUtils.addOntologyTerms( species, ontologyTerms );
	}

	/**
	 * 
	 * @param model
	 * @param term
	 * @param values
	 * @throws NumberFormatException
	 * @throws XMLStreamException
	 * @throws SBMLException
	 * @throws TransformerFactoryConfigurationError
	 * @throws IOException
	 */
	private static void updateTerm( final Model model, final String term, final Map<String,String> values ) throws NumberFormatException, XMLStreamException, SBMLException, TransformerFactoryConfigurationError, IOException
	{
		for( Map.Entry<String,String> entry : values.entrySet() )
		{
			for( Species species : model.getListOfSpecies() )
			{
				if( species.getId().matches( entry.getKey() ) )
				{
					if( SbmlUtils.getNotes( species ).get( term ) != null && SbmlUtils.getNotes( species ).get( term ).equals( entry.getValue() ) )
					{
						System.out.println( species.getId() + " " + term + " doesn't need updating" ); //$NON-NLS-1$ //$NON-NLS-2$
					}
					else if( term.equals( SbmlUtils.CHARGE ) )
					{
						SbmlUtils.setCharge( species, Integer.parseInt( entry.getValue() ) );
					}
					else
					{
						SbmlUtils.set( species, term, entry.getValue() );
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param reaction
	 * @param reversible
	 */
	public static void updateReversibility( final Reaction reaction, final boolean reversible )
	{
		final String LOWER_BOUND = "LOWER_BOUND"; //$NON-NLS-1$
		final String UPPER_BOUND = "UPPER_BOUND"; //$NON-NLS-1$
		final double ZERO = 0;

		reaction.setReversible( reversible );

		final KineticLaw kineticLaw = reaction.getKineticLaw() == null ? reaction.createKineticLaw() : reaction.getKineticLaw();
		final LocalParameter lowerBound = kineticLaw.getLocalParameter( LOWER_BOUND ) == null ? kineticLaw.createLocalParameter( LOWER_BOUND ) : kineticLaw.getLocalParameter( LOWER_BOUND );
		final LocalParameter upperBound = kineticLaw.getLocalParameter( UPPER_BOUND ) == null ? kineticLaw.createLocalParameter( UPPER_BOUND ) : kineticLaw.getLocalParameter( UPPER_BOUND );
		lowerBound.setValue( reversible ? Double.NEGATIVE_INFINITY : ZERO );
		upperBound.setValue( Double.POSITIVE_INFINITY );
	}

	/**
	 * 
	 * @param annotations1
	 * @param annotations2
	 * @return boolean
	 */
	private static boolean equals( final Map<OntologyTerm,Object[]> annotations1, final Map<OntologyTerm,Object[]> annotations2 )
	{
		if( annotations1.size() != annotations2.size() )
		{
			return false;
		}

		for( Map.Entry<OntologyTerm,Object[]> entry : annotations1.entrySet() )
		{
			final Object[] value2 = annotations2.get( entry.getKey() );

			if( value2 == null || !Arrays.equals( value2, entry.getValue() ) )
			{
				return false;
			}
		}

		return true;
	}
}