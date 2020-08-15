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
package org.nograph.util;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.nograph.NoGraph;
import org.nograph.Node;
import org.nograph.PropertyHolder;
import org.nograph.Relationship;

/**
 * Utilities to help organize graph data.
 * 
 * @author aholinch
 *
 */
public class GraphUtil 
{
	/**
	 * Put the nodes into lists by type.
	 * 
	 * @param nodes
	 * @return
	 */
    public static Map<String,List<Node>> groupNodesByType(List<Node> nodes)
    {
    	Map<String,List<Node>> m = new HashMap<String,List<Node>>();
    	
    	int size = 0;
    	if(nodes != null) size = nodes.size();
    	
    	List<Node> tmp = null;
    	String type = null;
    	Node n = null;
    	
    	for(int i=0; i<size; i++)
    	{
    		n = nodes.get(i);
    		type = n.getType();
    		tmp = m.get(type);
    		if(tmp == null)
    		{
    			tmp = new ArrayList<Node>();
    			m.put(type, tmp);
    		}
    		tmp.add(n);
    	}
    	
    	return m;
    }
    
	/**
	 * Put the nodes into lists by type.
	 * 
	 * @param nodes
	 * @return
	 */
    public static Map<String,List<Node>> groupNodesByProperty(List<Node> nodes, String propName)
    {
    	Map<String,List<Node>> m = new HashMap<String,List<Node>>();
    	
    	int size = 0;
    	if(nodes != null) size = nodes.size();
    	
    	List<Node> tmp = null;
    	String prop = null;
    	Node n = null;
    	
    	for(int i=0; i<size; i++)
    	{
    		n = nodes.get(i);
    		prop = n.getString(propName);
    		tmp = m.get(prop);
    		if(tmp == null)
    		{
    			tmp = new ArrayList<Node>();
    			m.put(prop, tmp);
    		}
    		tmp.add(n);
    	}
    	
    	return m;
    }
    
    /**
     * Put the relationships in lists by types.
     * 
     * @param rels
     * @return
     */
    public static Map<String,List<Relationship>> groupRelationshipsByType(List<Relationship> rels)
    {
    	Map<String,List<Relationship>> m = new HashMap<String,List<Relationship>>();
    	
    	int size = 0;
    	if(rels != null) size = rels.size();
    	
    	List<Relationship> tmp = null;
    	String type = null;
    	Relationship r = null;
    	
    	for(int i=0; i<size; i++)
    	{
    		r = rels.get(i);
    		type = r.getType();
    		tmp = m.get(type);
    		if(tmp == null)
    		{
    			tmp = new ArrayList<Relationship>();
    			m.put(type, tmp);
    		}
    		tmp.add(r);
    	}
    	    	
    	return m;
    }
    
    /**
     * Using a basic GML parser fill the list of nodes and relationships.
     * 
     * @param nodes
     * @param rels
     */
    public static void readGML(String file, List<Node> nodes, List<Relationship> rels)
    {
    	String content = FileUtil.getStringFromFile(file);
    	String lines[] = content.split("\n");
    	int nl = lines.length;
    	String line = null;
    	
    	// get to start of graph
    	int ind = 0;
    	int end = nl-1;
    	while (!lines[end].contains("]")){ end--;}
    	end--;
    	
    	while(!lines[ind].trim().startsWith("graph") && ind < nl){
    		ind++;
    	}
    	
    	while(!lines[ind].contains("["))
    	{
    		ind++;
    	}
    	
    	if(ind == nl || ind >= end)
    	{
    		Logger.getLogger(GraphUtil.class.getName()).log(Level.WARNING,"No graph data");
    	}
    	
    	// we don't have any properties on the graph itself, so we look for the next appearance of [
    	
    	String type=null;
    	String lbl = null;
    	
    	int si = 0;
    	int si2 = 0;
    	
    	int cnt = 0;
    	int os = 0;
    	int oe = 0;
    	PropertyHolder ph = null;
    	Node n = null;
    	Relationship rel = null;
    	NoGraph ng = NoGraph.getInstance();
    	
    	for(int i=ind; i<end; i++)
    	{
    		line = lines[i].trim();
    		si = line.indexOf('[');
    		if(si > -1)
    		{
    			if(si == 0) // symbol at start means get previous line
    			{
    				type = lines[i-1].trim();
    			}
    			else
    			{
    				type = line.substring(0,si).trim();
    			}
    			
    			type = type.toLowerCase();
    			if(type.equals("node") || type.equals("edge"))
    			{
    				// parse nodes and edges
    				cnt = 1;
    				ind = i+1;
    				os = ind;
    				while(cnt > 0 && ind < end)
    				{
    					line = lines[ind].trim();
    					si = line.indexOf('[');
    					si2 = line.indexOf(']');
    					if(si>-1)cnt++;
    					if(si2>-1)cnt--;
    					ind++;
    				}
    				oe = ind;
    				
    				if(oe <= end)
    				{
    					// let's parse it
    					if(type.equals("node"))
    					{
    						n = ng.newNode("");
    						ph = n;
    						nodes.add(n);
    					}
    					else
    					{
    						rel = ng.newRelationship("");
    						ph = rel;
    						rels.add(rel);
    					}
    					
    					for(int j=os; j<oe; j++)
    					{
    						line = lines[j].trim();
    						if(line.startsWith("id "))
    						{
    							ph.setID(line.substring(3).trim());
    						}
    						else if(line.startsWith("label "))
    						{
    							lbl = line.substring(6).trim();
    							if(lbl.startsWith("\"")) lbl = lbl.substring(1).trim();
    							if(lbl.endsWith("\"")) lbl = lbl.substring(0,lbl.length()-1).trim();
    							ph.setLabel(lbl);
    						}
    						else if(line.startsWith("source ") && rel != null)
    						{
    							n = ng.newNode("");
    							n.setID(line.substring(7).trim());
    							rel.setNode1(n);
    						}
    						else if(line.startsWith("target ") && rel != null)
    						{
    							n = ng.newNode("");
    							n.setID(line.substring(7).trim());
    							rel.setNode2(n);
    						}
    					}
    					
    					n = null;
    					rel = null;
    					ph = null;
    				}
    				i = ind-1;
    			}
    		}
    	}    	
    	
    } // end GML
}
