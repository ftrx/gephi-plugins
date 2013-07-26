/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.fabiantroxler.atlastixmlimporter;

import org.gephi.io.importer.api.FileType;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.io.importer.spi.FileImporterBuilder;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author fabiantroxler
 */

@ServiceProvider(service = FileImporterBuilder.class)

public class ImporterBuilderAtlasTiXML implements FileImporterBuilder {
    public String getName() {
       return "xml";
    }

    public FileType[] getFileTypes() {
       return new FileType[]{new FileType(".xml", "Atlas Ti XML file")};
    }
    
    public FileImporter buildImporter() {
        return new ImporterAtlasTiXML();
    }
 
    public boolean isMatchingImporter(FileObject fileObject) {
       return fileObject.getExt().equalsIgnoreCase("xml");
    }
}
