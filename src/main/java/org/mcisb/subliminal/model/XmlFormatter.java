package org.mcisb.subliminal.model;

import java.io.*;
import javax.xml.stream.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.mcisb.subliminal.kegg.*;
import org.sbml.jsbml.*;

/**
 * 
 * @author neilswainston
 * 
 */
public class XmlFormatter
{
	/**
	 * 
	 */
	private static final SBMLWriter writer = new SBMLWriter();

	/**
	 * 
	 */
	private Transformer transformer;

	/**
	 * 
	 */
	private static XmlFormatter xmlFormatter = null;

	/**
	 * 
	 * @return XmlFormatter
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 * @throws IOException
	 */
	public synchronized static XmlFormatter getInstance() throws TransformerConfigurationException, TransformerFactoryConfigurationError, IOException
	{
		if( xmlFormatter == null )
		{
			xmlFormatter = new XmlFormatter();
		}

		return xmlFormatter;
	}

	/**
	 * 
	 * @throws TransformerConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws IOException
	 */
	private XmlFormatter() throws TransformerConfigurationException, TransformerFactoryConfigurationError, IOException
	{
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();

		try ( final InputStream is = XmlFormatter.class.getResourceAsStream( "indenter.xsl" ) ) //$NON-NLS-1$
		{
			transformer = transformerFactory.newTransformer( new StreamSource( is ) );
		}
	}

	/**
	 * 
	 * @param document
	 * @param outFile
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws SBMLException
	 * @throws XMLStreamException
	 * @throws KeggUtils.getOrganismName
	 *             ( keggOrganismId )
	 */
	public void write( final SBMLDocument document, final File outFile ) throws TransformerFactoryConfigurationError, TransformerException, SBMLException, XMLStreamException, IOException
	{
		final Reader reader = new StringReader( writer.writeSBMLToString( document ) );
		write( reader, outFile );
	}

	/**
	 * 
	 * @param document
	 * @param os
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws SBMLException
	 * @throws XMLStreamException
	 */
	public void write( final SBMLDocument document, final OutputStream os ) throws TransformerFactoryConfigurationError, TransformerException, SBMLException, XMLStreamException
	{
		final Reader reader = new StringReader( writer.writeSBMLToString( document ) );
		write( reader, os );
	}

	/**
	 * 
	 * @param inFile
	 * @param outFile
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws SBMLException
	 * @throws IOException
	 */
	public void write( final File inFile, final File outFile ) throws TransformerFactoryConfigurationError, TransformerException, SBMLException, IOException
	{
		try ( final Reader reader = new FileReader( inFile ) )
		{
			write( reader, outFile );
		}
	}

	/**
	 * 
	 * @param reader
	 * @param outFile
	 * @throws TransformerException
	 * @throws IOException
	 */
	private void write( final Reader reader, final File outFile ) throws TransformerException, IOException
	{
		try ( final OutputStream os = new FileOutputStream( outFile ) )
		{
			write( reader, os );
		}
	}

	/**
	 * 
	 * @param reader
	 * @param outFile
	 * @throws TransformerException
	 */
	private void write( final Reader reader, final OutputStream os ) throws TransformerException
	{
		final StreamSource xmlInput = new StreamSource( reader );
		final StreamResult xmlOutput = new StreamResult( os );
		transformer.transform( xmlInput, xmlOutput );
	}

	/**
	 * 
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws TransformerException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 * @throws SBMLException
	 */
	public static void main( String[] args ) throws SBMLException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException, XMLStreamException, IOException
	{
		XmlFormatter.getInstance().write( new SBMLReader().readSBML( args[ 0 ] ), new File( args[ 1 ] ) );
	}
}