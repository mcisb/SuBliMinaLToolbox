/**
 * 
 */
package org.mcisb.subliminal.balance;

import java.io.*;
import org.junit.*;
import org.mcisb.sbml.*;
import org.mcisb.util.io.*;
import org.sbml.jsbml.*;

/**
 * @author Neil Swainston
 */
public class SbmlReactionBalancerTaskTest
{
	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void balance() throws Exception
	{
		final int MAX_STOICHIOMETRIC_COEFFICIENT = 8;
		final File in = File.createTempFile( "inFile", ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$
		final File out = File.createTempFile( "outFile", ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$

		try ( final OutputStream os = new FileOutputStream( in ) )
		{
			new StreamReader( getClass().getClassLoader().getResourceAsStream( "org/mcisb/subliminal/balance/balance.xml" ), os ).read(); //$NON-NLS-1$
		}

		new SbmlReactionBalancerTask( in, out, MAX_STOICHIOMETRIC_COEFFICIENT ).run();

		final SBMLDocument document = SBMLReader.read( out );
		final Model model = document.getModel();

		final Reaction metabolic = model.getReaction( "metabolic" ); //$NON-NLS-1$
		Assert.assertTrue( metabolic.getNumReactants() == 2 );
		Assert.assertTrue( metabolic.getNumProducts() == 2 );
		Assert.assertTrue( metabolic.getReactant( 0 ).getStoichiometry() == 4 );
		Assert.assertTrue( metabolic.getReactant( 1 ).getStoichiometry() == 2 );
		Assert.assertTrue( metabolic.getProduct( 0 ).getStoichiometry() == 4 );
		Assert.assertTrue( metabolic.getProduct( 1 ).getStoichiometry() == 2 );

		final Reaction transport = model.getReaction( "transport" ); //$NON-NLS-1$
		Assert.assertTrue( transport.getReactant( 0 ).getStoichiometry() == 5 );
		Assert.assertTrue( transport.getReactant( 1 ).getStoichiometry() == 2 );
		Assert.assertTrue( transport.getProduct( 0 ).getStoichiometry() == 5 );
		Assert.assertTrue( transport.getProduct( 1 ).getStoichiometry() == 2 );

		final Reaction boundary = model.getReaction( "boundary" ); //$NON-NLS-1$
		Assert.assertTrue( boundary.getNumReactants() == 1 );
		Assert.assertTrue( boundary.getNumProducts() == 0 );
	}

	/**
	 * @throws Exception
	 * 
	 */
	@Test
	public void balanceFractionalAndRepeatingUnits() throws Exception
	{
		final int MAX_STOICHIOMETRIC_COEFFICIENT = 8;
		final File in = File.createTempFile( "inFile", ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$
		final File out = File.createTempFile( "outFile", ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$

		try ( final OutputStream os = new FileOutputStream( in ) )
		{
			new StreamReader( getClass().getClassLoader().getResourceAsStream( "org/mcisb/subliminal/balance/R_LPS2.xml" ), os ).read(); //$NON-NLS-1$
		}

		final SbmlReactionBalancerTask reactionBalancer = new SbmlReactionBalancerTask( in, out, MAX_STOICHIOMETRIC_COEFFICIENT );
		reactionBalancer.run();
		reactionBalancer.printResult();

		final SBMLDocument document = SBMLReader.read( out );
		final Model model = document.getModel();
		final Reaction reaction = model.getReaction( 0 );

		Assert.assertTrue( reaction.getReactant( 0 ).getStoichiometry() == 1 );
		Assert.assertTrue( reaction.getReactant( 1 ).getStoichiometry() == 1 );
		Assert.assertTrue( reaction.getProduct( 0 ).getStoichiometry() == 1 );
		Assert.assertTrue( reaction.getProduct( 1 ).getStoichiometry() == 1 );
		Assert.assertTrue( reaction.getProduct( 2 ).getStoichiometry() == 1 );

		Assert.assertTrue( SbmlUtils.getFormula( model, model.getSpecies( "M_mag_hs_c" ) ).equals( "C19H38O4" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}
}