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
package org.mcisb.subliminal;

import java.io.*;
import java.util.*;
import org.mcisb.subliminal.kegg.*;
import org.mcisb.subliminal.metacyc.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.subliminal.sbml.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class ReconstructionGenerator
{
	/**
	 * 
	 * @param directory
	 * @throws Exception
	 */
	public static void reconstructAll( final File directory ) throws Exception
	{
		for( String organismId : KeggUtils.getOrganismIds() )
		{
			reconstruct( directory, organismId, true );
		}
	}

	/**
	 * 
	 * @param directory
	 * @param keggOrganismId
	 * @throws Exception
	 */
	public static void reconstructFrom( final File directory, final String keggOrganismId ) throws Exception
	{
		boolean found = false;

		for( String organismId : KeggUtils.getOrganismIds() )
		{
			if( !found && organismId.equals( keggOrganismId ) )
			{
				found = true;
			}

			if( found )
			{
				reconstruct( directory, organismId, true );
			}
		}
	}

	/**
	 * 
	 * @param directory
	 * @param keggOrganismIds
	 * @throws Exception
	 */
	public static void reconstructList( final File directory, final String[] keggOrganismIds ) throws Exception
	{
		for( String keggOrganismId : keggOrganismIds )
		{
			reconstruct( directory, keggOrganismId, true );
		}
	}

	/**
	 * 
	 * @param directory
	 * @param id
	 * @throws Exception
	 */
	public static void reconstruct( final File directory, final String id ) throws Exception
	{
		final boolean isKeggOrganismId = !id.matches( "\\d+" ); //$NON-NLS-1$
		reconstruct( directory, id, isKeggOrganismId );
	}

	/**
	 * 
	 * @param directory
	 * @param id
	 * @param isKeggOrganismId
	 * @throws Exception
	 */
	private static void reconstruct( final File directory, final String id, final boolean isKeggOrganismId ) throws Exception
	{
		String taxonomyId = null;
		String keggOrganismId = null;

		if( isKeggOrganismId )
		{
			final KeggUtils keggUtils = new KeggExtracterUtils();
			taxonomyId = keggUtils.getTaxonomyId( id );
			keggOrganismId = id;
		}
		else
		{
			taxonomyId = id;
			keggOrganismId = KeggUtils.getKeggOrganismId( id );
		}

		if( taxonomyId == null )
		{
			final String[] organismIds = KeggUtils.getOrganismIds();
			throw new UnsupportedOperationException( "KEGG organism id " + keggOrganismId + " unknown. Supported organisms are " + Arrays.toString( organismIds ) + "." ); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}

		if( keggOrganismId == null )
		{
			throw new UnsupportedOperationException( "NCBI Taxonomy id " + taxonomyId + " unknown." ); //$NON-NLS-1$//$NON-NLS-2$
		}

		reconstruct( directory, keggOrganismId, taxonomyId );
	}

	/**
	 * 
	 * @param directory
	 * @param keggOrganismId
	 * @param taxonomyId
	 * @throws Exception
	 */
	private static void reconstruct( final File directory, final String keggOrganismId, final String taxonomyId ) throws Exception
	{
		if( !directory.exists() && !directory.mkdirs() )
		{
			throw new IOException();
		}

		final File modelFile = new File( directory, keggOrganismId + ".xml" ); //$NON-NLS-1$

		if( !modelFile.exists() )
		{
			SBMLDocument document = null;

			try
			{
				final AbstractKeggExtracter keggExtracter = new KeggExtracter();
				document = keggExtracter.run( keggOrganismId );
			}
			catch( UnsupportedOperationException e )
			{
				// KEGG may not contain pathway data for all organisms.
				System.out.println( e.getMessage() );
			}

			final boolean keggDocumentExists = document != null;

			try
			{
				if( document == null )
				{
					document = Extracter.initDocument( taxonomyId );
				}

				MetaCycExtracter.run( taxonomyId, document );
			}
			catch( UnsupportedOperationException e )
			{
				// MetaCyc may not contain all organisms of interest.
				System.out.println( e.getMessage() );

				if( !keggDocumentExists )
				{
					document = null;
				}
			}

			if( document != null )
			{
				ModelGenerater.run( document );
				XmlFormatter.getInstance().write( document, modelFile );
				SbmlFactory.getInstance().unregister();
			}
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		if( args.length == 1 )
		{
			ReconstructionGenerator.reconstructAll( new File( args[ 0 ] ) );
		}
		else if( args.length == 2 )
		{
			ReconstructionGenerator.reconstruct( new File( args[ 0 ] ), args[ 1 ] );
		}
		else if( args.length == 3 && args[ 1 ].equals( "-f" ) ) //$NON-NLS-1$
		{
			ReconstructionGenerator.reconstructFrom( new File( args[ 0 ] ), args[ 2 ] );
		}
		else if( args.length > 2 && args[ 1 ].equals( "-l" ) ) //$NON-NLS-1$
		{
			ReconstructionGenerator.reconstructList( new File( args[ 0 ] ), Arrays.copyOfRange( args, 2, args.length ) );
		}
	}
}