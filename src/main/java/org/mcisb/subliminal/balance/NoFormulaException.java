/**
 * 
 */
package org.mcisb.subliminal.balance;

import org.sbml.jsbml.*;

/**
 * @author Neil Swainston
 */
class NoFormulaException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	private final Species species;

	/**
	 * @param message
	 */
	public NoFormulaException( final String message, final Species species )
	{
		super( message );
		this.species = species;
	}

	/**
	 * @param cause
	 */
	public Species getSpecies()
	{
		return species;
	}
}