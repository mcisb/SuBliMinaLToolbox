/**
 * 
 */
package org.mcisb.subliminal.kegg;

import java.io.*;
import java.util.*;
import org.junit.*;

/**
 * @author Neil Swainston
 */
public class KeggUtilsTest
{
	/**
	 * 
	 */
	private final KeggUtils keggExtracterUtils = new KeggExtracterUtils();

	/**
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void getOrganismIds() throws IOException
	{
		Assert.assertTrue( Arrays.asList( KeggUtils.getOrganismIds() ).contains( "sce" ) ); //$NON-NLS-1$
	}

	/**
	 * 
	 * @throws IOException
	 */
	@Test
	public void getTaxonomyId() throws IOException
	{
		Assert.assertTrue( keggExtracterUtils.getTaxonomyId( "ant" ).equals( "572480" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( keggExtracterUtils.getTaxonomyId( "sce" ).equals( "559292" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("static-method")
	@Test
	public void getKeggOrganismId() throws IOException
	{
		Assert.assertTrue( KeggUtils.getKeggOrganismId( "572480" ).equals( "ant" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( KeggUtils.getKeggOrganismId( "559292" ).equals( "sce" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * 
	 * @throws IOException
	 */
	@Test
	public void getOrganismName() throws IOException
	{
		Assert.assertTrue( keggExtracterUtils.getOrganismName( "ant" ).equals( "Arcobacter nitrofigilis DSM 7299" ) ); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue( keggExtracterUtils.getOrganismName( "sce" ).equals( "Saccharomyces cerevisiae S288c" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}
}