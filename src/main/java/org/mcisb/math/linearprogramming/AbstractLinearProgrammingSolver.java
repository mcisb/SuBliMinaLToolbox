/*******************************************************************************
 * Manchester Centre for Integrative Systems Biology
 * University of Manchester
 * Manchester M1 7ND
 * United Kingdom
 * 
 * Copyright (C) 2008 University of Manchester
 * 
 * This program is released under the Academic Free License ("AFL") v3.0.
 * (http://www.opensource.org/licenses/academic.php)
 *******************************************************************************/
package org.mcisb.math.linearprogramming;

import java.util.*;
import org.gnu.glpk.*;

/**
 * @author Neil Swainston
 */
public abstract class AbstractLinearProgrammingSolver
{
	/**
	 * 
	 */
	private static final int MAX_COLUMN_NAME_LENGTH = 255;

	/**
	 * 
	 */
	protected final int timeout;

	/**
	 * 
	 */
	private final int defaultColumnKind;

	/**
	 * 
	 */
	private final boolean output;

	/**
	 * 
	 * @param defaultColumnKind
	 * @param timeout
	 * @param output
	 */
	public AbstractLinearProgrammingSolver( final int defaultColumnKind, final int timeout, final boolean output )
	{
		this.defaultColumnKind = defaultColumnKind;
		this.timeout = timeout;
		this.output = output;
	}

	/**
	 * 
	 * @param matrix
	 * @param columnIds
	 * @param rowBounds
	 * @param lowerBounds
	 * @param upperBounds
	 * @param objectiveCoefficients
	 * @return double[]
	 */
	public double[] solve( final double[][] matrix, final String[] columnIds, final double[] rowBounds, final double[] lowerBounds, final double[] upperBounds, final double[] objectiveCoefficients )
	{
		final int[] columnKinds = new int[ columnIds.length ];
		Arrays.fill( columnKinds, defaultColumnKind );
		return solve( matrix, columnIds, columnKinds, rowBounds, lowerBounds, upperBounds, objectiveCoefficients );
	}

	/**
	 * 
	 * @param matrix
	 * @param columnIds
	 * @param columnKinds
	 * @param rowBounds
	 * @param lowerBounds
	 * @param upperBounds
	 * @param objectiveCoefficients
	 * @return double[]
	 */
	public double[] solve( final double[][] matrix, final String[] columnIds, final int[] columnKinds, final double[] rowBounds, final double[] lowerBounds, final double[] upperBounds, final double[] objectiveCoefficients )
	{
		// Create problem
		final glp_prob lp = GLPK.glp_create_prob();

		if( !output )
		{
			GLPK.glp_term_out( GLPKConstants.GLP_OFF );
		}

		// Define columns
		final int columnCount = columnIds.length;
		GLPK.glp_add_cols( lp, columnCount );

		for( int i = 0; i < columnCount; i++ )
		{
			GLPK.glp_set_col_name( lp, i + 1, columnIds[ i ].length() > MAX_COLUMN_NAME_LENGTH ? columnIds[ i ].substring( 0, MAX_COLUMN_NAME_LENGTH ) : columnIds[ i ] );
			GLPK.glp_set_col_kind( lp, i + 1, columnKinds[ i ] );
			GLPK.glp_set_col_bnds( lp, i + 1, GLPKConstants.GLP_DB, lowerBounds[ i ], upperBounds[ i ] );
		}

		// Create constraints
		final int rowCount = matrix.length;
		GLPK.glp_add_rows( lp, rowCount );

		for( int i = 0; i < rowCount; i++ )
		{
			GLPK.glp_set_row_bnds( lp, i + 1, GLPKConstants.GLP_FX, rowBounds[ i ], rowBounds[ i ] );

			final double[] row = matrix[ i ];
			final SWIGTYPE_p_int ind = GLPK.new_intArray( columnCount + 1 );
			final SWIGTYPE_p_double val = GLPK.new_doubleArray( columnCount + 1 );

			for( int j = 0; j < columnCount; j++ )
			{
				GLPK.intArray_setitem( ind, j + 1, j + 1 );
				GLPK.doubleArray_setitem( val, j + 1, row[ j ] );
			}

			GLPK.glp_set_mat_row( lp, i + 1, columnCount, ind, val );
		}

		// Define objective
		GLPK.glp_set_obj_dir( lp, GLPKConstants.GLP_MIN );

		for( int i = 0; i < objectiveCoefficients.length; i++ )
		{
			GLPK.glp_set_obj_coef( lp, i + 1, objectiveCoefficients[ i ] );
		}

		// Solve model and retrieve solution
		double[] solution = null;

		// if( returnValue == 0 && GLPK.glp_mip_status( lp ) ==
		// GLPKConstants.GLP_OPT )
		if( solve( lp ) )
		{
			if( output )
			{
				write_solution( lp );
			}

			solution = new double[ columnCount ];

			for( int i = 0; i < columnCount; i++ )
			{
				solution[ i ] = getColumnVal( lp, i + 1 );
			}
		}

		// free memory
		GLPK.glp_delete_prob( lp );

		return solution;
	}

	/**
	 * 
	 * @param lp
	 */
	private void write_solution( final glp_prob lp )
	{
		System.out.print( GLPK.glp_get_obj_name( lp ) != null ? GLPK.glp_get_obj_name( lp ) : "OBJECTIVE" ); //$NON-NLS-1$
		System.out.print( " = " ); //$NON-NLS-1$
		System.out.println( getObjectiveVal( lp ) );

		for( int i = 1; i <= GLPK.glp_get_num_cols( lp ); i++ )
		{
			System.out.print( GLPK.glp_get_col_name( lp, i ) );
			System.out.print( " = " ); //$NON-NLS-1$
			System.out.println( getColumnVal( lp, i ) );
		}
	}

	/**
	 * 
	 * @param lp
	 * @return boolean
	 */
	protected abstract boolean solve( final glp_prob lp );

	/**
	 * 
	 * @param lp
	 * @return double
	 */
	protected abstract double getObjectiveVal( final glp_prob lp );

	/**
	 * 
	 * @param lp
	 * @param index
	 * @return double
	 */
	protected abstract double getColumnVal( final glp_prob lp, final int index );
}