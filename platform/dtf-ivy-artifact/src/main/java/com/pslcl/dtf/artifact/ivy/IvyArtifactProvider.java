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
package com.pslcl.dtf.artifact.ivy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.ExtraInfoHolder;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.AbstractPatternsBasedResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.FileUtil;

import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.artifact.ArtifactProvider;
import com.pslcl.dtf.core.artifact.Content;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.template.Template;

public class IvyArtifactProvider implements ArtifactProvider
{
    private static class IvyModule implements Module
    {
        private ResolvedModuleRevision rmv;

        public IvyModule(ResolvedModuleRevision rmv)
        {
            this.rmv = rmv;
        }

        @Override
        public String getOrganization()
        {
            return rmv.getDescriptor().getModuleRevisionId().getOrganisation();
        }

        @Override
        public String getName()
        {
            return rmv.getDescriptor().getModuleRevisionId().getName();
        }

        @Override
        public String getVersion()
        {
            return rmv.getDescriptor().getModuleRevisionId().getRevision();
        }

        @Override
        public String getStatus()
        {
            return rmv.getDescriptor().getStatus();
        }

        @Override
        public Map<String, String> getAttributes()
        {
            @SuppressWarnings("unchecked")
            Map<String, String> map = rmv.getDescriptor().getExtraAttributes();
            return map;
        }

        private static final SimpleDateFormat format = new SimpleDateFormat("YYYYMMddHHmmss");

        @Override
        public String getSequence()
        {
            return format.format(rmv.getPublicationDate());
        }

        @Override
        public List<Artifact> getArtifacts()
        {
            List<Artifact> result = new ArrayList<Artifact>();

            org.apache.ivy.core.module.descriptor.Artifact[] al = rmv.getDescriptor().getAllArtifacts();
            if (al != null)
            {
                for (org.apache.ivy.core.module.descriptor.Artifact a : al)
                {
                    for (String configuration : a.getConfigurations())
                    {
                        Artifact A = new IvyArtifact(this, rmv, configuration, a);
                        result.add(A);
                    }
                }
            }

            return result;
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern)
        {
            List<Artifact> full = getArtifacts();
            List<Artifact> result = new ArrayList<Artifact>();
            Pattern p = Pattern.compile(namePattern);

            for (Artifact A : full)
            {
                Matcher m = p.matcher(A.getName());
                if (m.matches())
                    result.add(A);
            }

            return result;
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern, String configuration)
        {
            List<Artifact> full = getArtifacts(namePattern);
            List<Artifact> result = new ArrayList<Artifact>();

            for (Artifact A : full)
            {
                if (configuration.equals(A.getConfiguration()))
                    result.add(A);
            }

            return result;
        }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((rmv == null) ? 0 : rmv.hashCode());
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
			IvyModule other = (IvyModule) obj;
			if (rmv == null) {
				if (other.rmv != null)
					return false;
			} else if (!rmv.equals(other.rmv))
				return false;
			return true;
		}
    }

    private static class IvyArtifact implements Artifact
    {
        private IvyModule module;
        private String configuration;
        private String name;
        private IvyContent content;
        private String targetFilePath;

        public IvyArtifact(IvyModule module, ResolvedModuleRevision rmv, String configuration, org.apache.ivy.core.module.descriptor.Artifact artifact)
        {
            this.module = module;
            this.configuration = configuration;
            this.content = new IvyContent(rmv, artifact);

            this.name = artifact.getName() + "." + artifact.getExt();
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
            return content;
        }

        @Override
        public int getPosixMode()
        {
            return 0b100_100_100;
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
			result = prime * result + ((content == null) ? 0 : content.hashCode());
			result = prime * result + ((module == null) ? 0 : module.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			IvyArtifact other = (IvyArtifact) obj;
			if (configuration == null) {
				if (other.configuration != null)
					return false;
			} else if (!configuration.equals(other.configuration))
				return false;
			if (content == null) {
				if (other.content != null)
					return false;
			} else if (!content.equals(other.content))
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
			if (targetFilePath == null) {
				if (other.targetFilePath != null)
					return false;
			} else if (!targetFilePath.equals(other.targetFilePath))
				return false;
			return true;
		}		
    }

    private static class IvyContent implements Content
    {
        private File file = null;
        private Hash hash = null;
        private boolean downloaded = false;
        private ResolvedModuleRevision rmv;
        private org.apache.ivy.core.module.descriptor.Artifact artifact;

        public IvyContent(ResolvedModuleRevision rmv, org.apache.ivy.core.module.descriptor.Artifact artifact)
        {
            this.rmv = rmv;
            this.artifact = artifact;
        }

        private void download()
        {
            if (downloaded)
                return;

            DownloadOptions options = new DownloadOptions();
            DownloadReport report = rmv.getArtifactResolver().download(new org.apache.ivy.core.module.descriptor.Artifact[] { artifact }, options);
            ArtifactDownloadReport areport = report.getArtifactReport(artifact);
            file = areport.getLocalFile();
            hash = Hash.fromContent(file);
            downloaded = true;
        }

        @Override
        public Hash getHash()
        {
            try
            {
                download();
                return hash;
            } catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public InputStream asStream()
        {
            try
            {
                download();
                return new FileInputStream(file);
            } catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public byte[] asBytes()
        {
            try
            {
                download();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileInputStream fis = new FileInputStream(file);

                IOUtils.copy(fis, os);
                fis.close();
                os.close();

                return os.toByteArray();
            } catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public String getValue(Template template) throws Exception
        {
            return getHash().toString();
        }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
			result = prime * result + (downloaded ? 1231 : 1237);
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + ((hash == null) ? 0 : hash.hashCode());
			result = prime * result + ((rmv == null) ? 0 : rmv.hashCode());
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
			IvyContent other = (IvyContent) obj;
			if (artifact == null) {
				if (other.artifact != null)
					return false;
			} else if (!artifact.equals(other.artifact))
				return false;
			if (downloaded != other.downloaded)
				return false;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			if (hash == null) {
				if (other.hash != null)
					return false;
			} else if (!hash.equals(other.hash))
				return false;
			if (rmv == null) {
				if (other.rmv != null)
					return false;
			} else if (!rmv.equals(other.rmv))
				return false;
			return true;
		}
    }

    /**
     * Extract the standard dtf-merge-info information.
     * @param module The module to check.
     * @return A string representing everything to merge to.
     */
    private static String extractMergeTo(ModuleDescriptor module)
    {
        String result = "";
        Map<String, String> ns = module.getExtraAttributesNamespaces();
        String prefix = "";
        for (Map.Entry<String, String> entry : ns.entrySet())
        {
            if (entry.getValue().equals("http://com.pslcl/dtf-ivy"))
            {
                prefix = entry.getKey();
                break;
            }
        }

        String sep = "";
        for (ExtraInfoHolder extra : module.getExtraInfos())
        {
            if (extra.getName().equals(prefix + ":" + "dtf-merge-info"))
            {
                Map<String, String> attributes = extra.getAttributes();
                for (Map.Entry<String, String> attribute : attributes.entrySet())
                {
                    if (attribute.getKey().equals(prefix + ":merge-to"))
                    {
                        result += sep + attribute.getValue();
                        sep = "&";
                    }
                }
            }
        }

        return result;
    }

    /**
     * This is the ivy object that is initialized in init() and remains active until close().
     * TBD: It is unclear if the ivy class is thread-safe. 
     */
    private Ivy ivy = null;

    /**
     * Given an ivy class, resolver, module (org/mod/ver), criteria and additional optional parameters, determine if there are additional
     * tokens that can be resolved and resolve them. This is recursive, resulting in the discovery of additional ivy files.
     * @param ivy The ivy class to use.
     * @param pbr The patterns resolver to search.
     * @param org The organization.
     * @param module The module.
     * @param version The version
     * @param criteria Criteria that have already been determined to apply.
     * @param optional If the current search parameter is optional then the name is passed here.
     * @param attr A list of attribute sets that have been searched for to prevent duplicate searches.
     * @param moduleNotifier The callback to notify modules to.
     */
    private void scanOptions(Ivy ivy, AbstractPatternsBasedResolver pbr, String org, String module, String version, Map<String, String> criteria, String optional, List<String> attrs, ModuleNotifier moduleNotifier)
    {
        // Get all the ivy patterns from the resolver. We will search them all for optional fields.
        @SuppressWarnings("unchecked")
        List<String> ivys = pbr.getIvyPatterns();
        for (String pattern : ivys)
        {
            // Substitute each token in the criteria map. We must use this routine because the others remove optional token references.
            for (Map.Entry<String, String> entry : criteria.entrySet())
                pattern = IvyPatternHelper.substituteToken(pattern, entry.getKey(), entry.getValue());

            // Get the first token. This will be the next token to search.
            String token = IvyPatternHelper.getFirstToken(pattern);
            if (token == null)
            {
                // There is no token, so the set of criteria fully define a search. If the current token is
                // optional then remove it. The recursion will handle searching both with and without the
                // optional parameter.
                if (optional != null)
                    criteria.remove(optional);

                // We need only additional parameters for the call to findModule(), so remove the standard ones.
                Map<String, String> find = new HashMap<String, String>(criteria);
                find.remove(IvyPatternHelper.ORGANISATION_KEY);
                find.remove(IvyPatternHelper.MODULE_KEY);
                find.remove(IvyPatternHelper.REVISION_KEY);
                ModuleRevisionId mrid = ModuleRevisionId.newInstance(org, module, version, find);

                // The non-recursive call handles searching without additional parameters.
                if (find.size() == 0)
                    return;

                Attributes attr = new Attributes(find);
                String attrString = attr.toString();
                if (attrs.contains(attrString))
                    return;

                attrs.add(attrString);

                // Search for a matching ivy module.
                ResolvedModuleRevision rmv = ivy.findModule(mrid);
                if (rmv != null)
                {
                    // Notify the caller of a module.
                    moduleNotifier.module(this, new IvyModule(rmv), extractMergeTo(rmv.getDescriptor()));
                }

                return;
            }

            Map<String, String> next_criteria = new HashMap<String, String>(criteria);

            String tokenStr = "[" + token + "]";
            boolean opt = pattern.indexOf('(') < pattern.indexOf(tokenStr);
            next_criteria.put(token, "[" + token + "]");
            @SuppressWarnings("unchecked")
            Map<String, String>[] tokenList = pbr.listTokenValues(new String[] { token }, next_criteria);
            for (int i = 0; i < tokenList.length; i++)
            {
                String value = tokenList[i].get(token);
                next_criteria.put(token, value);
                scanOptions(ivy, pbr, org, module, version, next_criteria, null, attrs, moduleNotifier);
            }

            if (opt)
            {
                // This handles scanning for the token value when it does not exist.
                next_criteria.put(token, "");
                scanOptions(ivy, pbr, org, module, version, next_criteria, token, attrs, moduleNotifier);
            }
        }
    }

    @Override
    public void init() throws Exception
    {
        IvySettings ivySettings = new IvySettings();
        ivySettings.setBaseDir(Paths.get("portal/config/").toFile());

        ivy = Ivy.newInstance(ivySettings);

        File ivySettingsXmlFile = new File("portal/config/ivysettings.xml");
        if (!ivySettingsXmlFile.exists())
            throw new Exception("portal/config/ivysettings.xml file does not exist.");

        ivy.configure(ivySettingsXmlFile);

        // Clear out the download cache to force new downloads.
        FileUtil.forceDelete(ivy.getSettings().getDefaultCache());
    }

    @Override
    public void iterateModules(ModuleNotifier moduleNotifier) throws Exception
    {
        if (ivy == null)
            throw new IllegalStateException("init() must be called");

        try
        {
            // Start by determining all of the organizations from the ivy patterns.
            OrganisationEntry[] oes = ivy.listOrganisationEntries();
            List<String> orgs = new ArrayList<String>();
            for (OrganisationEntry oe : oes)
                if (!orgs.contains(oe.getOrganisation()))
                    orgs.add(oe.getOrganisation());

            orgs.sort(String.CASE_INSENSITIVE_ORDER);
            for (String org : orgs)
            {
                // Determine the modules from the ivy patterns.
                OrganisationEntry oe = new OrganisationEntry(null, org);

                ModuleEntry[] mes = ivy.listModuleEntries(oe);
                List<String> modules = new ArrayList<String>();
                for (ModuleEntry me : mes)
                    if (!modules.contains(me.getModule()))
                        modules.add(me.getModule());

                modules.sort(String.CASE_INSENSITIVE_ORDER);
                for (String module : modules)
                {
                    // Determine the versions from the ivy patterns.
                    ModuleEntry me = new ModuleEntry(oe, module);
                    RevisionEntry[] res = ivy.listRevisionEntries(me);
                    List<String> versions = new ArrayList<String>();
                    for (RevisionEntry re : res)
                        if (!versions.contains(re.getRevision()))
                            versions.add(re.getRevision());

                    versions.sort(String.CASE_INSENSITIVE_ORDER);
                    for (String version : versions)
                    {
                        /* For this org/mod/ver determine if there are other parameters in the ivy repository format.
                         * This is only possible for AbstractPatternsBasedResolvers.
                         * Since the values for each additional parameter must be passed in to determine the artifacts,
                         * we have to build a map of parameter -> [value list], and for each value in the list
                         * we have to determine if there are additional parameters. This has to be recursive, since
                         * the pattern can contain several additional parameters.
                         */
                        List<String> attrs = new ArrayList<String>();
                        for (@SuppressWarnings("rawtypes")
                        Iterator iter = ivy.getSettings().getResolvers().iterator(); iter.hasNext();)
                        {
                            DependencyResolver resolver = (DependencyResolver) iter.next();

                            if (resolver.getName().equals("all"))
                                continue;

                            if (AbstractPatternsBasedResolver.class.isAssignableFrom(resolver.getClass()))
                            {
                                AbstractPatternsBasedResolver pbr = (AbstractPatternsBasedResolver) resolver;
                                Map<String, String> criteria = new HashMap<String, String>();
                                criteria.put(IvyPatternHelper.ORGANISATION_KEY, org);
                                criteria.put(IvyPatternHelper.MODULE_KEY, module);
                                criteria.put(IvyPatternHelper.REVISION_KEY, version);

                                scanOptions(ivy, pbr, org, module, version, criteria, null, attrs, moduleNotifier);
                            }
                        }

                        {
                            ModuleRevisionId mrid = new ModuleRevisionId(new ModuleId(org, module), version);
                            ResolvedModuleRevision rmv;
                            try{
                            	rmv = ivy.findModule(mrid);
                            } catch(Exception e){
                            	System.err.println("Failed to find module: " + org + "#" + module + ";" + version);
                            	e.printStackTrace();
                            	continue;
                            }
                            if (rmv != null)
                                moduleNotifier.module(this, new IvyModule(rmv), extractMergeTo(rmv.getDescriptor()));
                            else
                                System.err.println("Failed to find module: " + org + "#" + module + ";" + version);
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            System.err.println("ERROR: Exception during module iteration, " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        ivy = null;
    }

    @Override
    public boolean merge(String merge, Module module, Module target)
    {
        String[] matches = merge.split("&");
        for (String match : matches)
        {
            String[] f = match.split("#");
            String match_organization = f[0];
            String[] f2 = f[1].split(";");
            String match_module = f2[0];
            String match_version = f2[1];

            if (!match_organization.equals(target.getOrganization()))
                continue;

            if (!match_module.equals(target.getName()))
                continue;

            if(match_version.contains("*")){
            	if (!target.getVersion().startsWith(match_version.substring(0, match_version.indexOf("*")))){
            		continue;
            	}
            } else{
            	if (!target.getVersion().equals(match_version))
            		continue;
            }

            return true;
        }

        return false;
    }

}
