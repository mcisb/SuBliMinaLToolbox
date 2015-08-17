/**
 * 
 */
package org.mcisb.subliminal.merge;

import java.io.*;
import java.util.*;
import org.junit.*;
import org.mcisb.ontology.OntologyUtils.MatchCriteria;
import org.mcisb.subliminal.model.*;
import org.mcisb.util.io.*;
import org.sbml.jsbml.*;

/**
 * @author Neil Swainston
 */
public class SbmlMergerTest
{
	/**
	 * @throws Exception
	 * 
	 */
	@Test
	public void merge() throws Exception
	{
		final File in = File.createTempFile( "inFile", ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$
		final File out = File.createTempFile( "outFile", ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$

		try ( final OutputStream os = new FileOutputStream( in ) )
		{
			new StreamReader( getClass().getClassLoader().getResourceAsStream( "org/mcisb/subliminal/merge/sce00010.xml" ), os ).read(); //$NON-NLS-1$
		}

		final SbmlMerger merger = new SbmlMerger( in, out, Arrays.asList( MatchCriteria.IDENTICAL, MatchCriteria.CONJUGATES ), true );
		merger.setMergeSmiles( true );
		merger.run();

		XmlFormatter.getInstance().write( in, out );

		final Model inModel = SBMLReader.read( in ).getModel();
		final Model outModel = SBMLReader.read( out ).getModel();

		Assert.assertEquals( inModel.getNumCompartments(), outModel.getNumCompartments() );
		Assert.assertEquals( inModel.getNumSpecies(), outModel.getNumSpecies() );
		Assert.assertEquals( inModel.getNumReactions(), outModel.getNumReactions() );
	}
}