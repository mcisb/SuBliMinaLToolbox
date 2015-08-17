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
package org.mcisb.subliminal.merge;

import java.io.*;
import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.OntologyUtils.MatchCriteria;
import org.mcisb.sbml.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.util.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlMerger
{
	/**
	 * 
	 */
	protected static final FileFilter fileFilter = new FileFilter()
	{
		@Override
		public boolean accept( final File file )
		{
			return file.isFile() && file.getName().endsWith( ".xml" ); //$NON-NLS-1$
		}
	};

	/**
	 * 
	 */
	protected final Collection<MatchCriteria> allMatchCriteria;

	/**
	 * 
	 */
	protected final boolean checkForReversibility;

	/**
	 * 
	 */
	protected boolean unsetNotes = false;

	/**
	 * 
	 */
	protected boolean mergeSmiles = false;

	/**
	 * 
	 */
	private final SBMLDocument[] inDocuments;

	/**
	 * 
	 */
	private final File outFile;

	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @param matchCriteria
	 * @param checkForReversibility
	 * @throws Exception
	 */
	public SbmlMerger( final File inFile, final File outFile, final MatchCriteria matchCriteria, final boolean checkForReversibility ) throws Exception
	{
		this( inFile, outFile, Arrays.asList( matchCriteria ), checkForReversibility );
	}

	/**
	 * 
	 * @param inFiles
	 * @param outFile
	 * @param matchCriteria
	 * @param checkForReversibility
	 * @throws Exception
	 */
	public SbmlMerger( final File[] inFiles, final File outFile, final MatchCriteria matchCriteria, final boolean checkForReversibility ) throws Exception
	{
		this( inFiles, outFile, Arrays.asList( matchCriteria ), checkForReversibility );
	}

	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @param allMatchCriteria
	 * @param checkForReversibility
	 * @throws Exception
	 */
	public SbmlMerger( final File inFile, final File outFile, final Collection<MatchCriteria> allMatchCriteria, final boolean checkForReversibility ) throws Exception
	{
		this( inFile.isDirectory() ? inFile.listFiles( fileFilter ) : new File[] { inFile }, outFile, allMatchCriteria, checkForReversibility );
	}

	/**
	 * 
	 * @param inFiles
	 * @param outFile
	 * @param allMatchCriteria
	 * @param checkForReversibility
	 * @throws Exception
	 */
	public SbmlMerger( final File[] inFiles, final File outFile, final Collection<MatchCriteria> allMatchCriteria, final boolean checkForReversibility ) throws Exception
	{
		this.outFile = outFile;
		this.allMatchCriteria = allMatchCriteria;
		this.checkForReversibility = checkForReversibility;

		final List<SBMLDocument> documents = new ArrayList<>();

		for( File file : inFiles )
		{
			if( file.isFile() )
			{
				documents.add( SBMLReader.read( file ) );
			}
		}

		inDocuments = documents.toArray( new SBMLDocument[ documents.size() ] );
	}

	/**
	 * 
	 * @param inDocuments
	 * @param matchCriteria
	 * @param checkForReversibility
	 */
	public SbmlMerger( final SBMLDocument[] inDocuments, final MatchCriteria matchCriteria, final boolean checkForReversibility )
	{
		this( inDocuments, Arrays.asList( matchCriteria ), checkForReversibility );
	}

	/**
	 * 
	 * @param inDocuments
	 * @param allMatchCriteria
	 * @param checkForReversibility
	 */
	public SbmlMerger( final SBMLDocument[] inDocuments, final Collection<MatchCriteria> allMatchCriteria, final boolean checkForReversibility )
	{
		this.outFile = null;
		this.allMatchCriteria = allMatchCriteria;
		this.checkForReversibility = checkForReversibility;
		this.inDocuments = Arrays.copyOf( inDocuments, inDocuments.length );
	}

	/**
	 * 
	 * @param mergeSmiles
	 */
	public void setMergeSmiles( final boolean mergeSmiles )
	{
		this.mergeSmiles = mergeSmiles;
	}

	/**
	 * 
	 * @param unsetNotes
	 */
	public void setUnsetNotes( final boolean unsetNotes )
	{
		this.unsetNotes = unsetNotes;
	}

	/**
	 * 
	 * @return SBMLDocument
	 * @throws Exception
	 */
	public SBMLDocument run() throws Exception
	{
		final SBMLDocument outDocument = new SBMLDocument( SbmlUtils.DEFAULT_LEVEL, SbmlUtils.DEFAULT_VERSION );
		final Model model = outDocument.createModel( StringUtils.getUniqueId() );
		final Map<String,Collection<String>> smilesToIds = mergeSmiles ? new HashMap<String,Collection<String>>() : null;
		final Map<OntologyTerm,Collection<String>> ontologyTermToIds = new HashMap<>();
		final Map<List<HashSet<List<Object>>>,String> allReactionDefinitions = new HashMap<>();

		for( final SBMLDocument subDocument : inDocuments )
		{
			subDocument.setLevelAndVersion( model.getLevel(), model.getVersion() );
			final Model subModel = subDocument.getModel();

			// Merge model annotations:
			SbmlUtils.addOntologyTerms( model, SbmlUtils.getOntologyTerms( subModel ) );

			final Map<String,String> originalIdToNewId = new HashMap<>();
			mergeCompartments( model, subModel.getListOfCompartments(), ontologyTermToIds, originalIdToNewId );
			mergeUnitDefinitions( model, subModel.getListOfUnitDefinitions(), ontologyTermToIds, originalIdToNewId );
			mergeSpecies( model, subModel.getListOfSpecies(), smilesToIds, ontologyTermToIds, originalIdToNewId, MergeUtils.getReactantToProducts( subModel ) );
			mergeParameters( model, subModel.getListOfParameters(), ontologyTermToIds, originalIdToNewId );
			mergeReactions( model, subModel, subModel.getListOfReactions(), ontologyTermToIds, originalIdToNewId, allReactionDefinitions );
		}

		outDocument.setNamespace( "http://www.sbml.org/sbml/level2/version4" ); //$NON-NLS-1$
		SbmlUtils.updateAnnotations( outDocument.getModel() );

		if( outFile != null )
		{
			XmlFormatter.getInstance().write( outDocument, outFile );
		}

		return outDocument;
	}

	/**
	 * 
	 * @param outModel
	 * @param listOfCompartments
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @throws Exception
	 */
	protected void mergeCompartments( final Model outModel, final ListOf<Compartment> listOfCompartments, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId ) throws Exception
	{
		for( int l = 0; l < listOfCompartments.size(); l++ )
		{
			final Compartment sbase = listOfCompartments.get( l );

			if( unsetNotes )
			{
				sbase.unsetNotes();
			}

			MergeUtils.addCompartment( outModel, sbase, ontologyTermToIds, originalIdToNewId );
		}
	}

	/**
	 * 
	 * @param outModel
	 * @param listOfUnitDefinitions
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @throws Exception
	 */
	private void mergeUnitDefinitions( final Model outModel, final ListOf<UnitDefinition> listOfUnitDefinitions, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId ) throws Exception
	{
		for( int l = 0; l < listOfUnitDefinitions.size(); l++ )
		{
			final UnitDefinition sbase = listOfUnitDefinitions.get( l );

			if( unsetNotes )
			{
				sbase.unsetNotes();
			}

			MergeUtils.addUnitDefinition( outModel, sbase, ontologyTermToIds, originalIdToNewId );
		}
	}

	/**
	 * 
	 * @param outModel
	 * @param listOfParameters
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @throws Exception
	 */
	private void mergeParameters( final Model outModel, final ListOf<Parameter> listOfParameters, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId ) throws Exception
	{
		for( int l = 0; l < listOfParameters.size(); l++ )
		{
			final Parameter sbase = listOfParameters.get( l );

			if( unsetNotes )
			{
				sbase.unsetNotes();
			}

			MergeUtils.addParameter( outModel, sbase, ontologyTermToIds, originalIdToNewId );
		}
	}

	/**
	 * 
	 * @param outModel
	 * @param listOfSpecies
	 * @param smilesToIds
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @param reactantToProducts
	 * @throws Exception
	 */
	protected void mergeSpecies( final Model outModel, final ListOf<Species> listOfSpecies, final Map<String,Collection<String>> smilesToIds, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId, final Map<String,Collection<String>> reactantToProducts ) throws Exception
	{
		for( int l = 0; l < listOfSpecies.size(); l++ )
		{
			final Species sbase = listOfSpecies.get( l );

			if( unsetNotes )
			{
				sbase.unsetNotes();
			}

			MergeUtils.addSpecies( outModel, sbase, smilesToIds, ontologyTermToIds, originalIdToNewId, allMatchCriteria, reactantToProducts );
		}
	}

	/**
	 * 
	 * @param outModel
	 * @param inModel
	 * @param listOfReactions
	 * @param ontologyTermToIds
	 * @param originalIdToNewId
	 * @param allReactionDefinitions
	 * @throws Exception
	 */
	protected void mergeReactions( final Model outModel, final Model inModel, final ListOf<Reaction> listOfReactions, final Map<OntologyTerm,Collection<String>> ontologyTermToIds, final Map<String,String> originalIdToNewId, final Map<List<HashSet<List<Object>>>,String> allReactionDefinitions ) throws Exception
	{
		for( int l = 0; l < listOfReactions.size(); l++ )
		{
			final Reaction sbase = listOfReactions.get( l );

			if( unsetNotes )
			{
				sbase.unsetNotes();
			}

			MergeUtils.addReaction( outModel, inModel, sbase, ontologyTermToIds, originalIdToNewId, allReactionDefinitions, checkForReversibility );
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		final boolean matchIdentical = args.length == 2 || Boolean.parseBoolean( args[ 2 ] );
		final List<MatchCriteria> matchCriteria = new ArrayList<>();
		matchCriteria.add( matchIdentical ? MatchCriteria.IDENTICAL : MatchCriteria.ANY );

		final SbmlMerger sbmlMerger = new SbmlMerger( new File( args[ 0 ] ), new File( args[ 1 ] ), matchCriteria, true );
		sbmlMerger.setUnsetNotes( true );
		sbmlMerger.run();
	}
}