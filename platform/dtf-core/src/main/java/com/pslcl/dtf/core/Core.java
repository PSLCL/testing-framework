/*
 * Copyright (c) 2010-2017, Panasonic Corporation.
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        public Hash getHash() {
            return hash;
        }

        @Override
        public String getValue(Template template) {
            return getHash().toString();
        }

        @Override
        // TODO: consider acceptable solution to replace null return
//      @Nullable
//      @SuppressWarnings("ReturnOfNull")
        public InputStream asStream() {
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

    /**
     * Check that an existing template is correct. If the template exists then the children
     * may also exist, but their documentation (of template steps) may be out of date, so update that.
     * @param dt The described template to check. Results are not currently checked.
     * @return DBDescribedTemplate
     */
    private Core.DBDescribedTemplate check(DescribedTemplate dt) throws Exception {
        // note: dt.getDependencies() is currently always empty
        for (DescribedTemplate child : dt.getDependencies()) {
            // Recursively check all dependent DescribedTemplate's.
            // Original TODO: Figure out if this is correct. Has not been tested, since .getDependencies() is empty.
            Optional<Core.DBDescribedTemplate> dbdtAsStored;
            DescribedTemplate.Key matchKey = child.getKey();
            try {
                dbdtAsStored = this.getStorage().getDBDescribedTemplate(matchKey);
            } catch (SQLException sqle) {
                this.log.error("<internal> Core.check() sees exception from .getDBDescribedTemplate(), msg: " + sqle);
                this.log.debug("stack trace: ", sqle);
                throw new Exception(".check() exits with exception ", sqle);
            }

            if (!dbdtAsStored.isPresent())
                throw new Exception("Parent template exists, child does not.");
            /*DBDescribedTemplate dbdt =*/ this.check(child); // recursion
        } // end for()

        Optional<Core.DBDescribedTemplate> wrappedMe;
        DescribedTemplate.Key matchKey = dt.getKey();
        try {
            wrappedMe = this.getStorage().getDBDescribedTemplate(matchKey);
        } catch (SQLException sqle) {
            this.log.error("<internal> Core.check() sees exception from .getDBDescribedTemplate(), msg: " + sqle);
            this.log.debug("stack trace: ", sqle);
            throw new Exception(".check() exits with exception ", sqle);
        }

        if (wrappedMe.isPresent()) {
            Core.DBDescribedTemplate me = wrappedMe.get();
            if (!dt.getDocumentationHash().equals(me.documentationHash)) {
                // Recreate documentation of template steps.
                this.getStorage().updateDocumentation(me.pk, dt);
            }
            return me;
        }
        throw new Exception("Request to check a non-existent described template.");
    }

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
                this.check(child);
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
                dbdt = this.check(ti.getDescribedTemplate());
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
                if (ti.getResult()!=null || ti.getOwner()!=null) {
                    try {
                        this.storage.updateRunResult(ti);
                    } catch (SQLException sqle) {
                        this.log.error("Core.syncDescribedTemplate() Continues after .updateRunResult() throws exception, msg: " + sqle);
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

}
