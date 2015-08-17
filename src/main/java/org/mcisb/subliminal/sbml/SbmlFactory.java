package org.mcisb.subliminal.sbml;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.xml.stream.*;
import libchebi.*;
import org.mcisb.subliminal.*;
import org.mcisb.subliminal.mnxref.*;
import org.mcisb.subliminal.xref.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SbmlFactory
{
	/**
	 * 
	 */
	private static final String REACTION_SEPARATOR = "\\s+=\\s+"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String BIGG = "bigg"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String BIGG_PREFIX = "bigg_"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String CHEBI = "chebi"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String KEGG = "kegg"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String MXNREF = "MXNREF"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final MxnRefChemUtils chemUtils = MxnRefChemUtils.getInstance();

	/**
	 * 
	 */
	private final MxnRefReactionUtils reactionUtils = MxnRefReactionUtils.getInstance();

	/**
	 * 
	 */
	private final Map<String,Species> idToSpecies = new HashMap<>();

	/**
	 * 
	 */
	private final Map<String,Reaction> idToReaction = new TreeMap<>();

	/**
	 * 
	 */
	private static SbmlFactory instance = null;

	/**
	 * 
	 */
	private SbmlFactory() throws MalformedURLException
	{
		// No implementation.
	}

	/**
	 * 
	 * @return SbmlFactory
	 * @throws MalformedURLException
	 */
	public static SbmlFactory getInstance() throws MalformedURLException
	{
		if( instance == null )
		{
			instance = new SbmlFactory();
		}

		return instance;
	}

	/**
	 * 
	 */
	public void unregister()
	{
		for( NamedSBase sbase : idToSpecies.values() )
		{
			unregister( sbase );
		}

		for( Reaction reaction : idToReaction.values() )
		{
			unregister( reaction );
			reaction.getListOfModifiers().clear();
		}
	}

	/**
	 * 
	 * @param model
	 * @param sbase
	 */
	@SuppressWarnings("unchecked")
	private static void unregister( final NamedSBase sbase )
	{
		final Model model = sbase.getModel();

		if( model != null )
		{
			model.unregister( sbase );

			final SBase parent = sbase.getParentSBMLObject();
			( (ListOf<SBase>)parent ).remove( sbase.getId() );

			// Remove references to KEGG and MetaCyc: they may not be valid for
			// every organism:
			for( Iterator<CVTerm> iterator = sbase.getAnnotation().getListOfCVTerms().iterator(); iterator.hasNext(); )
			{
				final CVTerm cvTerm = iterator.next();

				if( cvTerm.getQualifierType() == CVTerm.Type.BIOLOGICAL_QUALIFIER && cvTerm.getBiologicalQualifierType() == CVTerm.Qualifier.BQB_IS_DESCRIBED_BY )
				{
					iterator.remove();
				}
			}
		}
	}

	/**
	 * 
	 * @param id
	 * @param compartment
	 * @return Species
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws ParseException
	 * @throws NumberFormatException
	 * @throws ChebiException
	 */
	@SuppressWarnings("deprecation")
	public Species getSpecies( final String id, final String compartment ) throws IOException, XMLStreamException, NumberFormatException, ParseException, ChebiException
	{
		final String mxnRefId = chemUtils.getMxnRefId( id );

		if( mxnRefId != null )
		{
			final String compartmentalisedId = SubliminalUtils.getCompartmentalisedId( getSpeciesId( mxnRefId ), compartment );
			Species species = idToSpecies.get( compartmentalisedId );

			if( species == null )
			{
				species = new Species( SubliminalUtils.DEFAULT_LEVEL, SubliminalUtils.DEFAULT_VERSION );
				species.setId( compartmentalisedId );
				species.setName( chemUtils.getName( mxnRefId ) );
				species.setCompartment( compartment );
				species.setInitialConcentration( SubliminalUtils.DEFAULT_INITIAL_CONCENTRATION );
				species.setSBOTerm( SubliminalUtils.SBO_SIMPLE_CHEMICAL );

				// Notes:
				final Map<String,Object> notes = new TreeMap<>();
				notes.put( SubliminalUtils.FORMULA, chemUtils.getFormula( mxnRefId ) );

				final int charge = chemUtils.getCharge( mxnRefId );

				if( charge != SubliminalUtils.UNDEFINED_NUMBER )
				{
					notes.put( SubliminalUtils.CHARGE, Integer.valueOf( charge ) );
					species.setCharge( charge );
				}

				notes.put( SubliminalUtils.INCHI, chemUtils.getInchi( mxnRefId ) );
				notes.put( SubliminalUtils.SMILES, chemUtils.getSmiles( mxnRefId ) );

				// Annotations:
				final Map<String,Collection<String>> xrefIds = chemUtils.getXrefIds( mxnRefId );

				if( xrefIds != null )
				{
					final Collection<String> resources = new ArrayList<>();

					for( Map.Entry<String,Collection<String>> xrefId : xrefIds.entrySet() )
					{
						final String name = xrefId.getKey();
						Collection<String> values = xrefId.getValue();

						if( name.equals( CHEBI ) )
						{
							for( String value : values )
							{
								if( ChebiUtils.isPrimary( value ) )
								{
									resources.add( "http://identifiers.org/chebi/CHEBI:" + value ); //$NON-NLS-1$
								}
							}
						}

						notes.put( name.toUpperCase(), SubliminalUtils.toString( values ) );
					}

					SubliminalUtils.addCVTerms( species, resources, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS );
				}

				notes.put( MXNREF, mxnRefId );
				SubliminalUtils.setNotes( species, notes );

				idToSpecies.put( compartmentalisedId, species );
			}

			return species;
		}

		return null;
	}

	/**
	 * 
	 * @param reactionId
	 * @param compartmentId
	 * @return Reaction
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public Reaction getReaction( final String reactionId, final String compartmentId ) throws IOException, XMLStreamException
	{
		final String mxnRefId = reactionUtils.getMxnRefId( reactionId );

		if( mxnRefId != null )
		{
			final String compartmentalisedId = SubliminalUtils.getCompartmentalisedId( mxnRefId, compartmentId );
			Reaction reaction = idToReaction.get( compartmentalisedId );

			if( reaction == null )
			{
				reaction = new Reaction( SubliminalUtils.DEFAULT_LEVEL, SubliminalUtils.DEFAULT_VERSION );
				reaction.setId( compartmentalisedId );
				reaction.setName( reactionUtils.getDescription( mxnRefId ) );
				reaction.setSBOTerm( SubliminalUtils.SBO_BIOCHEMICAL_REACTION );

				final String equation = reactionUtils.getEquation( mxnRefId );
				parseReaction( reaction, equation, compartmentId );

				// Notes:
				final Map<String,Object> notes = new TreeMap<>();

				// Annotations:
				final Map<String,Collection<String>> xrefIds = reactionUtils.getXrefIds( mxnRefId );

				if( xrefIds != null )
				{
					final Collection<String> resources = new ArrayList<>();

					for( Map.Entry<String,Collection<String>> xrefId : xrefIds.entrySet() )
					{
						final String name = xrefId.getKey();
						Collection<String> values = xrefId.getValue();

						if( name.equals( KEGG ) )
						{
							for( String value : values )
							{
								resources.add( "http://identifiers.org/kegg.reaction/" + value ); //$NON-NLS-1$
							}
						}

						notes.put( name.toUpperCase(), SubliminalUtils.toString( values ) );
					}

					SubliminalUtils.addCVTerms( reaction, resources, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS );

					// EC terms:
					final Collection<String> ecResources = new TreeSet<>();

					for( String ecTerm : reactionUtils.getEC( mxnRefId ) )
					{
						ecResources.add( "http://identifiers.org/ec-code/" + ecTerm ); //$NON-NLS-1$
					}

					SubliminalUtils.addCVTerms( reaction, ecResources, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS_VERSION_OF );
				}

				notes.put( MXNREF, mxnRefId );
				SubliminalUtils.setNotes( reaction, notes );

				idToReaction.put( compartmentalisedId, reaction );
			}

			return reaction;
		}

		return null;
	}

	/**
	 * 
	 * @param reaction
	 * @param reactionString
	 * @param reactionSeparator
	 * @param compartmentId
	 * @throws IOException
	 */
	private void parseReaction( final Reaction reaction, final String reactionString, final String compartmentId ) throws IOException
	{
		final int REACTANTS = 0;
		final int PRODUCTS = 1;
		final String[] tokens = reactionString.split( REACTION_SEPARATOR );
		addSpeciesReferences( reaction, tokens[ REACTANTS ], compartmentId, true );
		addSpeciesReferences( reaction, tokens[ PRODUCTS ], compartmentId, false );
	}

	/**
	 * 
	 * @param reaction
	 * @param term
	 * @param compartmentId
	 * @param reactant
	 * @throws IOException
	 */
	private void addSpeciesReferences( final Reaction reaction, final String term, final String compartmentId, final boolean reactant ) throws IOException
	{
		final int STOICHIOMETRY = 0;
		final String SEPARATOR = "\\s+\\+\\s+"; //$NON-NLS-1$
		final String[] tokens = term.split( SEPARATOR );

		for( String token : tokens )
		{
			final String[] terms = token.split( SubliminalUtils.WHITESPACE );
			final SpeciesReference speciesReference = reactant ? reaction.createReactant() : reaction.createProduct();
			speciesReference.setSpecies( SubliminalUtils.getCompartmentalisedId( getSpeciesId( terms[ terms.length - 1 ] ), compartmentId ) );

			if( terms.length > STOICHIOMETRY )
			{
				speciesReference.setStoichiometry( SubliminalUtils.parseStoichiometry( terms[ STOICHIOMETRY ] ) );
			}
		}
	}

	/**
	 * 
	 * @param mxnRefId
	 * @return String
	 * @throws IOException
	 */
	private String getSpeciesId( final String mxnRefId ) throws IOException
	{
		final Collection<String> biggIds = chemUtils.getXrefIds( mxnRefId, BIGG );

		for( String biggId : biggIds )
		{
			return SubliminalUtils.getNormalisedId( BIGG_PREFIX + biggId );
		}

		return mxnRefId;
	}
}