package com.pslcl.qa.runner.process;

import java.util.concurrent.ConcurrentHashMap;

public class ActionStore {
   
    /**
     *  Used for pure storage, order of element access is determined by other means.
     *  Neither Long key nor InstanceState value may be null.
     *  Writes by any one thread locks only the relevant element, other threads freely access other elements
     */
    ConcurrentHashMap<Long, DescribedTemplateState> map; 
    
    public ActionStore() {
        map = new ConcurrentHashMap<>();
    }
    
    /**
     * 
     * @note put to an existing Long key overwrites previous the previously held InstanceState
     * @param iNum
     * @param iState
     * @return Previously stored InstanceState when put overwrites it, null otherwise. 
     */
    DescribedTemplateState put(long iNum, DescribedTemplateState iState) {
        return map.put(Long.valueOf(iNum), iState);
    }
    
    /**
     * 
     * @param iNum
     * @return
     */
    DescribedTemplateState get(long iNum) {
        return map.get(Long.valueOf(iNum));
    }
    
    /**
     * 
     * @note for iNum not found, nothing happens
     * @param iNum
     * @return The removed InstanceState
     */
    DescribedTemplateState remove(long iNum) {
        return map.remove(Long.valueOf(iNum));
    }

}