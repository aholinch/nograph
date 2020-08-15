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
package org.nograph.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.nograph.Node;
import org.nograph.Relationship;
import org.nograph.util.json.JSONObject;
import org.nograph.util.json.JSONString;

/**
 * Tracks information about property names for nodes and rels by type.
 * 
 * @author aholinch
 *
 */
public class GraphMeta implements JSONString
{
    protected Map<String,Map<String,String>> nodeProps = null;
    protected Map<String,Map<String,String>> relProps = null;
    
    public GraphMeta()
    {
    	nodeProps = new HashMap<String,Map<String,String>>();
    	relProps = new HashMap<String,Map<String,String>>();
    }
    
    public List<String> getPropertiesForNodeType(String type)
    {
    	List<String> out = null;
    	
    	Map<String,String> p = nodeProps.get(type);
    	if(p != null)
    	{
    		out = new ArrayList<String>(p.keySet());
    	}
    	else
    	{
    		out = new ArrayList<String>();
    	}
    	
    	return out;
    }
    
    public List<String> getPropertiesForRelationshipType(String type)
    {
    	List<String> out = null;
    	
    	Map<String,String> p = relProps.get(type);
    	if(p != null)
    	{
    		out = new ArrayList<String>(p.keySet());
    	}
    	else
    	{
    		out = new ArrayList<String>();
    	}
    	
    	return out;
    }
    
    public void updateNodeMeta(Node n)
    {
    	if(n != null)
    	{
    		String type = n.getType();
    		Map<String,Object> props = n.getPropertyMap();
    		updateMeta(nodeProps,type,props);
    	}
    }
    
    public void updateRelationshipMeta(Relationship r)
    {
    	if(r != null)
    	{
    		String type = r.getType();
    		Map<String,Object> props = r.getPropertyMap();
    		updateMeta(relProps,type,props);    		
    	}
    }
    
    protected void updateMeta(Map<String,Map<String,String>> m, String type, Map<String,Object> propMap)
    {
    	Map<String,String> p = m.get(type);
    	if(p == null)
    	{
    		p = new HashMap<String,String>();
    		m.put(type, p);
    	}
    	
    	if(propMap != null)
    	{
    		List<String> propNames = new ArrayList<String>(propMap.keySet());
    		int size = propNames.size();
    		String prop = null;
    		Object val = null;
    		String valtype = null;
    		for(int i=0; i<size; i++)
    		{
    			prop = propNames.get(i);
    			val = propMap.get(prop);
    			if(val != null)
    			{
    				valtype = getType(val);
    				p.put(prop, valtype);
    			}
    		}
    	}
    }
    
    @SuppressWarnings("rawtypes")
	protected String getType(Object val)
    {
    	String type = "string";
    	
    	if(val instanceof Number)
    	{
    		if(val instanceof Long)
    		{
    			type = "long";
    		}
    		else if(val instanceof Double)
    		{
    			type = "double";
    		}
    		else if(val instanceof Integer)
    		{
    			type = "integer";
    		}
    		else if(val instanceof Float)
    		{
    			type = "float";
    		}
    		else if(val instanceof Short)
    		{
    			type = "short";
    		}
    	}
    	else if(val instanceof java.util.Date)
    	{
    		type = "date";
    	}
    	else if(val instanceof java.util.Collection)
    	{
    		if(((java.util.Collection)val).size()>0)
    		{
    			Iterator iter = ((java.util.Collection)val).iterator();
    			type = getType(iter.next());
    		}
    	}
    	else if(val.getClass().isArray())
    	{
    		if(Array.getLength(val)>0)
    		{
    			type = getType(Array.get(val, 0));
    		}
    	}
    	
    	return type;
    }
    
	@Override
	public String toJSONString()
	{
		Map<String,Map<String,Map<String,String>>> out = new HashMap<String,Map<String,Map<String,String>>>();
		out.put("nodes", nodeProps);
		out.put("rels",relProps);
		/*
		List<String> types = null;
		List<String> props = null;
		Map<String,List<String>> m = null;
		
		int size = 0;
		String type = null;
		
		types = new ArrayList<String>(nodeProps.keySet());
		m = new HashMap<String,List<String>>();
		out.put("nodes", m);
		
		size = types.size();
		for(int i=0; i<size; i++)
		{
			type = types.get(i);
			props = getPropertiesForNodeType(type);
			m.put(type, props);
		}
		
		
		types = new ArrayList<String>(relProps.keySet());
		m = new HashMap<String,List<String>>();
		out.put("rels", m);
		
		size = types.size();
		for(int i=0; i<size; i++)
		{
			type = types.get(i);
			props = getPropertiesForRelationshipType(type);
			m.put(type, props);
		}
		*/
		JSONObject obj = new JSONObject(out);
		return obj.toString();
	}
	
	public void fromJSONString(String json)
	{
		JSONObject obj = new JSONObject(json);
		
		JSONObject o = null;
		
		if(obj.has("nodes"))
		{
			o = obj.getJSONObject("nodes");
			if(o != null)
			{
				List<String> types = new ArrayList<String>(o.keySet());
				JSONObject map = null;
				String type = null;
				int size = types.size();
				for(int i=0; i<size; i++)
				{
					type = types.get(i);
					map = o.getJSONObject(type);
					updateMeta(nodeProps,type,map.toMap());
				}
			}
		} // end nodes
		
		if(obj.has("rels"))
		{
			o = obj.getJSONObject("rels");
			if(o != null)
			{
				List<String> types = new ArrayList<String>(o.keySet());
				JSONObject map = null;
				String type = null;
				int size = types.size();
				for(int i=0; i<size; i++)
				{
					type = types.get(i);
					map = o.getJSONObject(type);
					updateMeta(relProps,type,map.toMap());
				}
			}
		} // end rels
		
	} // end fromJSONString
}
