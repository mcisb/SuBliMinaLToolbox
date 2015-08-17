/*******************************************************************************
 * Manchester Centre for Integrative Systems Biology
 * University of Manchester
 * Manchester M1 7ND
 * United Kingdom
 * 
 * Copyright (C) 2007 University of Manchester
 * 
 * This program is released under the Academic Free License ("AFL") v3.0.
 * (http://www.opensource.org/licenses/academic.php)
 *******************************************************************************/
package org.mcisb.subliminal.annotate;

import java.util.*;
import org.mcisb.ontology.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlAnnotatorTask extends AbstractSbmlAnnotatorTask
{
	/**
	 * 
	 * @param elementIdentifierToOntologySources
	 * @param silent
	 */
	public SbmlAnnotatorTask( final Map<Object,List<OntologySource>> elementIdentifierToOntologySources, final boolean silent )
	{
		super( new SbmlAnnotator( elementIdentifierToOntologySources, silent ) );
	}
}