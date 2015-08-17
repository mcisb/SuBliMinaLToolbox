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
package org.mcisb.subliminal;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.tar.*;
import org.mcisb.util.io.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author Neil Swainston
 */
public class SubliminalUtils
{
	/**
	 * 
	 */
	public static final int FIRST = 0;

	/**
	 * 
	 */
	public static final int DEFAULT_LEVEL = 2;

	/**
	 * 
	 */
	public static final int DEFAULT_VERSION = 4;

	/**
	 * 
	 */
	public static final double DEFAULT_INITIAL_CONCENTRATION = 1;

	/**
	 * 
	 */
	public static final int SBO_BIOCHEMICAL_REACTION = 176;

	/**
	 * 
	 */
	public static final int SBO_TRANSPORT_REACTION = 185;

	/**
	 * 
	 */
	public static final int SBO_SIMPLE_CHEMICAL = 247;

	/**
	 * 
	 */
	public static final int SBO_POLYPEPTIDE_CHAIN = 252;

	/**
	 * 
	 */
	public static final int SBO_COMPARTMENT = 290;

	/**
	 * 
	 */
	public static final int SBO_PROTEIN_COMPLEX = 297;

	/**
	 * 
	 */
	public static final int SBO_OMITTED_PROCESS = 397;

	/**
	 * 
	 */
	public static final int UNDEFINED_NUMBER = Integer.MIN_VALUE;

	/**
	 * 
	 */
	public final static String BIOMASS_REACTION = "BIOMASS_REACTION"; //$NON-NLS-1$

	/**
	 * 
	 */
	public final static String FORMULA = "FORMULA"; //$NON-NLS-1$

	/**
	 * 
	 */
	public final static String CHARGE = "CHARGE"; //$NON-NLS-1$

	/**
	 * 
	 */
	public final static String INCHI = "INCHI"; //$NON-NLS-1$

	/**
	 * 
	 */
	public final static String SMILES = "SMILES"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String DASH = "-"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String DASH_ENCODED = "_DASH_"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String UNDERSCORE = "_"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String COLON = ":"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String NON_WORD = "\\W+"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String WHITESPACE = "\\s+"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String LINE_SEPARATOR = System.getProperty( "line.separator" ); //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String OPENING_INT = "^(?=\\d)"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String NOTES_START = "<body xmlns=\"http://www.w3.org/1999/xhtml\">"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String NOTES_END = "</body>"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final Object NOTES_OPEN = "<p>"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String NOTES_SEPARATOR = ": "; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final Object NOTES_CLOSE = "</p>"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String COLLECTION_SEPARATOR = ", "; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final int COLLECTION_SEPARATOR_LENGTH = COLLECTION_SEPARATOR.length();

	/**
	 * 
	 */
	private static final String COMPARTMENT_SEPARATOR = "_"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String PRE_OPEN_TAG = "<pre>"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final String PRE_CLOSE_TAG = "</pre>"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final double DEFAULT_STOICHIOMETRY = 1.0;

	/**
	 * 
	 */
	public static final double DEFAULT_COMPARTMENT_SIZE = 1.0;

	/**
	 * 
	 */
	private static final String DOUBLE_REGEXP = "[\\d]+(\\.[\\d]+)?"; //$NON-NLS-1$

	/**
	 * 
	 */
	private static final long PAUSE = 10000;

	/**
	 * 
	 */
	private static boolean creatorInitialised = false;

	/**
	 * 
	 */
	private static Creator creator = null;

	/**
	 * 
	 * @param taxonomyId
	 * @return String
	 * @throws IOException
	 */
	public static String getTaxonomyName( final String taxonomyId ) throws IOException
	{
		String taxonomyName = null;

		try ( final BufferedReader reader = new BufferedReader( new InputStreamReader( new URL( "http://www.ncbi.nlm.nih.gov/taxonomy/?term=" + taxonomyId + "&report=taxon&format=text" ).openStream() ) ) ) //$NON-NLS-1$ //$NON-NLS-2$
		{
			String line = null;

			while( ( line = reader.readLine() ) != null )
			{
				if( line.contains( PRE_OPEN_TAG ) || line.contains( PRE_CLOSE_TAG ) )
				{
					taxonomyName = line.replace( PRE_OPEN_TAG, SubliminalUtils.EMPTY_STRING ).replace( PRE_CLOSE_TAG, SubliminalUtils.EMPTY_STRING );
					break;
				}
			}

			return ( taxonomyName == null || taxonomyName.length() == 0 ) ? null : taxonomyName;
		}
	}

	/**
	 * 
	 * @param document
	 */
	public static void addHistory( final SBMLDocument document )
	{
		if( !document.isSetMetaId() )
		{
			document.setMetaId( getUniqueId() );
		}

		getCreator();

		if( creator != null )
		{
			final History history = new History();
			final Date date = Calendar.getInstance().getTime();
			history.addCreator( creator );
			history.setCreatedDate( date );
			history.setModifiedDate( date );
			document.setHistory( history );
		}
	}

	/**
	 * 
	 * @param sbase
	 * @param resource
	 * @param type
	 * @param qualifier
	 */
	protected static void addCVTerm( final SBase sbase, final String resource, final CVTerm.Type type, final CVTerm.Qualifier qualifier )
	{
		addCVTerms( sbase, Arrays.asList( resource ), type, qualifier );
	}

	/**
	 * 
	 * @param sbase
	 * @param resources
	 * @param type
	 * @param qualifier
	 */
	public static void addCVTerms( final SBase sbase, final Collection<String> resources, final CVTerm.Type type, final CVTerm.Qualifier qualifier )
	{
		if( !sbase.isSetMetaId() )
		{
			sbase.setMetaId( getUniqueId() );
		}

		final CVTerm cvTerm = new CVTerm();

		for( String resource : resources )
		{
			cvTerm.addResource( resource );
		}

		cvTerm.setQualifierType( type );

		if( type == CVTerm.Type.MODEL_QUALIFIER )
		{
			cvTerm.setModelQualifierType( qualifier );
		}
		else if( type == CVTerm.Type.BIOLOGICAL_QUALIFIER )
		{
			cvTerm.setBiologicalQualifierType( qualifier );
		}

		sbase.addCVTerm( cvTerm );
	}

	/**
	 * 
	 * @return String
	 */
	public static String getUniqueId()
	{
		return SubliminalUtils.UNDERSCORE + UUID.randomUUID().toString().replaceAll( SubliminalUtils.DASH, SubliminalUtils.UNDERSCORE );
	}

	/**
	 * 
	 * @param id
	 * @return String
	 */
	public static String getNormalisedId( final String id )
	{
		return id.replaceAll( DASH, DASH_ENCODED ).replaceAll( NON_WORD, UNDERSCORE ).replaceAll( OPENING_INT, UNDERSCORE );
	}

	/**
	 * 
	 * @param id
	 * @param compartmentId
	 * @return String
	 */
	public static String getCompartmentalisedId( final String id, final String compartmentId )
	{
		return id + COMPARTMENT_SEPARATOR + compartmentId;
	}

	/**
	 * 
	 * @param id
	 * @param compartmentId
	 * @return String
	 */
	public static String getDecompartmentalisedId( final String id, final String compartmentId )
	{
		return id.replaceAll( COMPARTMENT_SEPARATOR + compartmentId + "$", EMPTY_STRING ); //$NON-NLS-1$
	}

	/**
	 * 
	 * @param root
	 * @param name
	 * @return File
	 */
	public static File find( final File root, final String name )
	{
		if( root.getName().equals( name ) )
		{
			return root;
		}

		if( root.isDirectory() )
		{
			for( final File child : root.listFiles() )
			{
				final File result = find( child, name );

				if( result != null )
				{
					return result;
				}
			}
		}

		return null;
	}

	/**
	 * 
	 * @param tagged
	 * @return String
	 */
	public static String stripTags( final String tagged )
	{
		final StringBuffer stripped = new StringBuffer();
		boolean inTag = false;

		for( int i = 0; i < tagged.length(); i++ )
		{
			final char c = tagged.charAt( i );

			if( c == '<' )
			{
				inTag = true;
				continue;
			}

			if( c == '>' )
			{
				inTag = false;
				continue;
			}

			if( inTag )
			{
				continue;
			}

			stripped.append( c );
		}

		return stripped.toString().trim();
	}

	/**
	 * 
	 * @param url
	 * @param destinationDirectory
	 * @throws IOException
	 */
	public static void untar( final URL url, final File destinationDirectory ) throws IOException
	{
		if( !destinationDirectory.exists() )
		{
			if( !destinationDirectory.mkdir() )
			{
				throw new IOException();
			}
		}

		TarArchiveInputStream is = null;

		try
		{
			is = new TarArchiveInputStream( new GZIPInputStream( url.openStream() ) );
			ArchiveEntry tarEntry = null;

			while( ( tarEntry = is.getNextEntry() ) != null )
			{
				final File destination = new File( destinationDirectory, tarEntry.getName() );

				if( tarEntry.isDirectory() )
				{
					if( !destination.mkdir() )
					{
						// Take no action.
						// throw new IOException();
					}
				}
				else
				{
					try ( final OutputStream os = new FileOutputStream( destination ) )
					{
						new StreamReader( is, os ).read();
					}
				}
			}
		}
		catch( IOException e )
		{
			if( !destinationDirectory.delete() )
			{
				throw new IOException();
			}
			throw e;
		}
		finally
		{
			if( is != null )
			{
				is.close();
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @param id
	 * @param name
	 * @return Compartment
	 */
	public static Compartment addCompartment( final Model model, final String id, final String name )
	{
		final int DEFAULT_SIZE = 1;
		final Compartment compartment = model.createCompartment();
		compartment.setId( id );
		compartment.setName( name );
		compartment.setSize( DEFAULT_SIZE );
		compartment.setSBOTerm( SubliminalUtils.SBO_COMPARTMENT );
		return compartment;
	}

	/**
	 * 
	 * @param query
	 * @return List<String[]>
	 * @throws Exception
	 */
	public static List<String[]> searchUniProt( final String query ) throws Exception
	{
		final int MAX_ATTEMPTS = 128;
		final List<String[]> results = new ArrayList<>();
		final URL url = new URL( "http://www.uniprot.org/uniprot/?query=" + query + "&format=tab&columns=id,protein%20names" ); //$NON-NLS-1$ //$NON-NLS-2$
		BufferedReader reader = null;

		for( int i = 0; i < MAX_ATTEMPTS; i++ )
		{
			try
			{
				reader = new BufferedReader( new InputStreamReader( url.openStream() ) );
				boolean first = true;
				String line = null;

				while( ( line = reader.readLine() ) != null )
				{
					if( first )
					{
						first = false;
					}
					else
					{
						final String[] tokens = line.split( "\t" ); //$NON-NLS-1$

						final String id = tokens[ 0 ].trim();
						String name = null;

						for( String token : tokens[ 1 ].split( "\\(" ) ) //$NON-NLS-1$
						{
							name = token.replaceAll( "\\)", "" ).trim(); //$NON-NLS-1$ //$NON-NLS-2$
							break;
						}

						results.add( new String[] { id, name } );
					}
				}

				return results;
			}
			catch( Exception e )
			{
				if( i == MAX_ATTEMPTS - 1 )
				{
					throw e;
				}

				Thread.sleep( PAUSE );
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}
		}

		return results;
	}

	/**
	 * 
	 */
	public static String encodeUniProtSearchTerm( final String term )
	{
		final String encodedTerm = term.replaceAll( "[^a-zA-Z0-9]", "\\\\$0" ).replaceAll( "\\_", "\\\\_" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return encodedTerm;
	}

	/**
	 * 
	 * @param sbase
	 * @param notes
	 * @throws XMLStreamException
	 */
	public static void setNotes( final SBase sbase, final Map<String,Object> notes ) throws XMLStreamException
	{
		sbase.unsetNotes();

		final StringBuilder builder = new StringBuilder( LINE_SEPARATOR );
		builder.append( NOTES_START );
		builder.append( LINE_SEPARATOR );

		for( Map.Entry<String,Object> entry : notes.entrySet() )
		{
			final Object value = entry.getValue();

			if( value != null )
			{
				builder.append( NOTES_OPEN );
				builder.append( entry.getKey() );
				builder.append( NOTES_SEPARATOR );
				builder.append( value );
				builder.append( NOTES_CLOSE );
				builder.append( LINE_SEPARATOR );
			}
		}

		builder.append( NOTES_END );
		builder.append( LINE_SEPARATOR );
		builder.append( LINE_SEPARATOR );

		sbase.setNotes( builder.toString() );
	}

	/**
	 * 
	 * @param sbase
	 * @param key
	 * @param value
	 * @throws XMLStreamException
	 * @throws UnsupportedEncodingException
	 */
	public static void addNote( final SBase sbase, final String key, final Object value ) throws UnsupportedEncodingException, XMLStreamException
	{
		final Map<String,Object> notes = getNotes( sbase );
		notes.put( key, value );
		setNotes( sbase, notes );
	}

	/**
	 * 
	 * @param sbase
	 * @return Map<String,Object>
	 * @throws XMLStreamException
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String,Object> getNotes( final SBase sbase ) throws UnsupportedEncodingException, XMLStreamException
	{
		final String PARAGRAPH = "p"; //$NON-NLS-1$
		final Map<String,Object> notes = new TreeMap<>();

		if( sbase.isSetNotes() )
		{
			final String sbaseNotes = sbase.getNotesString();

			for( final String element : getElements( PARAGRAPH, new ByteArrayInputStream( sbaseNotes.getBytes( Charset.defaultCharset() ) ), true ) )
			{
				final int index = element.indexOf( NOTES_SEPARATOR );

				if( index == -1 )
				{
					notes.put( element, null );
				}
				else
				{
					notes.put( element.substring( 0, index ), element.substring( index + NOTES_SEPARATOR.length() ).trim() );
				}
			}
		}

		return notes;
	}

	/**
	 * 
	 * @param sbase
	 * @param key
	 * @return Collection<String>
	 * @throws UnsupportedEncodingException
	 * @throws XMLStreamException
	 */
	public static List<String> getNoteValues( final SBase sbase, final String key ) throws UnsupportedEncodingException, XMLStreamException
	{
		final List<String> noteValues = new ArrayList<>();
		final Map<String,Object> notes = getNotes( sbase );
		final Object value = notes.get( key );

		if( value != null && value instanceof String )
		{
			noteValues.addAll( Arrays.asList( ( (String)value ).split( COLLECTION_SEPARATOR ) ) );
		}

		return noteValues;
	}

	/**
	 * 
	 * @param s
	 * @return double
	 */
	public static double parseStoichiometry( final String s )
	{
		if( s.matches( DOUBLE_REGEXP ) )
		{
			return Double.parseDouble( s );
		}

		return DEFAULT_STOICHIOMETRY;
	}

	/**
	 * 
	 * @param values
	 * @return String
	 */
	public static String toString( final Collection<?> values )
	{
		final StringBuilder builder = new StringBuilder();

		for( Object value : values )
		{
			builder.append( value );
			builder.append( COLLECTION_SEPARATOR );
		}

		if( values.size() > 0 )
		{
			builder.setLength( builder.length() - COLLECTION_SEPARATOR_LENGTH );
		}

		return builder.toString();
	}

	/**
	 * 
	 * @param file
	 */
	public static void delete( final File file )
	{
		if( file.isDirectory() )
		{
			for( File child : file.listFiles() )
			{
				delete( child );
			}
		}

		file.delete();
	}

	/**
	 * 
	 * @return Creator
	 */
	private static Creator getCreator()
	{
		if( !creatorInitialised )
		{
			// Set creator:
			final String creatorGivenName = System.getProperty( "org.mcisb.subliminal.CreatorGivenName" ); //$NON-NLS-1$
			final String creatorFamilyName = System.getProperty( "org.mcisb.subliminal.CreatorFamilyName" ); //$NON-NLS-1$
			final String creatorEmail = System.getProperty( "org.mcisb.subliminal.CreatorEmail" ); //$NON-NLS-1$
			final String creatorOrganisation = System.getProperty( "org.mcisb.subliminal.CreatorOrganisation" ); //$NON-NLS-1$

			if( creatorGivenName != null && creatorFamilyName != null && creatorEmail != null && creatorOrganisation != null )
			{
				creator = new Creator();
				creator.setGivenName( creatorGivenName );
				creator.setFamilyName( creatorFamilyName );
				creator.setEmail( creatorEmail );
				creator.setOrganisation( creatorOrganisation );
			}

			creatorInitialised = true;
		}

		return creator;
	}

	/**
	 * 
	 * @param elementName
	 * @param is
	 * @param onlyValues
	 * @return Collection
	 * @throws XMLStreamException
	 * @throws UnsupportedEncodingException
	 */
	private static Collection<String> getElements( final String elementName, final InputStream is, final boolean onlyValues ) throws XMLStreamException, UnsupportedEncodingException
	{
		final Collection<String> elements = new ArrayList<>();
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader( new InputStreamReader( is, Charset.defaultCharset().name() ) );
		final XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter( new OutputStreamWriter( os, Charset.defaultCharset().name() ) );
		boolean read = false;
		String characters = null;

		while( reader.peek() != null )
		{
			final XMLEvent event = (XMLEvent)reader.next();

			switch( event.getEventType() )
			{
				case XMLStreamConstants.START_DOCUMENT:
				case XMLStreamConstants.END_DOCUMENT:
				{
					// Ignore.
					break;
				}
				case XMLStreamConstants.START_ELEMENT:
				{
					read = read || elementName.equals( event.asStartElement().getName().getLocalPart() );

					if( read && !onlyValues )
					{
						writer.add( event );
					}

					break;
				}
				case XMLStreamConstants.ATTRIBUTE:
				{
					if( read && !onlyValues )
					{
						writer.add( event );
					}
					break;
				}
				case XMLStreamConstants.CHARACTERS:
				{
					if( read && !onlyValues )
					{
						writer.add( event );
					}
					characters = event.asCharacters().getData();
					break;
				}
				case XMLStreamConstants.END_ELEMENT:
				{
					if( read && !onlyValues )
					{
						writer.add( event );
					}
					if( elementName.equals( event.asEndElement().getName().getLocalPart() ) )
					{
						writer.flush();

						if( characters != null )
						{
							elements.add( characters );
						}

						os.reset();
						read = false;
					}
					break;
				}
				default:
				{
					// Ignore
					break;
				}
			}
		}

		return elements;
	}
}