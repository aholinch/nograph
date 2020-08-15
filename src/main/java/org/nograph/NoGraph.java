/* 

Copyright 2020 aholinch

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package org.nograph;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.nograph.impl.GenericNode;
import org.nograph.impl.GenericRelationship;
import org.nograph.impl.LuceneGraphManager;

/**
 * A singleton with key features for the API.
 *  
 * @author aholinch
 *
 */
public class NoGraph 
{
	private static final Logger logger = Logger.getLogger(NoGraph.class.getName());
	
	private static final String sync = "mutex";
	
	private static NoGraph instance = null;
	
	protected NoGraphConfig config = null;
	
    private NoGraph()
    {
    	config = new NoGraphConfig();
    }
    
    public static NoGraph getInstance()
    {
    	if(instance == null)
    	{
    		synchronized(sync)
    		{
    			// yes, check again
    			if(instance == null)
    			{
    				instance = new NoGraph();
    			}
    		}
    	}
    	
    	return instance;
    }
    
    public Node newNode(String type)
    {
    	Node n = new GenericNode();
    	n.setType(type);
    	return n;
    }
    
    public Relationship newRelationship(String type)
    {
    	Relationship r = new GenericRelationship();
    	r.setType(type);
    	return r;
    }
    
    /**
     * Builds a light-weight relationship with node instances that just hold the node id values.
     * 
     * @param id1
     * @param id2
     * @param type
     * @return
     */
    public Relationship buildLightRelationship(String id1, String id2, String type)
    {
    	Relationship r = new GenericRelationship();
    	r.setType(type);
    	
    	Node n1 = new GenericNode();
    	Node n2 = new GenericNode();
    	
    	n1.setID(id1);
    	n2.setID(id2);
    	r.setNode1(n1);
    	r.setNode2(n2);
    	
    	return r;
    }
    
    /**
     * Return an instance of the configured graphmanager.  May be a singleton.
     * 
     * @return
     */
    public GraphManager getGraphManager()
    {
    	return getGraphManager(NoGraphConfig.DEFAULT_NAME);
    }
    
    /**
     * Return as instance of the named graphamanager.  May be a singleton.
     * 
     * @param name
     * @return
     */
    public GraphManager getGraphManager(String name)
    {
    	GraphManager gm = null;
    	
    	String gmclass = config.getGraphManagerClassName();
    	if(gmclass == null || gmclass.equalsIgnoreCase("org.nograph.impl.LuceneGraphManager"))
    	{
    		gm = LuceneGraphManager.getInstance(name);
    	}
    	else
    	{
    		try
    		{
    			gm = (GraphManager)Class.forName(gmclass).newInstance();
    		}
    		catch(Exception ex)
    		{
    			logger.log(Level.SEVERE, "Error getting graph manager", ex);
    		}
    	}
    	
    	return gm;
    }
    
    public NoGraphConfig getConfig()
    {
    	return config;
    }
}
