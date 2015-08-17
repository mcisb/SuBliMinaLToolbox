/**
 * 
 */
package org.mcisb.subliminal.model;

import org.mcisb.subliminal.*;
import org.sbml.jsbml.*;

/**
 * @author Neil Swainston
 */
public class GeneAssociationGenerater
{
	/**
	 * 
	 */
	private static final String GENE_ASSOCIATION = "GENE_ASSOCIATION"; //$NON-NLS-1$

	/**
	 * 
	 * @param document
	 * @throws Exception
	 */
	public static void run( final SBMLDocument document ) throws Exception
	{
		final String SEPARATOR = " or "; //$NON-NLS-1$
		final Model model = document.getModel();

		for( Reaction reaction : model.getListOfReactions() )
		{
			final StringBuilder builder = new StringBuilder();

			for( ModifierSpeciesReference modifier : reaction.getListOfModifiers() )
			{
				builder.append( modifier.getSpecies() );
				builder.append( SEPARATOR );
			}

			if( builder.length() > 0 )
			{
				builder.setLength( builder.length() - SEPARATOR.length() );
			}

			SubliminalUtils.addNote( reaction, GENE_ASSOCIATION, builder.toString() );
		}
	}
}