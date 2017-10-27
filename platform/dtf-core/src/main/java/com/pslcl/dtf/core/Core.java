/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.core;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.artifact.Content;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.generator.template.Template;
import com.pslcl.dtf.core.generator.template.TestInstance;
import com.pslcl.dtf.core.generator.template.TestInstance.Action.ArtifactUses;
import com.pslcl.dtf.core.storage.DTFStorage;
import com.pslcl.dtf.core.storage.mysql.MySQLDtfStorage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

//import javax.annotation.Nullable; // requires an external jar

/**
 * This class represents the relationship between the program and external resources like
 * filesystems and databases. It also contains all of the synchronization code.
 */
public class Core
{
    // TODO: consider moving this to its own separate class, overcome warning "public inner class"
    public static class DBDescribedTemplate {
        public long pk;
        public Hash documentationHash;

        public DBDescribedTemplate(long pk, Hash documentationHash) {
            this.pk = pk;
            this.documentationHash = documentationHash;
        }
    }

    private static final String singleQuote = "'";

    private final Logger log;

    public PortalConfig config = null;
    private File artifacts = null;
    private DTFStorage storage = null;

    /**
     * The private key of the test that is being generated.
     */
    public long pk_target_test = 0;

    public File getArtifactsDirectory() {
        return this.artifacts;
    }

    PortalConfig getConfig() {
        return this.config;
    }

    /**
      * Constructor, must be followed by calling this.init().
     */
    public Core() {
        this(0L); // 0L is never used for pk_test
    }

    /**
     * Constructor, must be followed by calling this.init().
     * @param pk_test The test number from table test.
     */
    public Core(long pk_test) {
        this.log = LoggerFactory.getLogger(getClass());
        this.pk_target_test = pk_test;
        this.init();
    }

    private void init() {
        this.config = new PortalConfig();
        String dir = config.dirArtifacts();
        if (dir != null) {
            this.artifacts = new File(dir);
        } else {
            this.log.error("<internal> Core constructor: null config artifact directory, return with no further action");
            return;
        }
        if (!this.artifacts.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            this.artifacts.mkdirs();
        }
        this.storage = new MySQLDtfStorage(this, this.config); // opens database
    }

    /**
     * Close the core object, releasing any resources.
     */
    public void close() {
        this.storage.close();
    }

    public boolean isReadOnly() {
        return this.storage.isReadOnly();
    }

    public DTFStorage getStorage() {
        return this.storage;
    }

    /**
     * Delete all previous sequence build numbers of the same version as this module.
     * @param module The module that previous builds of should be deleted. It is required
     * that this module not be already added to the database.
     */
    void deletePriorBuildSequenceNumbers(Module module)
    {
        while (true) {
            long pk = 0;
            try {
                pk = this.storage.findModuleWithoutPriorSequence(module);
            } catch (SQLException sqle) {
                this.log.error("<internal> Core.deletePriorBuildSequenceNumbers(): Continue even though couldn't read modules, " + sqle);
            }
            if (pk == 0)
                break;

            try {
                this.storage.deleteModule(pk);
            } catch (SQLException sqle) {
                this.log.error("<internal> Core.deletePriorBuildSequenceNumbers(): Couldn't delete module, " + sqle);
            }
        }
    }

    /**
     * Add content given an input stream. If the content is already stored, then a new file is created anyway,
     * and assumed to be correct. Then the new file is stored and the database is updated.
     * @param is An input stream for the content.
     * @param length The length of the stream, or (0 or -1) if the entire stream is to be added.
     * @return Hash of the added content
     */
//  @Nullable
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ReturnOfNull"})
    Hash addContent(InputStream is, long length) {
        File tmp;
        FileOutputStream os = null;

        try {
            tmp = File.createTempFile("artifact", "hash");  // tmp has filename "artifact.hash"
            os = new FileOutputStream(tmp);

            @SuppressWarnings("MagicNumber")
            byte[] content = new byte[1024];
            long remaining = length;
            while (remaining>0 || // in this case, remaining will decrement
                   length<=0) {   // in this case, remaining is negative (or 0) and does not decrement
                int consumed = is.read(content, 0, content.length);
                if (consumed < 0) {
                    // consumed -1: no data was read into buffer content because end of stream is reached

                    // For length of (0 or -1), we are asked to consume all bytes of stream is.
                    if (length > 0) {
                        // instead, full length was specified but not every byte was consumed
                        this.log.error("<internal> Core.addContent(): End of file while expanding content.");
                        return null;
                    }
                    // We are asked to consume entire stream (no length specified) and we finally encountered stream end.
                    // There is an odd case where the input stream contained 0 bytes, for that case: output stream os AND file temp are empty.
                    break; // success
                } else if (consumed > 0) {
                    os.write(content, 0, consumed);
                    if (length > 0) {
                        // progress check on specified length
                        remaining -= consumed;
                        if (remaining < 0)
                            this.log.debug("<internal> Core.addContent(): more bytes consumed than requested, while expanding content. actual/requested: " + (length - remaining) + "/" + length);
                    }
                } else {
                    // consumed is 0, an unexpected case: input stream not exhausted but no bytes are consumed, no exception
                    this.log.debug("<internal> Core.addContent(), while expanding content: bytes expected but no bytes consumed; remaining/length: " + remaining + "/" + length);
                    this.log.error("<internal> Core.addContent(): Infinite loop detected and exited, while expanding content.");
                    return null;
                }
            } // end while()
        } catch (Exception e) {
            // Cannot even determine the hash, so we don't know if it has already been added or not.
            this.log.error("<internal> Core.addContent(): Could not add content, " + e.getMessage());
            return null;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignore) {
                    // Ignore
                }
            }
        }

        Hash h = Hash.fromContent(tmp);
        if (h == null) {
            this.log.error("<internal> Core.addContent() Could not compute hash of an artifact's content.");
            return null;
        }

        String strHash = h.toString();
        File target = new File(this.artifacts, strHash);

        boolean dbKnowsOfFileHash = false;
        try {
            dbKnowsOfFileHash = this.storage.artifactFileHashStoredInDB(h);
        } catch (SQLException sqle) {
            this.log.error("<internal> Core.addContent() Continues even though NOT obtaining db read of artifact hash, msg: " + sqle);
        }

        if (!this.storage.isReadOnly()) {
            try {
                // Move the file to the cache, then update db based on success/fail, then if fail, cleanup.
                FileUtils.deleteQuietly(target); // in case target file exists
                FileUtils.moveFile(tmp, target);

                if (dbKnowsOfFileHash) {
                    return h;
                } else {
                    this.storage.addContent(h);
                    return h;
                }
            } catch (SQLException | IOException e) {
                FileUtils.deleteQuietly(tmp); // cleanup
                this.log.error("<internal> Core.addContent(): Continues even though could not add, to db, content file of hash " + strHash + " , exception message" + e);
                // TODO: cleanup this unlikely case that our entry is stored in db
//              if (dbKnowsOfFileHash) {
//              }
                // fall through to return null
            }
        } else {
            // readonly db
            if (dbKnowsOfFileHash) {
                try {
                    FileUtils.deleteQuietly(target); // in case target already exists
                    // move the file to the cache
                    FileUtils.moveFile(tmp, target);
                    return h;
                } catch (IOException ioe) {
                    FileUtils.deleteQuietly(tmp); // cleanup
                    this.log.error("<internal> Core.addContent(): db readonly case, could not write new content file of hash " + strHash + " to storage cache, exception msg " + ioe);
                    return target.exists() ? h : null;
                }
            } else {
                // Cannot write db: the new file hash cannot be saved, so also do not store the new file.
                FileUtils.deleteQuietly(tmp); // basic cleanup
                // But do cleanup the unlikely case that a file of that hash name is stored.
                if (!FileUtils.deleteQuietly(target)) {
                    this.log.error("<internal> Core.addContent(), db readonly case: Could not delete content file, of hash " + h.toString());
                    return h;
                }
                // fall through to return null
            }
        }
        return null;
    }

    /**
     * Return a file for content that exists in the cache.
     * @param h The hash of the file to return.
     * @return A file if it exists, null otherwise.
     *
     */
//  @Nullable
    @SuppressWarnings("ReturnOfNull")
    File getContentFile(Hash h)
    {
        File f = new File(this.artifacts, h.toString());
        if (f.exists())
            return f;

        return null;
    }

    /**
     * This class represents a module that is backed by the core database. Operations on the module will refer
     * to database content.
     */
    public static class DBModule implements Module {
        private Core core;
        public long pk;
        private String organization;
        private String name;
        private Attributes attributes;
        private String version;
        private String status;
        private String sequence;

        public DBModule(Core core, long pk, String organization, String name, String attribute_string, String version, String status, String sequence) {
            this.core = core;
            this.pk = pk;
            this.organization = organization;
            this.name = name;
            this.attributes = new Attributes(attribute_string);
            this.version = version;
            this.status = status;
            this.sequence = sequence;
        }

        @Override
        public String getOrganization() {
            return organization;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes.getAttributes();
        }

        @Override
        public String getSequence() {
            return sequence;
        }

        @Override
        public List<Artifact> getArtifacts() {
            return this.getArtifacts(null, null);
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern) {
            return this.getArtifacts(namePattern, null);
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern, String configuration) {
            List<Artifact> ret = new ArrayList<>();
            try {
                ret = this.core.getStorage().getArtifacts(this.pk, namePattern, configuration);
            } catch (SQLException sqle) {
                this.core.log.error(".getArtifacts() Continues after DTFStorage.getArtifacts() throws exception, msg: " + sqle);
                this.core.log.debug("stack trace", sqle);
            }
            return ret;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((organization == null) ? 0 : organization.hashCode());
            result = prime * result + (int) (pk ^ (pk >>> 32));
            result = prime * result + ((sequence == null) ? 0 : sequence.hashCode());
            result = prime * result + ((status == null) ? 0 : status.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DBModule other = (DBModule) obj;
            if (attributes == null) {
                if (other.attributes != null)
                    return false;
            } else if (!attributes.equals(other.attributes))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (organization == null) {
                if (other.organization != null)
                    return false;
            } else if (!organization.equals(other.organization))
                return false;
            if (pk != other.pk)
                return false;
            if (sequence == null) {
                if (other.sequence != null)
                    return false;
            } else if (!sequence.equals(other.sequence))
                return false;
            if (status == null) {
                if (other.status != null)
                    return false;
            } else if (!status.equals(other.status))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }
    }

    /**
     * This class represents an artifact that is represented in the database. It is the class
     * returned to generators.
     */
    public static class DBArtifact implements Artifact
    {
        private Core core;
        private long pk;
        private Module module;
        private String configuration;
        private String name;
        private int posixMode;
        private Hash hash;
        private String targetFilePath;

        /**
         * Construct an artifact associated with a component, name, version, platform and variant. The content
         * associated with the artifact is passed as a hash.
         * @param core The core managing the database.
         * @param pk The primary key of the artifact.
         * @param module The module the artifact belongs to.
         * @param configuration The configuration the artifact belongs to.
         * @param name The name of the artifact.
         * @param mode The POSIX mode of the artifact.
         * @param hash The hash of the artifact contents.
         * @param targetDirectory The target directory to which the artifact should be deployed.
         */
        public DBArtifact(Core core, long pk, Module module, String configuration, String name, int mode, Hash hash, String targetDirectory) {
            this.core = core;
            this.pk = pk;
            this.module = module;
            this.configuration = configuration;
            this.name = name;
            this.posixMode = mode;
            this.hash = hash;
            this.targetFilePath = targetDirectory;
        }

        /**
         * Construct an artifact associated with a component, name, version, platform and variant. The content
         * associated with the artifact is passed as a hash.
         * @param core The core managing the database.
         * @param pk The primary key of the artifact.
         * @param module The module the artifact belongs to.
         * @param configuration The configuration the artifact belongs to.
         * @param name The name of the artifact.
         * @param mode The POSIX mode of the artifact.
         * @param hash The hash of the artifact contents.
         */
        public DBArtifact(Core core, long pk, Module module, String configuration, String name, int mode, Hash hash) {
            this(core, pk, module, configuration, name, mode, hash, name);
        }

        public long getPK() {
            return pk;
        }

        @Override
        public Module getModule()
        {
            return module;
        }

        @Override
        public String getConfiguration()
        {
            return configuration;
        }

        @Override
        public String getName()
        {
            return name;
        }

//        public String getEncodedName()
//        {
//            try
//            {
//                return URLEncoder.encode(name, "UTF-8");
//            } catch (Exception ignore)
//            {
//                // This should never happen, as UTF-8 is a required charset.
//                return "error";
//            }
//        }

        @Override
        public Content getContent()
        {
            return new DBContent(core, hash);
        }

        @Override
        public int getPosixMode()
        {
            return posixMode;
        }

        @Override
        public String getTargetFilePath() {
            return this.targetFilePath;
        }

        @Override
        public void setTargetFilePath(String targetFilePath) {
            this.targetFilePath = targetFilePath;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
            result = prime * result + ((hash == null) ? 0 : hash.hashCode());
            result = prime * result + posixMode;
            result = prime * result + ((module == null) ? 0 : module.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + (int) (pk ^ (pk >>> 32));
            result = prime * result + ((targetFilePath == null) ? 0 : targetFilePath.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DBArtifact other = (DBArtifact) obj;
            if (configuration == null) {
                if (other.configuration != null)
                    return false;
            } else if (!configuration.equals(other.configuration))
                return false;
            if (hash == null) {
                if (other.hash != null)
                    return false;
            } else if (!hash.equals(other.hash))
                return false;
            if (posixMode != other.posixMode)
                return false;
            if (module == null) {
                if (other.module != null)
                    return false;
            } else if (!module.equals(other.module))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (pk != other.pk)
                return false;
            if (targetFilePath == null) {
                if (other.targetFilePath != null)
                    return false;
            } else if (!targetFilePath.equals(other.targetFilePath))
                return false;
            return true;
        }

        //@Override
        //public String getValue( Template template ) {
        //    return module.getOrganization() + "#" + module.getName() + " " + getEncodedName() + " " + hash.toString();
        //}

    } // end static class DBArtifact

    private static class DBContent implements Content {
        private Core core;
        private Hash hash;

        DBContent(Core core, Hash hash) {
            this.core = core;
            this.hash = hash;
        }

        @Override
        public Hash getHash()
        {
            return hash;
        }

        @Override
        public String getValue(Template template)
        {
            return getHash().toString();
        }

        @Override
        // TODO: consider acceptable solution to replace null return
//      @Nullable
//      @SuppressWarnings("ReturnOfNull")
        public InputStream asStream()
        {
            File f = core.getContentFile(hash);
            if (f != null) {
                try {
                    return new FileInputStream(f);
                } catch (Exception ignore) {
                    // Ignore
                }
            }

            return null;
        }

        @Override
        @SuppressWarnings("ZeroLengthArrayAllocation")
        public byte[] asBytes()
        {
            File f = core.getContentFile(hash);
            if (f != null) {
                try {
                    return FileUtils.readFileToString(f, Charset.defaultCharset()).getBytes();
                } catch (Exception ignore) {
                    // Ignore
                }
            }
            return new byte[0]; // this is better than returning null, which poses a null pointer threat to the caller
        }

    } // end static class DBContent

    private void safeClose(ResultSet r) {
        try {
            if (r != null)
                r.close();
        } catch (Exception ignore) {
            // Ignore
        }
    }

    private void safeClose(Statement s) {
        try {
            if (s != null)
                s.close();
        } catch (Exception ignore) {
            // Ignore
        }
    }

    /*    public void syncGeneratedContent( Content sync ) {
            PreparedStatement find_content = null;
            PreparedStatement create_content = null;
            PreparedStatement mark_synchronized = null;
            ResultSet resultSet = null;

            if ( read_only ) {
                System.err.println( "------------------------" );
                System.err.println( "Generated Content: " + sync.getHash().toString() );
                System.err.println( "Content: " + sync.getContent() );
                System.err.println( "------------------------" );
                return;
            }

            try {
                if ( ! this.artifacts.isDirectory() )
                    //noinspection ResultOfMethodCallIgnored
                    this.artifacts.mkdirs();

                find_content = connect.prepareStatement( "SELECT hex(pk_content) FROM content WHERE pk_content=?" );
                create_content = connect.prepareStatement( "INSERT INTO content (pk_content,is_generated) VALUES (?,1)" );
                mark_synchronized = connect.prepareStatement( "UPDATE content SET is_generated=1 WHERE pk_content=?" );

                // Always update the file if it doesn't exist, independent of database.
                File a = new File( this.artifacts, sync.getHash().toString() );
                if ( ! a.exists() ) {
                    FileUtils.writeStringToFile( a, sync.getContent() );
                }


                find_content.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));
                resultSet = find_content.executeQuery();
                if ( ! resultSet.isBeforeFirst() ) {
                    // There is no content. Need to add.
                    safeClose( resultSet ); resultSet = null;

                    create_content.setBinaryStream( 1, new ByteArrayInputStream( sync.getHash().toBytes() ) );
                    create_content.executeUpdate();
                }
                else {
                    safeClose( resultSet ); resultSet = null;

                    mark_synchronized.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));
                    mark_synchronized.executeUpdate();
                }
            }
            catch ( Exception e ) {
                System.err.println( "ERROR: Couldn't synchronize content, " + e.getMessage() );
            }
            finally {
                safeClose( resultSet ); resultSet = null;
                safeClose( find_content ); find_content = null;
                safeClose( create_content ); create_content = null;
                safeClose( mark_synchronized ); mark_synchronized = null;
            }
        }
    */
//    void addToSet(DescribedTemplate dt, Set<DescribedTemplate> set)
//    {
//        if (!set.contains(dt))
//        {
//            set.add(dt);
//            for (DescribedTemplate child : dt.getDependencies())
//                addToSet(child, set);
//        }
//    }

    /**
     * Add a described template - it is known to not exist.
     * @param dt The described template to add.
     * @param result The result to report, if any.
     * @param owner The owner to assign, if any.
     * @param start The init time, or null.
     * @param ready The ready time, or null.
     * @param complete The complete time, or null.
     * @return The key information for the added described template.
     */
    private DBDescribedTemplate add(DescribedTemplate dt, Boolean result, String owner, Date start, Date ready, Date complete) throws Exception {
        try {
            DescribedTemplate.Key matchKey = dt.getKey();
            Optional<DBDescribedTemplate> dbdtAsStored = this.storage.getDBDescribedTemplate(matchKey);
            if (dbdtAsStored.isPresent())
                return dbdtAsStored.get();
        } catch (SQLException sqle) {
            this.log.error("<internal> Core.add() sees exception from one of the dbQuery methods, msg: " + sqle);
            this.log.debug("stack trace: ", sqle);
            throw new Exception(".add() exits with exception ", sqle);
        }

        // Recursively process all dependent DescribedTemplate's (add them or check them). But .getDependencies() is empty.
        // Original TODO: Figure out if this logic is correct. Doesn't appear to be. Has not been tested, since .getDependencies() is empty.
        for (DescribedTemplate child : dt.getDependencies()) {
            DescribedTemplate.Key matchKey = child.getKey();
            Optional<DBDescribedTemplate> dbdtAsStored;
            try {
                dbdtAsStored = this.storage.getDBDescribedTemplate(matchKey);
            } catch (SQLException sqle) {
                this.log.error("<internal> Core.add() sees exception from .getDBDescribedTemplate(), msg: " + sqle);
                this.log.debug("stack trace: ", sqle);
                throw new Exception(".add() exits with exception ", sqle);
            }

            if (!dbdtAsStored.isPresent())
                this.add(child, null, null, null, null, null);
            else
                this.getStorage().check(child);
        }

        // proceed with the "actual" .add() behavior, by calling .addToDB()
        try {
            Optional<DBDescribedTemplate> optional = this.storage.addToDB(dt, result, owner, start, ready, complete);
            if (optional.isPresent())
                return optional.get();
        } catch (SQLException sqle) {
            this.log.error("<internal> Core.add() sees exception from .addToDB(), msg: " + sqle);
        }

        // here by SQLException, OR by this.storage.addToDB() returning Optional.empty()
        throw new Exception(".add() calls .addToDB() and sees that it failed to add to database");
    }

    /**
     * Compare all described templates, deleting those that should not exist, adding
     * those that need to be created, and updating those that need to be updated.
     * Updates are limited to documentation changes.
     * @param testInstances A list of test instances to be synced.
     * @throws Exception on any error
     * @return count of added Described Templates.
     */
    public int syncDescribedTemplates(Iterable<TestInstance> testInstances) throws Exception {
        // parameter testInstances may be a small array or large, depending on Generator.maxInstanceAccumulationCount and the progress being made, as many generator scripts compete for execution time
        int addedDescribedTemplatesCount = 0;
        int checkedNotAddedDescribedTemplatesCount = 0;
        for (TestInstance ti : testInstances) {
            Optional<DBDescribedTemplate> dbdtAsStored;
            DescribedTemplate.Key matchKey = ti.getDescribedTemplate().getKey();
            try {
                dbdtAsStored = this.storage.getDBDescribedTemplate(matchKey);
            } catch (SQLException sqle) {
                this.log.error("<internal> Core.syncDescribedTemplate() Continues even though .getDBDescribedTemplate() throws exception, msg: " + sqle);
                this.log.debug("stack trace: ", sqle);
                continue;
            }

            DBDescribedTemplate dbdt;
            if (!dbdtAsStored.isPresent()) {
                // add the described template to table described_template
                dbdt = this.add(ti.getDescribedTemplate(), ti.getResult(), ti.getOwner(), ti.getStart(), ti.getReady(), ti.getComplete());
                ++addedDescribedTemplatesCount;
            } else {
                // check the stored described template
                dbdt = this.getStorage().check(ti.getDescribedTemplate());
                ++checkedNotAddedDescribedTemplatesCount;
            }

            if (dbdt != null) {
                // For object ti (might not be in db), we have object dbdt (which is in db table described_template).
                // In the database, is there a matching test_instance entry? It relates the current test (pk_test) to the current described template dbdt.

                // note that ti.pk may be unfilled right now- cannot use it for db lookup, as shown
//              statement.executeQuery("SELECT pk_test_instance FROM test_instance WHERE test_instance.pk_test_instance=" + Long.toString(ti.pk));

                // see if test_instance.fk_described_template exists to match dbdt.pk
                boolean dbNotHaveTI = false;
                try {
                    dbNotHaveTI = this.storage.describedTemplateHasTestInstanceMatch(dbdt.pk);
                } catch (SQLException sqle) {
                    this.log.error("<internal> Core.syncDescribedTemplate() Continues even though .describedTemplateHasTestInstanceMatch() throws exception, msg: " + sqle);
                    this.log.debug("stack trace: ", sqle);
                    continue;
                }

                if (dbNotHaveTI) {
                    // add our test instance to database
                    try {
                        ti.pk = this.storage.insertTestInstance(this.pk_target_test, dbdt.pk);
                    } catch (SQLException sqle) {
                        this.log.error("<internal> Core.syncDescribedTemplate() Continues even though .insetTestInstance() throws exception, msg: " + sqle);
                        continue;
                    }

                    // Insert all of the module references
                    List<TestInstance.Action> actions = ti.getActions();
                    for (TestInstance.Action action : actions) {
                        ArtifactUses au = action.getArtifactUses();
                        if (au == null)
                            continue;

                        Iterator<Artifact> iter = au.getArtifacts();
                        while (iter.hasNext()) {
                            Artifact artifact = iter.next();

                            long pk_module;
                            try {
                                pk_module = this.storage.findModule(artifact.getModule());
                            } catch (Exception sqle) {
                                this.log.error("<internal> Core.syncDescribedTemplates(): Continues even though couldn't find module, msg: " + sqle);
                                continue;
                            }
                            try {
                                this.storage.addModuleToTestInstanceEntry(pk_module, ti.pk);
                            } catch (SQLException ignore) {
                                // Ignore, since many times our new entry will be a duplicate.
                            }
                        }
                    }
                    dbdt.pk = ti.pk;
                }

                // If the ti has a result recorded, then make sure it is reflected in the run table.
                if (ti.getResult() != null || ti.getOwner() != null) {
                    Statement statement2 = null;
                    ResultSet resultSet = null;
                    Boolean dbResult = null;
                    String dbOwner = null;
                    try {
                        statement2 = this.storage.getConnect().createStatement();
                        resultSet = statement2.executeQuery("SELECT result, owner FROM run JOIN test_instance ON test_instance.fk_run = run.pk_run WHERE test_instance.pk_test_instance=" + Long.toString(ti.pk));

                        if (resultSet.next()) {
                            dbResult = resultSet.getBoolean("result");
                            if (resultSet.wasNull())
                                dbResult = null;

                            dbOwner = resultSet.getString("owner");
                            if (resultSet.wasNull())
                                dbOwner = null;
                        }
                    } catch (Exception ignore) {
                        // Ignore
                    } finally {
                        safeClose(resultSet);
                        resultSet = null;
                        safeClose(statement2);
                        statement2 = null;
                    }

                    // Check the run status, fix it if the status is known.
                    if (!Objects.equals(dbResult, ti.getResult()) ||
                        (dbOwner==null ? ti.getOwner()!=null : !Objects.equals(dbOwner, ti.getOwner()))) {
                        try {
                            // call a stored procedure that updates table run
                            this.storage.reportResult(ti.getDescribedTemplate().getTemplate().getHash().toString(), ti.getResult(), ti.getOwner(), ti.getStart(), ti.getReady(), ti.getComplete());
                        } catch (SQLException sqle) {
                            this.log.error("Core.syncDescribedTemplate() Continues after .reportResult() throws exception, msg: " + sqle);
                        }
                    }
                }
            } else {
                this.log.debug("<internal> Core.syncDescribedTemplates() computes null DBDescribedTemplate");
                throw new Exception("null DBDescribedTemplate");
            } // end if (dbdt)
        } // end for()

        if (checkedNotAddedDescribedTemplatesCount > 0)
            this.log.debug("<internal> Core.syncDescribedTemplates() checked (without adding) " + checkedNotAddedDescribedTemplatesCount + " described templates in database for test " + this.pk_target_test);
        return addedDescribedTemplatesCount;
    }

//    private List<Hash> getExistingTopArtifacts(long pk)
//    {
//        List<Hash> result = new ArrayList<Hash>();
//        PreparedStatement findArtifacts = null;
//        ResultSet foundArtifacts = null;
//
//        // Read all the related artifacts
//        try
//        {
//            findArtifacts = this.storage.getConnect().prepareStatement(String.format("select fk_content from template_to_all_content where fk_template='%d'", pk));
//            foundArtifacts = findArtifacts.executeQuery();
//            while (foundArtifacts.next())
//            {
//                Hash hash = new Hash(foundArtifacts.getBytes(1));
//                result.add(hash);
//            }
//        } catch (SQLException ignore)
//        {
//            // Ignore
//        } finally
//        {
//            safeClose(foundArtifacts);
//            foundArtifacts = null;
//            safeClose(findArtifacts);
//            findArtifacts = null;
//        }
//
//        return result;
//    }

    // Not called
//    private void getRequiredTopArtifacts(long pk, Set<Hash> combined)
//    {
//        PreparedStatement findArtifacts = null;
//        ResultSet foundArtifacts = null;
//        ResultSet foundChildren = null;
//
//        // Read all the related artifacts
//        try
//        {
//            findArtifacts = this.storage.getConnect().prepareStatement(String.format("select fk_content from template_to_content where fk_template='%d'", pk));
//            foundArtifacts = findArtifacts.executeQuery();
//            while (foundArtifacts.next())
//            {
//                Hash hash = new Hash(foundArtifacts.getBytes(1));
//                combined.add(hash);
//            }
//
//            PreparedStatement findChildren = this.storage.getConnect().prepareStatement(String.format("select fk_child from template_to_template where fk_parent='%d'", pk));
//            foundChildren = findChildren.executeQuery();
//            while (foundChildren.next())
//            {
//                long fk = foundChildren.getLong("fk_child");
//                getRequiredTopArtifacts(fk, combined);
//            }
//        } catch (Exception ignore)
//        {
//            // Ignore
//        } finally
//        {
//            safeClose(foundArtifacts);
//            foundArtifacts = null;
//            safeClose(foundChildren);
//            findArtifacts = null;
//        }
//    }

    // Not called
//    /**
//     * Roll up the artifact relationships for all top-level templates. Top-level templates are those referenced
//     * directly from a test instance. This roll-up allows SQL queries to map from an artifact to a test instance
//     * for artifact result reports.
//     */
//    void syncTopTemplateRelationships()
//    {
//        // Find all top-level templates.
//        if (this.storage.isReadOnly())
//            return;
//
//        /* Templates always have the same "contents" and "relationships" or their hash would change. This
//         * means that the worst synchronization problem can be a crash while we were in the process of adding
//         * relationships. An existing relationship will never be wrong.
//         */
//        ResultSet foundTemplates = null;
//        PreparedStatement insertArtifact = null;
//        try
//        {
//            PreparedStatement findTemplates = this.storage.getConnect().prepareStatement("select distinct fk_template from test_instance");
//            foundTemplates = findTemplates.executeQuery();
//            while (foundTemplates.next())
//            {
//                long pk = foundTemplates.getLong("fk_template");
//                List<Hash> existing = getExistingTopArtifacts(pk);
//                Set<Hash> required = new HashSet<Hash>();
//                getRequiredTopArtifacts(pk, required);
//
//                // Worst case we missed adding some last time.
//                required.removeAll(existing);
//                for (Hash h : required)
//                {
//                    // Need to add the relationship.
//                    insertArtifact = this.storage.getConnect().prepareStatement("INSERT INTO template_to_all_content (fk_template, fk_content) VALUES (?,?)");
//                    insertArtifact.setLong(1, pk);
//                    insertArtifact.setBinaryStream(2, new ByteArrayInputStream(h.toBytes()));
//                    insertArtifact.executeUpdate();
//                }
//            }
//        } catch (Exception ignore)
//        {
//            // Ignore.
//        } finally
//        {
//            safeClose(foundTemplates);
//            foundTemplates = null;
//            safeClose(insertArtifact);
//            insertArtifact = null;
//        }
//    }

//    public void syncTemplateRelationships(Template sync)
//    {
//        if (this.storage.isReadOnly())
//            return;
//
//        PreparedStatement statement = null;
//        ResultSet resultSet = null;
//        try
//        {
//            for (Template t : sync.allTemplates)
//            {
//                statement = this.storage.getConnect().prepareStatement(String.format("select fk_parent, fk_child from template_to_template where fk_parent='%d' and fk_child='%d'", sync.getPK(), t.getPK()));
//                resultSet = statement.executeQuery();
//                if (!resultSet.isBeforeFirst())
//                {
//                    // There were no matches. Time to insert. Need to determine if the content exists.
//                    safeClose(resultSet);
//                    resultSet = null;
//                    safeClose(statement);
//                    statement = null;
//
//                    statement = this.storage.getConnect().prepareStatement("INSERT INTO template_to_template (fk_parent, fk_child) VALUES (?,?)");
//                    statement.setLong(1, sync.getPK());
//                    statement.setLong(2, t.getPK());
//                    statement.executeUpdate();
//
//                    safeClose(statement);
//                    statement = null;
//                } else
//                {
//                    safeClose(resultSet);
//                    resultSet = null;
//                }
//            }
//
//            for (Content a : sync.artifacts)
//            {
//                statement = this.storage.getConnect().prepareStatement("select fk_template, fk_content from template_to_content where fk_template=? and fk_content=?");
//                statement.setLong(1, sync.getPK());
//                statement.setBinaryStream(2, new ByteArrayInputStream(a.getHash().toBytes()));
//                resultSet = statement.executeQuery();
//                if (!resultSet.isBeforeFirst())
//                {
//                    // There were no matches. Time to insert. Need to determine if the content exists.
//                    safeClose(resultSet);
//                    resultSet = null;
//                    safeClose(statement);
//                    statement = null;
//
//                    statement = this.storage.getConnect().prepareStatement("INSERT INTO template_to_content (fk_template, fk_content) VALUES (?,?)");
//                    statement.setLong(1, sync.getPK());
//                    statement.setBinaryStream(2, new ByteArrayInputStream(a.getHash().toBytes()));
//                    statement.executeUpdate();
//
//                    safeClose(statement);
//                    statement = null;
//                } else
//                {
//                    safeClose(resultSet);
//                    resultSet = null;
//                }
//            }
//        } catch (Exception e)
//        {
//            this.log.error("<internal> Core.syncTemplateRelationships(): Couldn't synchronize template relationships, " + e.getMessage());
//        } finally
//        {
//            safeClose(resultSet);
//            resultSet = null;
//            safeClose(statement);
//            statement = null;
//        }
//    }

    // Not called
//    public void startSyncTestInstance(long pk_test)
//    {
//        // Mark test instances for later cleanup.
//        if (this.storage.isReadOnly())
//            return;
//
//        PreparedStatement statement = null;
//        try
//        {
//            statement = this.storage.getConnect().prepareStatement(String.format("UPDATE test_instance SET synchronized=0 WHERE fk_test=%d", pk_test));
//            statement.executeUpdate();
//        } catch (Exception ignore)
//        {
//            //TODO: handle
//        } finally
//        {
//            safeClose(statement);
//            statement = null;
//        }
//    }

      // Not called
//    public void stopSyncTestInstance(long pk_test)
//    {
//        if (this.storage.isReadOnly())
//            return;
//
//        PreparedStatement statement = null;
//        try
//        {
//            statement = this.storage.getConnect().prepareStatement(String.format("DELETE FROM test_instance WHERE synchronized=0 AND fk_test=%d", pk_test));
//            statement.executeUpdate();
//        } catch (Exception e)
//        {
//            //TODO: handle
//        } finally
//        {
//            safeClose(statement);
//            statement = null;
//        }
//    }

//    private long findTestInstance(TestInstance sync, long pk_test)
//    {
//        PreparedStatement find_test_instance = null;
//        ResultSet test_instances = null;
//        PreparedStatement find_versions = null;
//        ResultSet his_versions = null;
//
//        try
//        {
//            // TODO: Is this next line broken? Table test_instance does not have column fk_template (does have fk_described_template).
//            find_test_instance = this.storage.getConnect().prepareStatement("SELECT pk_test_instance FROM test_instance WHERE fk_template=? AND fk_test=?");
//            find_test_instance.setLong(1, sync.getDescribedTemplate().getPK());
//            find_test_instance.setLong(2, pk_test);
//            test_instances = find_test_instance.executeQuery();
//            while (test_instances.next())
//            {
//                long pk = test_instances.getLong("pk_test_instance");
//
//                // We found a candidate, but need to verify that its version references exactly match.
//                find_versions = this.storage.getConnect().prepareStatement("SELECT fk_version FROM test_instance_to_version WHERE fk_test_instance=?");
//                find_versions.setLong(1, pk);
//                his_versions = find_versions.executeQuery();
//                boolean extras = false;
//
//                Collection<Long> my_versions = new ArrayList<Long>();
//                //                for ( Version v : sync.getVersions() )
//                //                    my_versions.add( v.getPK() );
//                while (his_versions.next() && !extras)
//                {
//                    Long vk = his_versions.getLong("fk_version");
//                    if (!my_versions.contains(vk))
//                        extras = true;
//
//                    my_versions.remove(vk); // Note, this is remove by object, not index.
//                }
//
//                safeClose(his_versions);
//                his_versions = null;
//                safeClose(find_versions);
//                find_versions = null;
//
//                if (extras)
//                    continue;
//
//                if (my_versions.size() == 0)
//                    return pk; // All versions matched.
//
//                // No match, keep searching.
//            }
//        } catch (Exception e)
//        {
//            this.log.error("<internal> Core.findTestInstance() exception msg: " + e);
//
//            // TODO: handle
//        } finally
//        {
//            safeClose(test_instances);
//            test_instances = null;
//            safeClose(find_test_instance);
//            find_test_instance = null;
//            safeClose(his_versions);
//            his_versions = null;
//            safeClose(find_versions);
//            find_versions = null;
//        }
//
//        return 0;
//    }

      // Not called
//    /**
//     * Synchronize the specified test instance belonging to the specified test. The test instance
//     * information itself is verified and the hashes are checked against the loaded information. If
//     * these match then no further work is done.
//     * @param sync The test instance to synchronize.
//     * @param pk_test The test that the instance is related to.
//     * @return The test instance number
//     */
//    public long syncTestInstance(TestInstance sync, long pk_test)
//    {
//        long pk = 0;
//
//        //TODO: Clean up
//        if (this.storage.isReadOnly())
//        {
//            this.log.error("<internal> Core.syncTestInstance(): database is read-only");
//            this.log.error("<internal> Core.syncTestInstance(): ------------------------");
//            //            System.err.println( "Template: " + sync.getDescribedTemplate().getHash().toString() );
//            this.log.error("<internal> Core.syncTestInstance(): Test Instance for Test \" + Long.toString(pk_test)");
//            this.log.error("<internal> Core.syncTestInstance(): Versions:");
//                //            for ( Version v : sync.getVersions() )
//                //                System.err.println( "\t" + v.getComponent() + ", " + v.getVersion() );
//            this.log.error("<internal> Core.syncTestInstance(): ------------------------");
//            return pk;
//        }
//
//        //        sync.getDescribedTemplate().sync();
//        //        sync.getDescription().sync();
//        //        for ( Version v : sync.getVersions() )
//        //            v.sync();
//
//        PreparedStatement statement = null;
//        ResultSet resultSet = null;
//        try
//        {
//            pk = findTestInstance(sync, pk_test);
//
//            if (pk == 0)
//            {
//                // There were no matches. Time to insert. Need to determine if the content exists.
//
//                // Get the component list associated with the test
//                statement = this.storage.getConnect().prepareStatement(String.format("SELECT distinct pk_component" + " FROM component" + " JOIN component_to_test_plan ON component_to_test_plan.fk_component = component.pk_component" + " JOIN test_plan ON test_plan.pk_test_plan = component_to_test_plan.fk_test_plan" + " JOIN test ON test.fk_test_plan = test_plan.pk_test_plan" + " WHERE test.pk_test='%d'", pk_test));
//                resultSet = statement.executeQuery();
//
//                // TODO: See why components here is never read. Is impl incomplete?
//                List<Long> components = new ArrayList<Long>();
//                while (resultSet.next())
//                {
//                    components.add(resultSet.getLong(1));
//                }
//
//                safeClose(resultSet);
//                resultSet = null;
//                safeClose(statement);
//                statement = null;
//
//                statement = this.storage.getConnect().prepareStatement("INSERT INTO test_instance (fk_test, fk_template, fk_description, phase, synchronized) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
//                statement.setLong(1, pk_test);
//                //                statement.setLong(2, sync.getDescribedTemplate().getPK());
//                //                statement.setLong(3, sync.getDescription().getPK());
//                //TODO: Determine the phase
//                statement.setLong(4, 0);
//                statement.setInt(5, 1); // Default is synchronized.
//                statement.executeUpdate();
//
//                try (ResultSet keys = statement.getGeneratedKeys()) {
//                    if (keys.next())
//                        pk = keys.getLong(1);
//                }
//
//                safeClose(statement);
//                statement = null;
//
//                /*                for ( Version v : sync.getVersions() ) {
//                    statement = connect.prepareStatement( String.format( "select fk_test_instance, fk_version from test_instance_to_version where fk_test_instance='%d' and fk_version='%d'",
//                            pk, v.getPK() ) );
//                    resultSet = statement.executeQuery();
//                    if ( ! resultSet.isBeforeFirst() ) {
//                        // There were no matches. Time to insert. Need to determine if the content exists.
//                        safeClose( resultSet ); resultSet = null;
//                        safeClose( statement ); statement = null;
//
//                        boolean primary = components.contains( v.getComponentPK() );
//                        statement = connect.prepareStatement( "insert into test_instance_to_version (fk_test_instance, fk_version, is_primary) values (?,?,?)" );
//                        statement.setLong(1, pk);
//                        statement.setLong(2, v.getPK());
//                        statement.setBoolean(3, primary);
//                        statement.executeUpdate();
//
//                        safeClose( statement ); statement = null;
//                    }
//                    else {
//                        safeClose( resultSet ); resultSet = null;
//                    }
//                } */
//            } else
//            {
//                // TODO: Validate the due date and phase
//                statement = this.storage.getConnect().prepareStatement("UPDATE test_instance SET synchronized=1, fk_description=? WHERE pk_test_instance=?");
//                //                statement.setLong( 1, sync.getDescription().getPK() );
//                statement.setLong(2, pk);
//                statement.executeUpdate();
//            }
//        } catch (Exception e)
//        {
//            this.log.error("<internal> Core.syncTestInstance() exception msg : " + e);
//            // TODO: handle
//        } finally
//        {
//            safeClose(resultSet);
//            resultSet = null;
//            safeClose(statement);
//            statement = null;
//        }
//
//        //        if ( sync.getResult() != null )
//        //            reportResult( sync.getDescribedTemplate().getHash().toString(), sync.getResult() );
//
//        return pk;
//    }

    // Not called
//    // TODO: consider alternative to returning null
////  @Nullable
////  @SuppressWarnings("ReturnOfNull")
//    public Long getInstanceRun(long testInstanceNumber) throws Exception
//    {
//        Statement statement = null;
//        try
//        {
//            statement = this.storage.getConnect().createStatement();
//            ResultSet resultSet = statement.executeQuery("SELECT fk_run FROM test_instance WHERE pk_test_instance = " + testInstanceNumber);
//            if(resultSet.next()){
//
//                long result = resultSet.getLong("fk_run");
//                if(!resultSet.wasNull())
//                    return result;
//            }
//            return null;
//        } catch(Exception e)
//        {
//            this.log.error("<internal> Core.getInstanceRun() exception msg: " + e);
//            throw e;
//        } finally
//        {
//            safeClose(statement);
//        }
//    }

    // TODO: consider alternative to returning null
//  @Nullable
//  @SuppressWarnings("ReturnOfNull")
    Long createInstanceRun(long testInstanceNumber, String owner) throws Exception
    {
        if (this.storage.isReadOnly())
            throw new Exception("Database connection is read only.");

        String hash;
        ResultSet resultSet = null;
        Statement templateStatement = null;
        try
        {
            templateStatement = this.storage.getConnect().createStatement();
            resultSet = templateStatement.executeQuery("SELECT hash FROM described_template JOIN test_instance ON fk_described_template = pk_described_template JOIN template ON fk_template = pk_template WHERE pk_test_instance = " + testInstanceNumber);
            if(resultSet.next()){
                hash = new Hash(resultSet.getBytes("hash")).toString();
            }
            else{
                this.log.error("<internal> Core.createInstanceRun(): Cannot find template for test instance " + testInstanceNumber);
                throw new Exception("Cannot find template for test instance " + testInstanceNumber);
            }
        } catch(Exception e)
        {
            this.log.error("<internal> Core.createInstanceRun() exception msg: " + e);
            throw e;
        } finally
        {
            safeClose(templateStatement);
        }

        PreparedStatement runStatement = null;
        try
        {
            runStatement = this.storage.getConnect().prepareStatement("call add_run(?, ?, ?, ?, ?, ?)");
            runStatement.setString(1, hash);
            runStatement.setNull(2, Types.BOOLEAN);

            if (owner != null)
                runStatement.setString(3, owner);
            else
                runStatement.setNull(3, Types.VARCHAR);

            runStatement.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
            runStatement.setNull(5, Types.TIMESTAMP);
            runStatement.setNull(6, Types.TIMESTAMP);
            if(runStatement.execute()){
                // we have a ResultSet
                resultSet = runStatement.getResultSet();
                boolean result = resultSet.next();
                if(result){
                    return resultSet.getLong("pk_run");
                }
            }
            else
                return null; // no ResultSet
        } catch (Exception e)
        {
            this.log.error("<internal> Core.createInstanceRun() exception msg: " + e);
            // TODO: handle
            throw e;
        } finally
        {
            safeClose(runStatement);
            runStatement = null;
        }
        throw new Exception("Failed to add new run for test instance " + testInstanceNumber);
    }

//    public static class TestTopLevelRelationships
//    {
//        public static void main(String[] args)
//        {
//            Core core = new Core(0);
//            core.syncTopTemplateRelationships();
//        }
//    }
//
//    public static class TestPruneTemplates
//    {
//        public static void main(String[] args)
//        {
//            Core core = new Core(0);
//            core.pruneTemplates();
//        }
//    }
}
