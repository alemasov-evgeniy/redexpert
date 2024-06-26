/*
 * AbstractXMLResourceReaderWriter.java
 *
 * Copyright (C) 2002-2017 Takis Diakoumis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.executequery.repository.spi;

import org.executequery.repository.RepositoryException;
import org.underworldlabs.swing.actions.ActionBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;

/**
 * @author Takis Diakoumis
 */
public abstract class AbstractXMLResourceReaderWriter<T> {

    protected final List<T> readResource(String classPathResource, DefaultHandler handler) throws RepositoryException {

        InputStream input = null;
        ClassLoader cl = ActionBuilder.class.getClassLoader();

        if (cl != null) {

            input = cl.getResourceAsStream(classPathResource);

        } else {

            input = ClassLoader.getSystemResourceAsStream(classPathResource);
        }

        return read(input, handler);
    }

    protected final List<T> read(String filePath, DefaultHandler handler) throws RepositoryException {

        File file = new File(filePath);
        if (file.exists()) {

            try {

                return read(new FileInputStream(file), handler);

            } catch (FileNotFoundException e) {

                handleException(e);
            } catch (RepositoryException e) {
                try {
                    file.delete();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                throw e;
            }

        }

        throw new RepositoryException(
                "Specified resource " + filePath + " does not exist");

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<T> read(InputStream in, DefaultHandler handler) throws RepositoryException {

        if (!(handler instanceof XMLRepositoryHandler<?>)) {

            throw new IllegalArgumentException(
                    "Repository handler must be of type XMLRepositoryHandler");
        }

        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);

            SAXParser parser = factory.newSAXParser();
            parser.parse(in, handler);

            return ((XMLRepositoryHandler) handler).getRepositoryItemsList();

        } catch (ParserConfigurationException e) {

            handleException(e);

        } catch (SAXException e) {

            handleException(e);

        } catch (IOException e) {

            handleException(e);

        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }

        }

        return null;
    }

    protected final void write(String filePath,
                               XMLReader xmlReader, InputSource inputSource) throws RepositoryException {

        OutputStream os = null;
        try {

            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();

            os = new FileOutputStream(new File(filePath));

            SAXSource source = new SAXSource(xmlReader, inputSource);
            StreamResult r = new StreamResult(os);

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, r);

        } catch (TransformerConfigurationException e) {

            handleException(e);

        } catch (FileNotFoundException e) {

            handleException(e);

        } catch (TransformerException e) {

            handleException(e);

        } finally {

            try {
                if (os != null) {

                    os.close();
                }
            } catch (IOException e) {
            }

        }

    }

    private void handleException(Throwable e) throws RepositoryException {

        throw new RepositoryException(e);
    }

}

