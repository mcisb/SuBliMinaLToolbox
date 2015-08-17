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

import org.gnu.glpk.*;

/**
 * @author Neil Swainston
 */
public class LinearProgrammingSolver extends AbstractLinearProgrammingSolver
{
	/**
	 * 
	 */
	private final static int COLUMN_KIND = GLPKConstants.GLP_CV;

	/**
	 * 
	 * @param timeout
	 * @param output
	 */
	public LinearProgrammingSolver( final int timeout, final boolean output )
	{
		super( COLUMN_KIND, timeout, output );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.util.math.linearprogramming.AbstractLinearProgrammingSolver
	 * #solve(org.gnu.glpk.glp_prob)
	 */
	@Override
	protected boolean solve( final glp_prob lp )
	{
		final glp_smcp smcp = new glp_smcp();
		GLPK.glp_init_smcp( smcp );
		smcp.setTm_lim( timeout );

		final int returnValue = GLPK.glp_simplex( lp, smcp );
		return returnValue == 0 /*
								 * && GLPK.glp_mip_status( lp ) ==
								 * GLPKConstants.GLP_OPT
								 */;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.util.math.linearprogramming.AbstractLinearProgrammingSolver
	 * #getObjectiveVal(org.gnu.glpk.glp_prob)
	 */
	@Override
	protected double getObjectiveVal( final glp_prob lp )
	{
		return GLPK.glp_get_obj_val( lp );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mcisb.util.math.linearprogramming.AbstractLinearProgrammingSolver
	 * #getColumnVal(org.gnu.glpk.glp_prob, int)
	 */
	@Override
	protected double getColumnVal( final glp_prob lp, final int index )
	{
		return GLPK.glp_get_col_prim( lp, index );
	}
}