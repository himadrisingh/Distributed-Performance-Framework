package org.tc.perf.work.items;

import static org.tc.perf.util.SharedConstants.TC_INSTALL_DIR;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.tc.perf.util.Configuration;

public abstract class AbstractL2WorkItem extends AbstractWork {

	private static final long serialVersionUID = 1L;
	
	protected static final Logger log = Logger.getLogger(AbstractL2WorkItem.class);

    public AbstractL2WorkItem(Configuration configuration) {
    	super(configuration);
	}
    
    protected String getTcInstallDir(){
        Element e = getTestCache().get(TC_INSTALL_DIR);
        if (e == null) {
            log.error("Kit not setup.");
            return "";
        }
        return (String) e.getValue();
    }
    
    protected void setTcInstallDir(String installDir){
        getTestCache().put(new Element(TC_INSTALL_DIR, installDir));
    }
}
