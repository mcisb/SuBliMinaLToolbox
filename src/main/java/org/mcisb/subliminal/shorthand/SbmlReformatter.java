package org.mcisb.subliminal.shorthand;

import java.util.*;
import org.mcisb.ontology.*;
import org.mcisb.sbml.*;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.CVTerm.Type;
import org.sbml.jsbml.*;

/**
 * 
 * @author neilswainston
 */
public class SbmlReformatter
{
	/**
	 * 
	 * @param model
	 * @throws Exception
	 */
	public static void reformatInchis( final Model model ) throws Exception
	{
		final String OLD_PREFIX = "InChI=1/"; //$NON-NLS-1$
		final String NEW_PREFIX = "InChI=1S/"; //$NON-NLS-1$

		for( Species species : model.getListOfSpecies() )
		{
			final Map<String,Object> notes = SbmlUtils.getNotes( species );
			final Object inchi = notes.remove( SbmlUtils.INCHI );

			if( inchi != null )
			{
				final String reformattedInchi = inchi.toString().replace( OLD_PREFIX, NEW_PREFIX );
				final OntologyTerm ontologyTerm = OntologyUtils.getInstance().getOntologyTerm( Ontology.INCHI, reformattedInchi );

				if( ontologyTerm != null )
				{
					SbmlUtils.addOntologyTerm( species, ontologyTerm, Type.BIOLOGICAL_QUALIFIER, Qualifier.BQB_IS );
				}

				species.unsetNotes();
				SbmlUtils.setNotes( species, notes );
			}
		}
	}
}