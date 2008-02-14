package org.apache.pig.backend.datastorage;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Properties;
import java.util.Map;

/**
 * DataStorageElementDescriptor provides methods necessary to manage an
 * element in a DataStorage.
 *
 */

public interface ElementDescriptor extends 
            Comparable<ElementDescriptor> {
    
        public static final String BLOCK_SIZE_KEY = "pig.path.block.size";
        public static final String BLOCK_REPLICATION_KEY = "pig.path.block.replication";
        public static final String LENGTH_KEY = "pig.path.length";
        public static final String MODIFICATION_TIME_KEY = "pig.path.modification.time";
        
        //
        // TODO: more keys
        //
        
        public DataStorage getDataStorage();
    
        /**
         * Opens a stream onto which an entity can be written to.
         * 
         * @param configuration information at the object level
         * @return stream where to write
         * @throws DataStorageException
         */
        public OutputStream create(Properties configuration) 
             throws IOException;

        public OutputStream create() 
            throws IOException;

        /**
         * Copy entity from an existing one, possibly residing in a 
         * different Data Storage.
         * 
         * @param dstName name of entity to create
         * @param dstConfiguration configuration for the new entity
         * @param removeSrc if src entity needs to be removed after copying it
         * @throws DataStorageException for instance, configuration 
         *         information for new entity is not compatible with 
         *         configuration information at the Data
         *         Storage level, user does not have privileges to read from
         *         source entity or write to destination storage...
         */
        public void copy(ElementDescriptor dstName,
                         Properties dstConfiguration,
                         boolean removeSrc) 
            throws IOException;
        
        public void copy(ElementDescriptor dstName,
                         boolean removeSrc) 
            throws IOException;
                
        /**
         * Open for read a given entity
         * 
         * @param configuration
         * @return entity to read from
         * @throws DataStorageExecption e.g. entity does not exist...
         */
        public InputStream open(Properties configuration) throws IOException;

        public InputStream open() throws IOException;

        /**
         * Open an element in the Data Storage with support for random access 
         * (seek operations).
         * 
         * @param configuration
         * @return a seekable input stream
         * @throws DataStorageException
         */
        public SeekableInputStream sopen(Properties configuration) 
             throws IOException;
        
        public SeekableInputStream sopen() throws IOException;
        /**
         * Checks whether the entity exists or not
         * 
         * @param name of entity
         * @return true if entity exists, false otherwise.
         */
        public boolean exists() throws IOException;
        
        /**
         * Changes the name of an entity in the Data Storage
         * 
         * @param newName new name of entity 
         * @throws DataStorageException 
         */
        public void rename(ElementDescriptor newName) 
             throws IOException;

        /**
         * Remove entity from the Data Storage.
         * 
         * @throws DataStorageException
         */
        public void delete() throws IOException;

        /**
         * Retrieve configuration information for entity
         * @return configuration
         */
        public Properties getConfiguration() throws IOException;

        /**
         * Update configuration information for this entity
         *
         * @param newConfig configuration
         * @throws DataStorageException
         */
        public void updateConfiguration(Properties newConfig) 
             throws IOException;
        
        /**
         * List entity statistics
         * @return DataStorageProperties
         */
        public Map<String, Object> getStatistics() throws IOException;
}