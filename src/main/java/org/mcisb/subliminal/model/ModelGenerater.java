package org.mcisb.subliminal.model;

import java.io.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class ModelGenerater
{
	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @throws Exception
	 */
	public static void run( final File inFile, final File outFile ) throws Exception
	{
		final SBMLDocument document = new SBMLReader().readSBML( inFile );
		run( document );
		new SBMLWriter().write( document, outFile );
	}

	/**
	 * 
	 * @param document
	 * @throws Exception
	 */
	public static void run( final SBMLDocument document ) throws Exception
	{
		TransportExtracter.run( document );
		BiomassExtracter.run( document );
		GeneAssociationGenerater.run( document );
		FluxBoundsGenerater.run( document.getModel() );
		// CobraFormatter.run( document );
	}
}