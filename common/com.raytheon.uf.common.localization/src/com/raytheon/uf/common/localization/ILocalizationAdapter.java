/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/

package com.raytheon.uf.common.localization;

import java.io.File;
import java.util.Date;
import java.util.Map;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.exception.LocalizationException;

/**
 * Provides a single, unified interface for localization interactions with the
 * appropriate back-end (CAVE or EDEX) to be implemented.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * May 15, 2008 #878        chammack    Initial creation
 * Jul 24, 2014 3378        bclement    removed unneeded abstract modifiers
 *                                      added createCache()
 * Nov 12, 2015 4834        njensen     Removed references to ModifiableLocalizationFile
 * Nov 30, 2015 4834        njensen     Removed references to LocalizationOpFailedException
 * Jun 22, 2017 6339        njensen     Updated listDirectory() to take a fileExtension
 *                                      Overrode ListResponse.toString()
 * 
 * </pre>
 * 
 * @author chammack
 */
public interface ILocalizationAdapter {

    /**
     * Return the fully qualified path given a localization context and a file
     * name
     * 
     * @param context
     *            the localization context
     * @param fileName
     *            the filename
     * @return the fully qualified path
     */
    public File getPath(LocalizationContext context, String fileName);

    /**
     * Return the metadata for a file or directory
     * 
     * @param context
     *            the context
     * @param fileName
     *            the file or directory
     * @return the metadata for the file or directoryOutput to a log
     * @throws LocalizationException
     */
    public ListResponse[] getLocalizationMetadata(
            LocalizationContext[] context, String fileName)
            throws LocalizationException;

    /**
     * Retrieve the localization file
     * 
     * @param file
     * @throws LocalizationException
     */
    public void retrieve(LocalizationFile file) throws LocalizationException;

    /**
     * Save a file a modifiable localization file
     * 
     * @param file
     *            the modifiable localization file
     * @throws LocalizationException
     */
    public boolean save(LocalizationFile file) throws LocalizationException;

    /**
     * List a directory given a set of contexts and a path.
     * 
     * Flags indicate whether the search should be recursive and whether it
     * should return only files or both files and directories.
     * 
     * When searching multiple contexts, this will provide the union of the
     * search.
     * 
     * @param context
     *            a set of contexts to search
     * @param path
     *            a path
     * @param fileExtension
     *            the extension to filter files on, or null if no filter
     * @param recursive
     *            whether the search should be recursive
     * @param filesOnly
     *            whether only files should be included in the response
     * @return the responses
     * @throws LocalizationException
     */
    public ListResponse[] listDirectory(LocalizationContext[] context,
            String path, String fileExtension, boolean recursive,
            boolean filesOnly) throws LocalizationException;

    /**
     * Return the localization contexts that should be searched given a
     * localization type
     * 
     * @param type
     *            the type
     * @return the localization contexts
     */
    public LocalizationContext[] getLocalSearchHierarchy(
            LocalizationContext.LocalizationType type);

    /**
     * Return the localization types that are used in this adapter for static
     * files
     * 
     * @return the localization types
     */
    public LocalizationType[] getStaticContexts();

    /**
     * Returns a localization context for the given type and level for the
     * active user.
     * 
     * @param type
     * @param level
     * @return the desired context
     */
    public LocalizationContext getContext(LocalizationType type,
            LocalizationLevel level);

    /**
     * Delete a file given a modifiable localization file
     * 
     * @param file
     *            the modifiable localization file
     * @throws LocalizationException
     */
    public boolean delete(LocalizationFile file) throws LocalizationException;

    public String[] getContextList(LocalizationLevel level)
            throws LocalizationException;

    public LocalizationLevel[] getAvailableLevels();

    /**
     * Determine if the LocalizationFile exists
     * 
     * @param file
     * @return
     */
    public boolean exists(LocalizationFile file);

    /**
     * Create cache using an implementation appropriate for this adapter.
     * 
     * @return
     */
    public Map<LocalizationFileKey, LocalizationFile> createCache();

    /**
     * The metadata of a localization file item
     * <p>
     * Localization file items consist of files and directories. This class
     * provides fields for holding file and directory metadata.
     * </p>
     * 
     */
    public static class ListResponse {

        /** localization context indicating where the item is located */
        public LocalizationContext context;

        /** string containing the filename portion of the item */
        public String fileName;

        /** The checksum of the file */
        public String checkSum;

        /** date indicating the modification time */
        public Date date;

        /** boolean flag indicating if the item is a directory */
        public boolean isDirectory;

        /**
         * defines the level the file is protected at, i.e. the file can not be
         * overridden at user or site level. null if not protected
         */
        public LocalizationLevel protectedLevel;

        /** defines if the file exists on the server */
        public boolean existsOnServer;

        @Override
        public String toString() {
            return "ListResponse [context=" + context + ", fileName=" + fileName
                    + ", checkSum=" + checkSum + ", date=" + date
                    + ", isDirectory=" + isDirectory + ", protectedLevel="
                    + protectedLevel + ", existsOnServer=" + existsOnServer
                    + "]";
        }

    }
}
