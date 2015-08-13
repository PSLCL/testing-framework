package com.pslcl.qa.platform.generator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.utils.IOUtils;
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

import com.pslcl.qa.platform.Hash;

public class IvyArtifactProvider implements ArtifactProvider {
    private static class IvyModule implements Module {
        private ResolvedModuleRevision rmv;
        
        public IvyModule( ResolvedModuleRevision rmv ) {
            this.rmv = rmv;
        }
        
        @Override
        public String getOrganization() {
            return rmv.getDescriptor().getModuleRevisionId().getOrganisation();
        }

        @Override
        public String getName() {
            return rmv.getDescriptor().getModuleRevisionId().getName();
        }

        @Override
        public String getVersion() {
            return rmv.getDescriptor().getModuleRevisionId().getRevision();
        }

        @Override
        public Map<String, String> getAttributes() {
            @SuppressWarnings("unchecked")
            Map<String,String> map = rmv.getDescriptor().getExtraAttributes();
            return map;
        }

        private static final SimpleDateFormat format = new SimpleDateFormat( "YYYYMMddHHmmss" );
        @Override
        public String getSequence() {
            return format.format( rmv.getPublicationDate() );
        }

        @Override
        public List<Artifact> getArtifacts() {
            List<Artifact> result = new ArrayList<Artifact>();
            
            org.apache.ivy.core.module.descriptor.Artifact[] al = rmv.getDescriptor().getAllArtifacts();
            if ( al != null ) {
                for ( org.apache.ivy.core.module.descriptor.Artifact a : al ) {
                    for ( String configuration : a.getConfigurations() ) {
                        Artifact A = new IvyArtifact( this, rmv, configuration, a );
                        result.add( A );
                    }
                }
            }
            
            return result;
        }

        @Override
        public List<Artifact> getArtifacts( String namePattern ) {
            List<Artifact> full = getArtifacts();
            List<Artifact> result = new ArrayList<Artifact>();
            Pattern p = Pattern.compile( namePattern );
            
            for ( Artifact A : full ) {
                Matcher m = p.matcher( A.getName() );
                if ( m.matches() )
                    result.add( A );
            }
            
            return result;
        }

        @Override
        public List<Artifact> getArtifacts( String namePattern, String configuration) {
            List<Artifact> full = getArtifacts( namePattern );
            List<Artifact> result = new ArrayList<Artifact>();
            
            for ( Artifact A : full ) {
                if ( configuration.equals( A.getConfiguration() ) )
                    result.add( A );
            }
            
            return result;
        }    
    }
    
    private static class IvyArtifact implements Artifact {
        private IvyModule module;
        private String configuration;
        private String name;
        private IvyContent content;
        
        public IvyArtifact( IvyModule module, ResolvedModuleRevision rmv, String configuration, org.apache.ivy.core.module.descriptor.Artifact artifact ) {
            this.module = module;
            this.configuration = configuration;
            this.content = new IvyContent( rmv, artifact );
            
            this.name = artifact.getName() + "." + artifact.getExt();
        }
        
        @Override
        public Module getModule() {
            return module;
        }
        
        @Override
        public String getConfiguration() {
            return configuration;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public Content getContent() {
            return content;
        }
    }
    
    private static class IvyContent implements Content {
        private File file = null;
        private Hash hash = null;
        private boolean downloaded = false;
        private ResolvedModuleRevision rmv;
        private org.apache.ivy.core.module.descriptor.Artifact artifact;
        
        public IvyContent( ResolvedModuleRevision rmv, org.apache.ivy.core.module.descriptor.Artifact artifact ) {
            this.rmv = rmv;
            this.artifact = artifact;
        }
        
        private void download() {
            if ( downloaded )
                return;
            
            DownloadOptions options = new DownloadOptions();
            DownloadReport report = rmv.getArtifactResolver().download(new org.apache.ivy.core.module.descriptor.Artifact[] { artifact }, options);
            ArtifactDownloadReport areport = report.getArtifactReport(artifact);
            file = areport.getLocalFile();
            hash = Hash.fromContent( file );
            downloaded = true;
        }
        
        @Override
        public Hash getHash() {
            try {
                download();
                return hash;
            }
            catch ( Exception e ) {
                return null;
            }
        }

        @Override
        public InputStream asStream() {
            try {
                download();
                return new FileInputStream(file);
            }
            catch ( Exception e ) {
                return null;
            }
        }

        @Override
        public byte[] asBytes() {
            try {
                download();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileInputStream fis = new FileInputStream( file );

                IOUtils.copy( fis, os );
                fis.close();
                os.close();

                return os.toByteArray();
            }
            catch ( Exception e ) {
                return null;
            }
        }

        @Override
        public String getValue(Template template) throws Exception {
            return getHash().toString();
        }      
    }
    
    /**
     * Extract the standard dtf-merge-info information.
     * @param module The module to check.
     * @return A string representing everything to merge to.
     */
    private static String extractMergeTo( ModuleDescriptor module ) {
        String result = "";
        Map<String,String> ns = module.getExtraAttributesNamespaces();
        String prefix = "";
        for ( Map.Entry<String,String> entry : ns.entrySet() ) {
            if ( entry.getValue().equals( "http://com.pslcl/dtf-ivy" ) ) {
                prefix = entry.getKey();
                break;
            }
        }
        
        String sep = "";
        for ( ExtraInfoHolder extra : module.getExtraInfos() ) {
            if ( extra.getName().equals( prefix + ":" + "dtf-merge-info" ) ) {
                Map<String,String> attributes = extra.getAttributes();
                for ( Map.Entry<String,String> attribute : attributes.entrySet() ) {
                    if ( attribute.getKey().equals( prefix + ":merge-to") ) {
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
     * Given an ivy class, resolver, revision, criteria and additional optional parameters, determine if there are additional
     * tokens that can be resolved and resolve them. This is recursive, resulting in the discovery of additional ivy files.
     * @param ivy The ivy class to use.
     * @param pbr The patterns resolver to search.
     * @param ve The org/mod/ver that we have already discovered, but that may have additional options.
     * @param criteria Criteria that have already been determined to apply.
     * @param optional If the current search parameter is optional then the name is passed here.
     * @param moduleNotifier The callback to notify modules to.
     */
    private void scanOptions( Ivy ivy, AbstractPatternsBasedResolver pbr, RevisionEntry ve, Map<String,String> criteria, String optional, ModuleNotifier moduleNotifier ) {
        // Get all the ivy patterns from the resolver. We will search them all for optional fields.
        @SuppressWarnings("unchecked")
        List<String> ivys = pbr.getIvyPatterns();
        for ( String pattern : ivys ) {
            // Substitute each token in the criteria map. We must use this routine because the others remove optional token references.
            for ( Map.Entry<String,String> entry : criteria.entrySet() )
                pattern = IvyPatternHelper.substituteToken( pattern, entry.getKey(), entry.getValue() );
            
            // Get the first token. This will be the next token to search.
            String token = IvyPatternHelper.getFirstToken(pattern);
            if ( token == null ) {
                // There is no token, so the set of criteria fully define a search. If the current token is
                // optional then remove it. The recursion will handle searching both with and without the
                // optional parameter.
                if ( optional != null )
                    criteria.remove( optional );
                
                // We need only additional parameters for the call to findModule(), so remove the standard ones.
                Map<String,String> find = new HashMap<String,String>( criteria );
                find.remove(IvyPatternHelper.ORGANISATION_KEY);
                find.remove(IvyPatternHelper.MODULE_KEY);
                find.remove(IvyPatternHelper.REVISION_KEY);
                ModuleRevisionId mrid = ModuleRevisionId.newInstance(ve.getOrganisation(), ve.getModule(), ve.getRevision(), find);
                
                // Search for a matching ivy module.
                ResolvedModuleRevision rmv = ivy.findModule(mrid);
                if ( rmv != null ) {     
                    // Notify the caller of a module.
                    moduleNotifier.module( this, new IvyModule(rmv), extractMergeTo( rmv.getDescriptor() ) );
                }

                return;
            }
            
            Map<String,String> next_criteria = new HashMap<String,String>( criteria );

            String tokenStr = "[" + token + "]";
            boolean opt = pattern.indexOf('(') < pattern.indexOf( tokenStr );
            next_criteria.put( token, "[" + token + "]" );
            @SuppressWarnings("unchecked")
            Map<String,String>[] tokenList = ve.getResolver().listTokenValues(new String[] {token}, next_criteria);
            for (int i = 0; i < tokenList.length; i++) {
                String value = (String) tokenList[i].get(token);
                next_criteria.put( token, value );
                scanOptions( ivy, pbr, ve, next_criteria, null, moduleNotifier );
            }
            
            if ( opt ) {
                // This handles scanning for the token value when it does not exist.
                next_criteria.put( token, "" );
                scanOptions( ivy, pbr, ve, next_criteria, token, moduleNotifier );
            }
        }
    }

    @Override
    public void init() throws Exception {
        IvySettings ivySettings = new IvySettings();
        ivySettings.setBaseDir( Paths.get("ivy/").toFile() );
        
        ivy = Ivy.newInstance(ivySettings);
        
        File ivySettingsXmlFile = new File("ivy/ivysettings.xml");
        if ( ! ivySettingsXmlFile.exists() )
            throw new Exception( "ivy/ivysettings.xml file does not exist." );
        
        ivy.configure(ivySettingsXmlFile);
    }

    @Override
    public void iterateModules(ModuleNotifier moduleNotifier) throws Exception {
        if ( ivy == null )
            throw new IllegalStateException("init() must be called");
        
        // Start by determining all of the organizations from the ivy patterns.
        OrganisationEntry[] orgs = ivy.listOrganisationEntries();
        for ( OrganisationEntry oe : orgs ) {
            // Determine the modules from the ivy patterns.
            ModuleEntry[] modules = ivy.listModuleEntries( oe );
            for ( ModuleEntry me : modules ) {
                // Determine the versions from the ivy patterns.
                RevisionEntry[] versions = ivy.listRevisionEntries( me );
                for ( RevisionEntry ve : versions ) {
                    /* For this org/mod/ver determine if there are other parameters in the ivy repository format.
                     * This is only possible for AbstractPatternsBasedResolvers.
                     * Since the values for each additional parameter must be passed in to determine the artifacts,
                     * we have to build a map of parameter -> [value list], and for each value in the list
                     * we have to determine if there are additional parameters. This has to be recursive, since
                     * the pattern can contain several additional parameters.
                     */
                    DependencyResolver R = ve.getResolver();
                    if ( AbstractPatternsBasedResolver.class.isAssignableFrom( R.getClass() ))  {
                        AbstractPatternsBasedResolver pbr = (AbstractPatternsBasedResolver) R;
                        Map<String,String> criteria = new HashMap<String,String>();
                        criteria.put( IvyPatternHelper.ORGANISATION_KEY, ve.getOrganisation() );
                        criteria.put( IvyPatternHelper.MODULE_KEY, ve.getModule() );
                        criteria.put( IvyPatternHelper.REVISION_KEY, ve.getRevision() );

                        scanOptions( ivy, pbr, ve, criteria, null, moduleNotifier );
                    }
                    else {
                        ModuleRevisionId mrid = new ModuleRevisionId(new ModuleId(oe.getOrganisation(),me.getModule()), ve.getRevision() );
                        
                        ResolvedModuleRevision rmv = ivy.findModule(mrid);
                        moduleNotifier.module( this, new IvyModule( rmv ), extractMergeTo( rmv.getDescriptor() ) );
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        ivy = null;
    }

    @Override
    public boolean merge(String merge, Module module, Module target) {
        String[] matches = merge.split("&");
        for ( String match : matches ) {
            String[] f = match.split("#");
            String match_organization = f[0];
            String[] f2 = f[1].split(";");
            String match_module = f2[0];
            String match_version = f2[1];
            
            if ( ! match_organization.equals( target.getOrganization() ) )
                continue;
            
            if ( ! match_module.equals( target.getName() ) )
                continue;
            
            if ( ! target.getVersion().startsWith( match_version ) )
                continue;
            
            return true;
        }

        return false;
    }

}
