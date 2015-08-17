/**
 * 
 */
package org.mcisb.subliminal;

import org.junit.*;

/**
 * @author neilswainston
 * 
 */
public class SubliminalUtilsTest
{
	@SuppressWarnings("static-method")
	@Test
	public void searchUniProt() throws Exception
	{
		String searchTerm = "hsa:10"; //$NON-NLS-1$
		String encodedSearchTerm = SubliminalUtils.encodeUniProtSearchTerm( searchTerm );
		String query = encodedSearchTerm + "+AND+taxonomy:9606"; //$NON-NLS-1$
		Assert.assertTrue( SubliminalUtils.searchUniProt( query ).size() == 4 );

		searchTerm = "snm:SP70585_2212"; //$NON-NLS-1$
		encodedSearchTerm = SubliminalUtils.encodeUniProtSearchTerm( searchTerm );
		query = encodedSearchTerm + "+AND+taxonomy:488221"; //$NON-NLS-1$
		Assert.assertTrue( SubliminalUtils.searchUniProt( query ).size() == 1 );
	}
}