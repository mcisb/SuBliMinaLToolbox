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
package org.mcisb.util.math.linearprogramming;

import java.util.*;
import org.junit.*;
import org.mcisb.math.linearprogramming.*;

/**
 * 
 * @author Neil Swainston
 */
public class AbstractLinearProgrammingSolverTest
{
	/**
	 * 
	 */
	private static final int TIMEOUT = 200;

	/**
	 * 
	 */
	private static final boolean OUTPUT = true;

	/**
	 * 
	 */
	@SuppressWarnings("static-method")
	@Test
	public void solveLinearProgram() // throws Exception
	{
		solve( new LinearProgrammingSolver( TIMEOUT, OUTPUT ) );
	}

	/**
	 * 
	 */
	@SuppressWarnings("static-method")
	@Test
	public void solveMixedIntegerLinearProgram() // throws Exception
	{
		solve( new MixedIntegerLinearProgrammingSolver( TIMEOUT, OUTPUT ) );
	}

	/**
	 * 
	 */
	@SuppressWarnings("static-method")
	@Test
	public void solveRandomLinearProgram() // throws Exception
	{
		solveRandom( new LinearProgrammingSolver( TIMEOUT, OUTPUT ) );
	}

	/**
	 * 
	 */
	@SuppressWarnings("static-method")
	@Test
	public void solveRandomMixedIntegerLinearProgram() // throws Exception
	{
		solveRandom( new MixedIntegerLinearProgrammingSolver( TIMEOUT, OUTPUT ) );
	}

	/**
	 * 
	 */
	private static void solve( final AbstractLinearProgrammingSolver solver ) // throws
																				// Exception
	{
		final double[][] matrix = new double[ 3 ][];
		final String[] columnIds = new String[] { "A", "B", "C", "D" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		final double[] rowBounds = new double[ matrix.length ];
		final double[] lowerBounds = new double[] { 1, 1, 1, 0 };
		final double[] upperBounds = new double[] { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
		final double[] objectiveCoefficients = new double[ columnIds.length ];
		Arrays.fill( objectiveCoefficients, 1 );

		matrix[ 0 ] = new double[] { 1, -1, 2, -2 };
		matrix[ 1 ] = new double[] { 1, -1, 2, -2 };
		matrix[ 2 ] = new double[] { 1, -1, 2, -2 };

		final double[] solution = solver.solve( matrix, columnIds, rowBounds, lowerBounds, upperBounds, objectiveCoefficients );
		Assert.assertTrue( Arrays.equals( new double[] { 1, 1, 1, 1 }, solution ) );
	}

	/**
	 * 
	 */
	private static void solveRandom( final AbstractLinearProgrammingSolver solver ) // throws
																					// Exception
	{
		final double[][] matrix = new double[ 3 ][];
		final String[] columnIds = new String[] { "A", "B", "C", "D" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		final double[] rowBounds = new double[ matrix.length ];
		final double[] lowerBounds = new double[] { 1, 1, 1, 0 };
		final double[] upperBounds = new double[] { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
		final double[] objectiveCoefficients = new double[ columnIds.length ];
		Arrays.fill( objectiveCoefficients, 1 );

		final Random random = new Random();
		matrix[ 0 ] = new double[] { random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt() };
		matrix[ 1 ] = new double[] { random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt() };
		matrix[ 2 ] = new double[] { random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt() };

		final double[] solution = solver.solve( matrix, columnIds, rowBounds, lowerBounds, upperBounds, objectiveCoefficients );
		Assert.assertFalse( Arrays.equals( new double[] { 1, 1, 1, 1 }, solution ) );
	}
}