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
package org.mcisb.subliminal.metacyc;

import java.io.*;
import java.util.*;
import org.mcisb.subliminal.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.subliminal.sbml.*;
import org.sbml.jsbml.*;

/**
 * @author Neil Swainston
 */
public class MetaCycExtracter extends Extracter
{
	/**
	 * 
	 */
	private final static String COMPARTMENT_SUFFIX = "_CCO_"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final static String MXN_REF_PREFIX = "metacyc:"; //$NON-NLS-1$

	/**
	 * 
	 * @param taxonomyId
	 * @param outFile
	 * @param metaCycDirectory
	 * @throws Exception
	 */
	public static void run( final String taxonomyId, final File outFile, final File metaCycDirectory ) throws Exception
	{
		final String taxonomyName = SubliminalUtils.getTaxonomyName( taxonomyId );

		if( taxonomyName == null )
		{
			throw new UnsupportedOperationException( "MetaCyc data unavailable for NCBI Taxonomy id " + taxonomyId ); //$NON-NLS-1$
		}

		final SBMLDocument document = initDocument( taxonomyId );
		run( taxonomyId, taxonomyName, document, metaCycDirectory, false );
		XmlFormatter.getInstance().write( document, outFile );
		SbmlFactory.getInstance().unregister();
	}

	/**
	 * 
	 * @param taxonomyId
	 * @param outFile
	 * @throws Exception
	 */
	public static void run( final String taxonomyId, final File outFile ) throws Exception
	{
		final SBMLDocument document = initDocument( taxonomyId );
		run( taxonomyId, document );
		XmlFormatter.getInstance().write( document, outFile );
		SbmlFactory.getInstance().unregister();
	}

	/**
	 * 
	 * @param taxonomyId
	 * @param document
	 * @throws Exception
	 */
	public static void run( final String taxonomyId, final SBMLDocument document ) throws Exception
	{
		final String taxonomyName = SubliminalUtils.getTaxonomyName( taxonomyId );

		if( taxonomyName == null )
		{
			throw new UnsupportedOperationException( "MetaCyc data unavailable for NCBI Taxonomy id " + taxonomyId ); //$NON-NLS-1$
		}

		final File tempDirectory = new File( System.getProperty( "java.io.tmpdir" ) ); //$NON-NLS-1$

		run( taxonomyId, taxonomyName, document, new File( tempDirectory, taxonomyName ), true );
	}

	/**
	 * 
	 * @param taxonomyId
	 * @param document
	 * @param metaCycDirectory
	 * @throws Exception
	 */
	private static void run( final String taxonomyId, final String taxonomyName, final SBMLDocument document, final File metaCycDirectory, final boolean deleteSource ) throws Exception
	{
		try
		{
			final File metaCycSource = MetaCycDownloader.getMetaCycSource( metaCycDirectory, taxonomyName );
			final File sbml = SubliminalUtils.find( metaCycSource, "metabolic-reactions.sbml" ); //$NON-NLS-1$

			if( sbml != null )
			{
				System.out.println( "MetaCyc: " + taxonomyName ); //$NON-NLS-1$
				final MetaCycFactory metaCycFactory = initFactory( metaCycSource );
				final SBMLDocument inDocument = new SBMLReader().readSBML( sbml );
				final Model inModel = inDocument.getModel();

				for( int l = 0; l < inModel.getNumReactions(); l++ )
				{
					final Reaction inReaction = inModel.getReaction( l );
					final Reaction outReaction = addReaction( document.getModel(), inReaction, taxonomyId, metaCycFactory );

					if( inReaction.isSetReversible() )
					{
						outReaction.setReversible( inReaction.getReversible() );
					}
				}

				final Collection<Object[]> resources = new ArrayList<>();
				resources.add( new Object[] { "http://identifiers.org/biocyc/" + metaCycFactory.getOrganismId(), CVTerm.Qualifier.BQB_IS_DESCRIBED_BY } ); //$NON-NLS-1$
				resources.add( new Object[] { "http://identifiers.org/pubmed/10592180", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY } ); //$NON-NLS-1$
				addResources( inModel, resources );
			}

			if( deleteSource )
			{
				SubliminalUtils.delete( metaCycSource );
			}
		}
		catch( FileNotFoundException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param source
	 * @return MetaCycReactionsParser
	 */
	private static MetaCycFactory initFactory( final File source )
	{
		final File versionFile = SubliminalUtils.find( source, "version.dat" ); //$NON-NLS-1$
		final File reactionsFile = SubliminalUtils.find( source, "reactions.dat" ); //$NON-NLS-1$
		final File enzymesFile = SubliminalUtils.find( source, "enzymes.col" ); //$NON-NLS-1$
		return new MetaCycFactory( versionFile, reactionsFile, enzymesFile );
	}

	/**
	 * 
	 * @param outModel
	 * @param inReaction
	 * @param taxonomyId
	 * @param metaCycEnzymeFactory
	 * @param resources
	 * @return Reaction
	 * @throws Exception
	 */
	private static Reaction addReaction( final Model outModel, final Reaction inReaction, final String taxonomyId, final MetaCycFactory metaCycEnzymeFactory ) throws Exception
	{
		final String inReactionId = inReaction.getId();
		Reaction outReaction = addReaction( outModel, getId( inReactionId ), DEFAULT_COMPARTMENT_ID );

		if( outReaction == null )
		{
			outReaction = outModel.createReaction();
			outReaction.setId( inReactionId );
			outReaction.setName( inReaction.getName() );

			for( int l = 0; l < inReaction.getNumReactants(); l++ )
			{
				final SpeciesReference inReactant = inReaction.getReactant( l );
				final SpeciesReference outReactant = outReaction.createReactant();

				final String speciesId = inReactant.getSpecies();
				final Species outSpecies = addSpecies( outModel, getId( speciesId ), inReaction.getModel().getSpecies( speciesId ).getName(), DEFAULT_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );

				outReactant.setSpecies( outSpecies.getId() );
				outReactant.setStoichiometry( inReactant.getStoichiometry() );
			}

			for( int l = 0; l < inReaction.getNumProducts(); l++ )
			{
				final SpeciesReference inProduct = inReaction.getProduct( l );
				final SpeciesReference outProduct = outReaction.createProduct();

				final String speciesId = inProduct.getSpecies();
				final Species outSpecies = addSpecies( outModel, getId( speciesId ), inReaction.getModel().getSpecies( speciesId ).getName(), DEFAULT_COMPARTMENT_ID, SubliminalUtils.SBO_SIMPLE_CHEMICAL );

				outProduct.setSpecies( outSpecies.getId() );
				outProduct.setStoichiometry( inProduct.getStoichiometry() );
			}
		}

		final Map<String,Integer> enzymes = metaCycEnzymeFactory.getEnzymes( inReactionId );
		final String[] enzymeIds = enzymes.keySet().toArray( new String[ enzymes.keySet().size() ] );

		for( String enzymeId : enzymeIds )
		{
			final String formattedEnzymeId = "MetaCyc:" + MetaCycUtils.unencode( enzymeId ); //$NON-NLS-1$
			final List<String[]> results = SubliminalUtils.searchUniProt( SubliminalUtils.encodeUniProtSearchTerm( formattedEnzymeId ) + "+AND+taxonomy:" + taxonomyId );//$NON-NLS-1$
			addEnzymes( outReaction, results, SubliminalUtils.getNormalisedId( formattedEnzymeId ), enzymeId, new ArrayList<Object[]>() );
		}

		return outReaction;
	}

	/**
	 * 
	 * @param id
	 * @return String
	 */
	private static String getId( final String id )
	{
		String formattedId = id;

		if( formattedId.contains( COMPARTMENT_SUFFIX ) )
		{
			formattedId = formattedId.substring( 0, id.indexOf( COMPARTMENT_SUFFIX ) );
		}

		return MXN_REF_PREFIX + MetaCycUtils.unencode( formattedId );
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		if( args.length == 2 )
		{
			MetaCycExtracter.run( args[ 0 ], new File( args[ 1 ] ) );
		}
		else if( args.length == 3 )
		{
			MetaCycExtracter.run( args[ 0 ], new File( args[ 1 ] ), new File( args[ 2 ] ) );
		}
	}
}