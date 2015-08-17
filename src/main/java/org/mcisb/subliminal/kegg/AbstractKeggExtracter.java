/*******************************************************************************
 * Manchester Institute of Biotechnology
 * University of Manchester
 * Manchester M1 7ND
 * United Kingdom
 * 
 * Copyright (C) 2013 University of Manchester
 * 
 * This program is released under the Academic Free License ("AFL") v3.0.
 * (http://www.opensource.org/licenses/academic.php)
 *******************************************************************************/
package org.mcisb.subliminal.kegg;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import org.mcisb.subliminal.*;
import org.mcisb.subliminal.mnxref.*;
import org.mcisb.subliminal.model.*;
import org.mcisb.subliminal.sbml.*;
import org.sbml.jsbml.*;
import com.ctc.wstx.exc.*;

/**
 * 
 * @author Neil Swainston
 */
public abstract class AbstractKeggExtracter extends Extracter
{
	/**
	 * 
	 */
	private final static String MXN_REF_PREFIX = "kegg:"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final KeggUtils keggUtils;

	/**
	 * 
	 * @param keggUtils
	 */
	public AbstractKeggExtracter( final KeggUtils keggUtils )
	{
		this.keggUtils = keggUtils;
	}

	/**
	 * 
	 * @param id
	 * @param out
	 * @param isKeggOrganismId
	 * @throws Exception
	 */
	public void run( final String id, final File out, final boolean isKeggOrganismId ) throws Exception
	{
		String keggOrganismId = null;

		if( !isKeggOrganismId )
		{
			keggOrganismId = KeggUtils.getKeggOrganismId( id );

			if( keggOrganismId == null )
			{
				throw new IllegalArgumentException( "KEGG: pathway data unavailable for NCBI Taxonomy id " + id ); //$NON-NLS-1$
			}
		}
		else
		{
			keggOrganismId = id;
		}

		File outFile = null;

		if( out.exists() )
		{
			if( out.isDirectory() )
			{
				outFile = new File( out, keggOrganismId + ".xml" ); //$NON-NLS-1$
			}
			else
			{
				outFile = out;
			}
		}
		else
		{
			if( out.getName().contains( "." ) ) //$NON-NLS-1$
			{
				outFile = out;
			}
			else
			{
				out.mkdirs();
				outFile = new File( out, keggOrganismId + ".xml" ); //$NON-NLS-1$
			}
		}

		XmlFormatter.getInstance().write( run( keggOrganismId ), outFile );
		SbmlFactory.getInstance().unregister();
	}

	/**
	 * 
	 * @param keggOrganismId
	 * @return SBMLDocument
	 * @throws Exception
	 */
	public SBMLDocument run( final String keggOrganismId ) throws Exception
	{
		final String taxonomyId = keggUtils.getTaxonomyId( keggOrganismId );
		final String organismName = keggUtils.getOrganismName( keggOrganismId );

		System.out.println( "KEGG: " + organismName ); //$NON-NLS-1$

		final SBMLDocument document = initDocument( taxonomyId );
		final Model model = document.getModel();

		for( String pathway : getPathways( keggOrganismId, organismName ) )
		{
			try
			{
				addPathway( model, pathway, keggOrganismId, taxonomyId );
			}
			catch( RuntimeException e )
			{
				if( e.getCause() instanceof WstxEOFException )
				{
					// Due to empty KGML files:
					System.err.println( "KGML file for pathway " + pathway + " is empty: pathway will be ignored." ); //$NON-NLS-1$ //$NON-NLS-2$
					// e.printStackTrace();
				}
				else
				{
					throw e;
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}

		final Collection<Object[]> resources = new ArrayList<>();
		resources.add( new Object[] { "http://identifiers.org/kegg.genome/" + keggOrganismId, CVTerm.Qualifier.BQB_IS_DESCRIBED_BY } ); //$NON-NLS-1$
		resources.add( new Object[] { "http://identifiers.org/pubmed/9847135", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY } ); //$NON-NLS-1$
		addResources( model, resources );

		return document;
	}

	/**
	 * 
	 * @return List<String>
	 */
	protected abstract List<String> getPathways( final String keggOrganismId, final String organismName ) throws IOException;

	/**
	 * 
	 * @param model
	 * @param pathway
	 * @param keggOrganismId
	 * @param taxonomyId
	 * @throws Exception
	 */
	protected abstract void addPathway( final Model model, final String pathway, final String keggOrganismId, final String taxonomyId ) throws Exception;

	/**
	 * 
	 * @param model
	 * @param kgml
	 * @param keggOrganismId
	 * @param taxonomyId
	 * @throws FactoryConfigurationError
	 * @throws Exception
	 */
	protected void parseKgml( final Model model, final URL kgml, final String keggOrganismId, final String taxonomyId ) throws FactoryConfigurationError, Exception
	{
		try ( final InputStream is = kgml.openStream(); final Reader reader = new InputStreamReader( is, Charset.defaultCharset() ) )
		{
			final XMLEventReader xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader( reader );

			// Update Model:
			Reaction currentReaction = null;

			while( xmlEventReader.hasNext() )
			{
				XMLEvent event = (XMLEvent)xmlEventReader.next();

				switch( event.getEventType() )
				{
					case XMLStreamConstants.START_ELEMENT:
					{
						final StartElement startElement = event.asStartElement();

						if( startElement.getName().getLocalPart().equals( "entry" ) ) //$NON-NLS-1$
						{
							parseEntry( startElement, model, keggOrganismId, taxonomyId );
						}
						else if( startElement.getName().getLocalPart().equals( "reaction" ) ) //$NON-NLS-1$
						{
							String name = null;
							String type = null;

							for( Iterator<?> iterator = startElement.getAttributes(); iterator.hasNext(); )
							{
								final Attribute attribute = (Attribute)iterator.next();

								if( attribute.getName().equals( new QName( "name" ) ) ) //$NON-NLS-1$
								{
									name = attribute.getValue();
								}
								else if( attribute.getName().equals( new QName( "type" ) ) ) //$NON-NLS-1$
								{
									type = attribute.getValue();
								}
							}

							if( name != null )
							{
								for( String reactionToken : name.split( "\\s+" ) ) //$NON-NLS-1$
								{
									final String reactionId = MxnRefReactionUtils.getInstance().getMxnRefId( MXN_REF_PREFIX + getId( reactionToken ) );
									currentReaction = model.getReaction( SubliminalUtils.getCompartmentalisedId( reactionId, DEFAULT_COMPARTMENT_ID ) );

									if( currentReaction == null )
									{
										currentReaction = model.getReaction( SubliminalUtils.getCompartmentalisedId( getId( reactionToken ), DEFAULT_COMPARTMENT_ID ) );
									}

									if( "irreversible".equals( type ) && currentReaction != null ) //$NON-NLS-1$
									{
										currentReaction.setReversible( false );
									}
									else
									{
										currentReaction = null;
									}
								}
							}
						}
						else if( currentReaction != null && startElement.getName().getLocalPart().equals( "substrate" ) ) //$NON-NLS-1$
						{
							for( Iterator<?> iterator = startElement.getAttributes(); iterator.hasNext(); )
							{
								final Attribute attribute = (Attribute)iterator.next();

								if( attribute.getName().equals( new QName( "name" ) ) ) //$NON-NLS-1$
								{
									final String name = attribute.getValue();

									outer: for( String compoundToken : name.split( "\\s+" ) ) //$NON-NLS-1$
									{
										final String substrateId = getId( compoundToken );
										final String mnxRefSubstrateId = MxnRefChemUtils.getInstance().getMxnRefId( MXN_REF_PREFIX + substrateId );

										for( int l = 0; l < currentReaction.getNumProducts(); l++ )
										{
											// TODO: convert substrateId to
											// MNX_REF id:
											final String mnxRefReactionProductId = MxnRefChemUtils.getInstance().getMxnRefId( SubliminalUtils.getDecompartmentalisedId( currentReaction.getProduct( l ).getSpecies(), DEFAULT_COMPARTMENT_ID ) );

											if( mnxRefReactionProductId != null && mnxRefReactionProductId.equals( mnxRefSubstrateId ) )
											{
												flipReaction( currentReaction );
												break outer;
											}
										}
									}
								}
							}
						}
						break;
					}
					default:
					{
						continue;
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param startElement
	 * @param model
	 * @param keggOrganismId
	 * @param taxonomyId
	 * @throws Exception
	 */
	protected abstract void parseEntry( final StartElement startElement, final Model model, final String keggOrganismId, final String taxonomyId ) throws Exception;

	/**
	 * 
	 * @param model
	 * @param id
	 * @param geneNames
	 * @param keggOrganismId
	 * @param taxonomyId
	 * @throws Exception
	 */
	protected void addReaction( final Model model, final String id, final String geneNames, final String keggOrganismId, final String taxonomyId ) throws Exception
	{
		Reaction reaction = addReaction( model, MXN_REF_PREFIX + id, DEFAULT_COMPARTMENT_ID );

		if( reaction == null )
		{
			reaction = model.getReaction( SubliminalUtils.getCompartmentalisedId( id, DEFAULT_COMPARTMENT_ID ) );

			if( reaction == null )
			{
				reaction = getKeggReaction( model, id, DEFAULT_COMPARTMENT_ID );
			}
		}

		// Add modifiers:
		for( String geneName : geneNames.split( "\\s+" ) ) //$NON-NLS-1$
		{
			String geneId = getId( geneName );
			final String keggGeneId = keggOrganismId + ":" + geneId; //$NON-NLS-1$
			final String geneResource = "http://identifiers.org/kegg.genes/" + keggGeneId; //$NON-NLS-1$

			final Collection<Object[]> resources = new ArrayList<>();
			resources.add( new Object[] { geneResource, CVTerm.Qualifier.BQB_IS_ENCODED_BY } );

			final List<String[]> results = SubliminalUtils.searchUniProt( SubliminalUtils.encodeUniProtSearchTerm( getUniProtSearchTerm( keggGeneId ) ) + "+AND+taxonomy:" + taxonomyId );//$NON-NLS-1$
			addEnzymes( reaction, results, geneId, geneName, resources );
		}
	}

	/**
	 * 
	 * @param model
	 * @param id
	 * @return Reaction
	 * @throws Exception
	 * @throws XMLStreamException
	 */
	private static Reaction getKeggReaction( final Model model, final String reactionId, final String compartmentId ) throws XMLStreamException, Exception
	{
		final int KEY = 0;

		final Reaction reaction = model.createReaction();
		reaction.setId( SubliminalUtils.getCompartmentalisedId( reactionId, compartmentId ) );

		final Collection<String> ecResources = new TreeSet<>();

		final BufferedReader reader = new BufferedReader( new InputStreamReader( new URL( "http://rest.kegg.jp/get/" + reactionId ).openStream(), Charset.defaultCharset() ) ); //$NON-NLS-1$
		String line = null;

		while( ( line = reader.readLine() ) != null )
		{
			final String[] tokens = line.split( "\\s+" ); //$NON-NLS-1$

			if( tokens[ KEY ].equals( "DEFINITION" ) ) //$NON-NLS-1$
			{
				reaction.setName( line.replaceAll( tokens[ KEY ], SubliminalUtils.EMPTY_STRING ).trim() );
			}
			else if( tokens[ KEY ].equals( "EQUATION" ) ) //$NON-NLS-1$
			{
				final String equation = line.replaceAll( tokens[ KEY ], SubliminalUtils.EMPTY_STRING ).trim();
				parseReaction( model, reaction, equation, compartmentId );
			}
			else if( tokens[ KEY ].equals( "ENZYME" ) ) //$NON-NLS-1$
			{
				for( int i = 1; i < tokens.length; i++ )
				{
					ecResources.add( "http://identifiers.org/ec-code/" + tokens[ i ] ); //$NON-NLS-1$
				}
			}
		}

		final Collection<String> resources = new ArrayList<>();
		resources.add( "http://identifiers.org/kegg.reaction/" + reactionId ); //$NON-NLS-1$
		SubliminalUtils.addCVTerms( reaction, resources, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS );

		SubliminalUtils.addCVTerms( reaction, ecResources, CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS_VERSION_OF );

		return reaction;
	}

	/**
	 * 
	 * @param keggGeneId
	 * @return String
	 */
	protected abstract String getUniProtSearchTerm( final String keggGeneId );

	/**
	 * @param reaction
	 */
	protected static void flipReaction( final Reaction reaction )
	{
		final Map<String,Double> reactants = new LinkedHashMap<>();
		final Map<String,Double> products = new LinkedHashMap<>();

		while( reaction.getNumReactants() != 0 )
		{
			final SpeciesReference reference = reaction.getReactant( 0 );
			reactants.put( reference.getSpecies(), Double.valueOf( reference.getStoichiometry() ) );
			reaction.removeReactant( 0 );
		}

		while( reaction.getNumProducts() != 0 )
		{
			final SpeciesReference reference = reaction.getProduct( 0 );
			products.put( reference.getSpecies(), Double.valueOf( reference.getStoichiometry() ) );
			reaction.removeProduct( 0 );
		}

		for( Map.Entry<String,Double> entry : reactants.entrySet() )
		{
			final SpeciesReference reference = reaction.createProduct();
			reference.setSpecies( entry.getKey() );
			reference.setStoichiometry( entry.getValue().doubleValue() );
		}

		for( Map.Entry<String,Double> entry : products.entrySet() )
		{
			final SpeciesReference reference = reaction.createReactant();
			reference.setSpecies( entry.getKey() );
			reference.setStoichiometry( entry.getValue().doubleValue() );
		}
	}

	/**
	 * 
	 * @param id
	 * @return String
	 */
	protected static String getId( final String id )
	{
		return id.replaceAll( SubliminalUtils.NON_WORD, SubliminalUtils.UNDERSCORE ).substring( id.indexOf( SubliminalUtils.COLON ) + 1 );
	}

	/**
	 * 
	 * @param reaction
	 * @param reactionString
	 * @param compartmentId
	 * @throws Exception
	 */
	private static void parseReaction( final Model model, final Reaction reaction, final String reactionString, final String compartmentId ) throws Exception
	{
		final String REACTION_SEPARATOR = "\\s+<=>\\s+"; //$NON-NLS-1$
		final int REACTANTS = 0;
		final int PRODUCTS = 1;
		final String[] tokens = reactionString.split( REACTION_SEPARATOR );
		addSpeciesReferences( model, reaction, tokens[ REACTANTS ], compartmentId, true );
		addSpeciesReferences( model, reaction, tokens[ PRODUCTS ], compartmentId, false );
	}

	/**
	 * 
	 * @param reaction
	 * @param term
	 * @param compartmentId
	 * @param reactant
	 * @throws Exception
	 */
	private static void addSpeciesReferences( final Model model, final Reaction reaction, final String term, final String compartmentId, final boolean reactant ) throws Exception
	{
		final int STOICHIOMETRY = 0;
		final int SPECIES = 1;
		final String SEPARATOR = "\\s+\\+\\s+"; //$NON-NLS-1$
		final String[] tokens = term.split( SEPARATOR );

		for( String token : tokens )
		{
			final String[] terms = token.split( SubliminalUtils.WHITESPACE );
			final String speciesId = "kegg:" + terms[ terms.length - 1 ]; //$NON-NLS-1$
			final Species species = addSpecies( model, speciesId, MxnRefChemUtils.getInstance().getName( speciesId ), compartmentId, SubliminalUtils.SBO_SIMPLE_CHEMICAL );
			final SpeciesReference speciesReference = reactant ? reaction.createReactant() : reaction.createProduct();
			speciesReference.setSpecies( species.getId() );

			if( terms.length > SPECIES )
			{
				speciesReference.setStoichiometry( SubliminalUtils.parseStoichiometry( terms[ STOICHIOMETRY ] ) );
			}
		}
	}
}