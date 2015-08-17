/**
 * 
 */
package org.mcisb.subliminal.model;

// import java.util.*;
import org.mcisb.subliminal.*;
import org.sbml.jsbml.*;

/**
 * @author Neil Swainston
 */
public class FluxBoundsGenerater
{
	/**
	 * 
	 */
	public static final String LOWER_BOUND = "LOWER_BOUND"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String UPPER_BOUND = "UPPER_BOUND"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String FLUX_VALUE = "FLUX_VALUE"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final double ZERO_FLUX = 0;

	/**
	 * 
	 */
	public static final double MIN_FLUX = Double.NEGATIVE_INFINITY;

	/**
	 * 
	 */
	public static final double MAX_FLUX = Double.POSITIVE_INFINITY;

	/**
	 * 
	 */
	private static final ASTNode math = JSBML.readMathMLFromString( "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><ci>FLUX_VALUE</ci></math>" ); //$NON-NLS-1$

	/**
	 * 
	 * @param model
	 */
	public static void run( final Model model )
	{
		for( Reaction reaction : model.getListOfReactions() )
		{
			generateKineticLaw( reaction );
			setObjectiveCoefficient( reaction );
		}
	}

	/**
	 * 
	 * @param reaction
	 * @return KineticLaw
	 */
	public static KineticLaw generateKineticLaw( final Reaction reaction )
	{
		final KineticLaw kineticLaw = getKineticLaw( reaction );

		// Add dummy math element:
		if( !kineticLaw.isSetMath() )
		{
			kineticLaw.setMath( math );
		}

		final String fluxUnit = getFluxUnit( reaction.getModel() );
		final LocalParameter lowerBound = setLocalParameter( reaction, LOWER_BOUND, reaction.getReversible() ? MIN_FLUX : ZERO_FLUX, fluxUnit );
		final LocalParameter upperBound = setLocalParameter( reaction, UPPER_BOUND, MAX_FLUX, fluxUnit );
		setLocalParameter( reaction, FLUX_VALUE, 0, fluxUnit );

		if( lowerBound.getValue() == ZERO_FLUX && upperBound.getValue() > ZERO_FLUX )
		{
			// Irreversible:
			reaction.setReversible( false );
		}
		else if( lowerBound.getValue() < ZERO_FLUX && upperBound.getValue() == ZERO_FLUX )
		{
			// Irreversible, but backwards; therefore flip:
			// SbmlUpdater.reverse( reaction );
			reaction.setReversible( false );
		}
		else if( lowerBound.getValue() == ZERO_FLUX && upperBound.getValue() == ZERO_FLUX )
		{
			// Unblock, based on specified reversibility:
			// lowerBound.setValue( reaction.getReversible() ? MIN_FLUX :
			// ZERO_FLUX );
			// upperBound.setValue( MAX_FLUX );
		}
		else
		{
			// Bounds set as reversible; ensure SBML reflects this:
			reaction.setReversible( true );
		}

		return kineticLaw;
	}

	/**
	 * 
	 * @param reaction
	 * @return KineticLaw
	 */
	public static KineticLaw getKineticLaw( final Reaction reaction )
	{
		KineticLaw kineticLaw = reaction.getKineticLaw();

		if( kineticLaw == null )
		{
			kineticLaw = reaction.createKineticLaw();
		}

		return kineticLaw;
	}

	/**
	 * 
	 * @param reaction
	 * @param id
	 * @return double
	 */
	public static double getParameterValue( final Reaction reaction, final String id )
	{
		final LocalParameter localParameter = getLocalParameter( reaction, id );
		return localParameter == null ? Double.NaN : localParameter.getValue();
	}

	/**
	 * 
	 * @param reaction
	 * @param id
	 * @return LocalParameter
	 */
	public static LocalParameter getLocalParameter( final Reaction reaction, final String id )
	{
		final KineticLaw kineticLaw = getKineticLaw( reaction );
		return kineticLaw.getLocalParameter( id );
	}

	/**
	 * 
	 * @param reaction
	 * @param id
	 * @param value
	 * @param units
	 * @return LocalParameter
	 */
	public static LocalParameter setLocalParameter( final Reaction reaction, final String id, final double value, final String units )
	{
		final KineticLaw kineticLaw = getKineticLaw( reaction );
		LocalParameter parameter = kineticLaw.getLocalParameter( id );

		if( parameter == null )
		{
			parameter = kineticLaw.createLocalParameter( id );

			if( !Double.isNaN( value ) )
			{
				parameter.setValue( value );
			}

			if( units != null )
			{
				parameter.setUnits( units );
			}
		}

		return parameter;
	}

	/**
	 * 
	 * @param reaction
	 */
	public static void setObjectiveCoefficient( final Reaction reaction )
	{
		final String OBJECTIVE_COEFFICIENT = "OBJECTIVE_COEFFICIENT"; //$NON-NLS-1$

		final KineticLaw kineticLaw = reaction.getKineticLaw();
		LocalParameter objectiveCoefficient = kineticLaw.getLocalParameter( OBJECTIVE_COEFFICIENT );

		if( objectiveCoefficient == null )
		{
			objectiveCoefficient = kineticLaw.createLocalParameter( OBJECTIVE_COEFFICIENT );
			objectiveCoefficient.setValue( reaction.getId().equals( SubliminalUtils.BIOMASS_REACTION ) ? 1.0 : 0.0 );
			objectiveCoefficient.setUnits( "dimensionless" ); //$NON-NLS-1$
		}
	}

	/**
	 * 
	 * @param model
	 * @return String
	 */
	private static String getFluxUnit( final Model model )
	{
		final String FLUX_UNIT = "flux_unit"; //$NON-NLS-1$
		UnitDefinition fluxUnitDefinition = model.getUnitDefinition( FLUX_UNIT );

		if( fluxUnitDefinition == null )
		{
			fluxUnitDefinition = model.createUnitDefinition();
			fluxUnitDefinition.setId( FLUX_UNIT );

			final Unit mole = fluxUnitDefinition.createUnit();
			mole.setKind( Unit.Kind.MOLE );

			final Unit second = fluxUnitDefinition.createUnit();
			second.setKind( Unit.Kind.SECOND );
			second.setExponent( -1.0 );
		}

		return fluxUnitDefinition.getId();
	}
}