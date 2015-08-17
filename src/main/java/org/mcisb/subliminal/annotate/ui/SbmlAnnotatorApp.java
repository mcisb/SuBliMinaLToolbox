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
package org.mcisb.subliminal.annotate.ui;

import java.util.*;
import javax.swing.*;
import org.mcisb.ontology.*;
import org.mcisb.ontology.chebi.*;
import org.mcisb.ontology.go.*;
import org.mcisb.ontology.sbo.*;
import org.mcisb.ontology.uniprot.*;
import org.mcisb.subliminal.annotate.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlAnnotatorApp extends AbstractSbmlAnnotatorApp
{
	/**
	 * 
	 */
	private final Map<Object,List<OntologySource>> elementIdentifierToOntologySources;

	/**
	 * 
	 * @param frame
	 * @param elementIdentifierToOntologySources
	 * @param silent
	 */
	public SbmlAnnotatorApp( final JFrame frame, final Map<Object,List<OntologySource>> elementIdentifierToOntologySources, final boolean silent )
	{
		super( frame, silent );
		this.elementIdentifierToOntologySources = elementIdentifierToOntologySources;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mcisb.subliminal.annotate.ui.AbstractSbmlAnnotatorApp#getTask()
	 */
	@Override
	public AbstractSbmlAnnotatorTask getTask()
	{
		return new SbmlAnnotatorTask( elementIdentifierToOntologySources, silent );
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		final OntologySource goUtils = GoUtils.getInstance();
		final OntologySource chebiUtils = ChebiUtils.getInstance();
		final OntologySource uniProtUtils = new UniProtUtils( UniProtUtils.UniProtUtilsSearchOption.FULL_TEXT );
		final Map<Object,List<OntologySource>> elementIdentifierToOntologySources = new HashMap<>();
		elementIdentifierToOntologySources.put( Integer.valueOf( SboUtils.SIMPLE_CHEMICAL ), Arrays.asList( chebiUtils ) );
		elementIdentifierToOntologySources.put( Integer.valueOf( SboUtils.POLYPEPTIDE_CHAIN ), Arrays.asList( uniProtUtils ) );
		elementIdentifierToOntologySources.put( "compartment", Arrays.asList( goUtils ) ); //$NON-NLS-1$
		elementIdentifierToOntologySources.put( "species", Arrays.asList( chebiUtils ) ); //$NON-NLS-1$
		final JFrame app = new JFrame();
		new SbmlAnnotatorApp( app, elementIdentifierToOntologySources, args.length > 0 ? Boolean.parseBoolean( args[ 0 ] ) : false ).show();
	}
}