package ai.elimu.util.epub;

import ai.elimu.model.content.StoryBook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Deprecated
public class EpubToStoryBookConverterLetsReadAsia {
    
    private static final Logger logger = Logger.getLogger(EpubToStoryBookConverterLetsReadAsia.class);
    
    public static StoryBook getStoryBookFromEpub(File ePubFile) {
        logger.info("getStoryBookFromEpub");
        
        logger.info("Converting \"" + ePubFile + "\" from ePUB to StoryBook");
        
        StoryBook storyBook = new StoryBook();
        
        // Unzip ePUB
        List<File> unzippedFiles = unzipFiles(ePubFile);
        logger.info("unzippedFiles.size(): " + unzippedFiles.size());
        
        // Extract storybook metadata and cover image from the Open Package Format (OPF) file
        for (File unzippedFile : unzippedFiles) {
            // OEBPS/content.opf
            if ("content.opf".equals(unzippedFile.getName())) {
                extractMetadataFromOPF(unzippedFile, storyBook);
            }
        }
        
        // Iterate chapters in the Table of Contents (TOC) file and extract images and paragraphs
        for (File unzippedFile : unzippedFiles) {
            // OEBPS/toc.xhtml
            if ("toc.xhtml".equals(unzippedFile.getName())) {
                extractChaptersFromTOC(unzippedFile, storyBook);
            }
        }
        
        // Delete the temporary folder
        // TODO
        
        return storyBook;
    }
    
    /**
     * Unzip the contents of the ePUB file to a temporary folder.
     */
    private static List<File> unzipFiles(File ePubFile) {
        logger.info("unzipFiles");
        
        List<File> unzippedFiles = new ArrayList<>();
        
        String tmpDir = System.getProperty("java.io.tmpdir");
        logger.info("tmpDir: " + tmpDir);
        File tmpDirElimuAi = new File(tmpDir, "elimu-ai");
        logger.info("tmpDirElimuAi: " + tmpDirElimuAi);
        logger.info("tmpDirElimuAi.mkdir(): " + tmpDirElimuAi.mkdir());
        File unzipDestinationDirectory = new File(tmpDirElimuAi, ePubFile.getName().replace(" ", "_") + "_unzipped");
        logger.info("unzipDestinationDirectory: " + unzipDestinationDirectory);
        logger.info("unzipDestinationDirectory.mkdir(): " + unzipDestinationDirectory.mkdir());
        byte[] buffer = new byte[1024];
        try {
            FileInputStream fileInputStream = new FileInputStream(ePubFile);
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                logger.info("zipEntry: " + zipEntry);
                
                // Create intermediate folders.
                File metaInfDirectory = new File(unzipDestinationDirectory, "META-INF");
                logger.info("metaInfDirectory.mkdir(): " + metaInfDirectory.mkdir());
                File contentDirectory = new File(unzipDestinationDirectory, "OEBPS");
                logger.info("contentDirectory.mkdir(): " + contentDirectory.mkdir());
                
                // E.g. unzipDestinationDirectory + "/" + "META-INF/container.xml"
                File unzipDestinationFile = new File(unzipDestinationDirectory + File.separator + zipEntry.toString());
                logger.info("unzipDestinationFile: " + unzipDestinationFile);
                
                // Write file to disk
                FileOutputStream fileOutputStream = new FileOutputStream(unzipDestinationFile);
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.close();
                
                logger.info("unzipDestinationFile.exists(): " + unzipDestinationFile.exists());
                unzippedFiles.add(unzipDestinationFile);
                
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException ex) {
            logger.error(null, ex);
        } catch (IOException ex) {
            logger.error(null, ex);
        }
        
        return unzippedFiles;
    }
    
    /**
     * Extracts ePUB metadata from the <code>/content/book.opf</code> file, and adds it to a {@link StoryBook} instance.
     * 
     * @param opfFile The XML file containing the metadata.
     * @param storyBook The {@link StoryBook} that will be populated with the metadata extracted from the OPF file.
     * @return the updated {@link StoryBook}.
     */
    private static void extractMetadataFromOPF(File opfFile, StoryBook storyBook) {
        logger.info("extractMetadataFromOPF");
        
        logger.info("Extracting metadata from \"" + opfFile + "\"");
        
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(opfFile);
            NodeList nodeList = document.getDocumentElement().getChildNodes();
            logger.info("nodeList.getLength(): " + nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                logger.info("node: " + node);
                
                // Extract metadata
                if ("metadata".equals(node.getNodeName())) {
                    NodeList metadataNodeList = node.getChildNodes();
                    logger.info("nodeList.getLength(): " + nodeList.getLength());
                    for (int j = 0; j < metadataNodeList.getLength(); j++) {
                        Node metadataNode = metadataNodeList.item(j);
                        logger.info("metadataNode: " + metadataNode);
                        
                        if ("dc:title".equals(metadataNode.getNodeName())) {
                            String title = metadataNode.getTextContent();
                            logger.info("title: " + title);
                            storyBook.setTitle(title);
                        }
                        
                        if ("dc:description".equals(metadataNode.getNodeName())) {
                            String description = metadataNode.getTextContent();
                            logger.info("description: " + description);
                            storyBook.setDescription(description);
                        }
                    }
                }
                
                // Extract cover image
                if ("manifest".equals(node.getNodeName())) {
                    // TODO
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            logger.error(null, ex);
        }
    }
    
    /**
     * Extracts content from the chapters listed in the Table of Contents.
     */
    private static void extractChaptersFromTOC(File tocFile, StoryBook storyBook) {
        logger.info("extractChaptersFromTOC");
        
        logger.info("Extracting paragraphs from \"" + tocFile + "\"");
        
        List<String> chapterReferences = EPubChapterExtractionHelper.extractChapterReferencesFromTableOfContentsFile(tocFile);
        logger.info("chapterReferences.size(): " + chapterReferences.size());
        for (String chapterReference : chapterReferences) {
            File xhtmlChapterFile = new File(tocFile.getParent(), chapterReference);
            logger.info("xhtmlChapterFile: " + xhtmlChapterFile);

            List<String> paragraphs = EPubParagraphExtractionHelper.extractParagraphsFromChapterFile(xhtmlChapterFile);
            logger.info("paragraphs.size(): " + paragraphs.size());
            if (storyBook.getParagraphs() == null) {
                storyBook.setParagraphs(paragraphs);
            } else {
                storyBook.getParagraphs().addAll(paragraphs);
            }
        }                        
    }
}
