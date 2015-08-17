/**
 * 
 */
package org.mcisb.subliminal.model;

import java.util.*;
import org.mcisb.subliminal.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class CobraFormatter extends Extracter
{
	/**
	 * 
	 */
	private static final String BIGG = "BIGG"; //$NON-NLS-1$

	/**
	 * 
	 * @param document
	 * @throws Exception
	 */
	public static void run( final SBMLDocument document ) throws Exception
	{
		final Map<String,String> speciesIdToBiggId = new TreeMap<>();
		final Model model = document.getModel();

		// Update Species ids:
		for( Species species : model.getListOfSpecies() )
		{
			if( species.getSBOTerm() == SubliminalUtils.SBO_SIMPLE_CHEMICAL )
			{
				final List<String> biggIds = SubliminalUtils.getNoteValues( species, BIGG );

				if( biggIds.size() > 0 )
				{
					final String biggId = SubliminalUtils.getNormalisedId( "M_" + biggIds.get( SubliminalUtils.FIRST ) + "_" + species.getCompartment() ); //$NON-NLS-1$ //$NON-NLS-2$
					speciesIdToBiggId.put( species.getId(), biggId );
					species.setId( biggId );
				}
			}
		}

		// Update SpeciesReference ids:
		for( Reaction reaction : model.getListOfReactions() )
		{
			for( SpeciesReference reference : reaction.getListOfReactants() )
			{
				updateReference( reference, speciesIdToBiggId );
			}

			for( SpeciesReference reference : reaction.getListOfProducts() )
			{
				updateReference( reference, speciesIdToBiggId );
			}
		}
	}

	/**
	 * 
	 * @param reference
	 * @param speciesIdToBiggId
	 */
	private static void updateReference( final SpeciesReference reference, final Map<String,String> speciesIdToBiggId )
	{
		final String biggId = speciesIdToBiggId.get( reference.getSpecies() );

		if( biggId != null )
		{
			reference.setSpecies( biggId );
		}
	}
}