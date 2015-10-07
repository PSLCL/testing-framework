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
package com.pslcl.qa.platform.generator;

/**
 * This interface represents providers of artifacts. Artifact providers will return sets of Modules. Modules
 * follow the definition established by many common repository projects (like Maven and Ivy). Modules have associated Artifacts.
 * 
 * It is assumed that the set of Modules may change over time, but that the set of Artifacts associated with a Module
 * will not change. If the test platform already knows of a Module then it assumes that its Artifacts are consistent.
 * 
 * There is special meaning to artifacts with a configuration of dtf_test_generator. These artifacts are treated normally, but
 * are also cached separately and with their normal filenames for use by generator execution.
 * 
 * Finally, there is a special capability to merge an module's artifacts into another set of modules. This is indicated by
 * a flag passed along with the module. When set, and after all other modules have been loaded, then the provider will be
 * asked whether or not each combination of modules should be merged. If the result is yes then the merged module's artifacts
 * are hidden (they will not be found with artifact searches) and copied into the other module. Further, merged modules are
 * replaced (with their merged artifacts removed) when another module with the same version information (except the sequence)
 * is found. This allows for testing artifacts to be updated on existing (even released) modules.
 */
public interface ArtifactProvider {
    /**
     * THis interface represents an asynchronous notification that is called for each module.
     */
    public interface ModuleNotifier {
        /**
         * Notify the implementer that a module exists. The implementation must be thread-safe and
         * handle multiple simultaneous notifications. The module's lifecycle should not exceed that
         * of the call to {@link ArtifactProvider#close()}.
         * @param source The artifact provider that is providing the module.
         * @param module The module that exists.
         * @param merge Either null, or a definition of how the module should be merged. This information will be passed back
         * to {@link ArtifactProvider#merge}.
         */
        void module( ArtifactProvider source, Module module, String merge );
    }
    
    /**
     * Initialize the artifact provider. This should be paired with a call to {@link #close()}. The same instance may be reused if {@link #close()}
     * is called and then followed by another call to {@link #init()}. The provider may maintain state between calls to {@link #open()} and {@link #close()}.
     * @throws Exception Thrown if the artifact provider cannot be initialized. This can indicate either permanent or temporary failures.
     */
    void init() throws Exception;

    /**
     * Iterate through all the modules known to the provider. This routine will not return until all known modules have been
     * passed to the callback. If there is an error then an Exception is thrown. In that case it should be assumed that only
     * a partial set of modules was iterated.
     * @param moduleNotifier The object that the modules should be notified to.
     * @throws Exception Thrown if there is a problem iterating the modules.
     */
    void iterateModules( ModuleNotifier moduleNotifier ) throws Exception;

    /**
     * Determine whether a module's artifacts should be merged into another. This call is triggered
     * by passing a non-null merge parameter in {@link ModuleNotifier#module(Module, String)} call. If set,
     * and after all other modules have been learned, then this call is made to determine if the module should
     * be merged. If the result is true then the test harness will do the merge.
     * @param merge The parameter passed out to the test harness during module discovery.
     * @param module The module that will be merged. This module will have been defined by the provider.
     * @param target The target module. This module may come from an ArtifactProvider.
     * @return True if the module should be merged into the target. False if it should not.
     */
    boolean merge( String merge, Module module, Module target );
    
    /**
     * Close an artifact provider. The same instance may be reused if {@link #init()} is called again.
     */
    void close();
}
