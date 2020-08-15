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

import java.util.HashMap;
import java.util.Map;

import org.nograph.Node;
import org.nograph.Relationship;
import org.nograph.util.json.JSONObject;

public class GenericRelationship extends BasePropertyHolder implements Relationship 
{
    protected Node node1;
    protected Node node2;
    
    public static final String N1_KEY = "node1";
    public static final String N2_KEY = "node2";
    public static final String N1_TYPEKEY = "node1type";
    public static final String N2_TYPEKEY = "node2type";
    
	public GenericRelationship()
	{
		super();
	}
	
	@Override
	public Node getNode1() 
	{
		return node1;
	}

	@Override
	public void setNode1(Node node) 
	{
		node1 = node;
	}

	@Override
	public Node getNode2() 
	{
		return node2;
	}

	@Override
	public void setNode2(Node node) 
	{
		node2 = node;
	}

	@Override
	public String getNode1ID() 
	{
		if(node1 == null) return null;
		
		return node1.getID();
	}

	@Override
	public String getNode2ID() 
	{
		if(node2 == null) return null;
		
		return node2.getID();
	}

	@Override
	public String getNode1Type() 
	{
		if(node1 == null) return null;
		
		return node1.getType();
	}

	@Override
	public String getNode2Type() 
	{
		if(node2 == null) return null;
		
		return node2.getType();
	}

	protected Map<String,Object> getPropertiesForJSON()
	{
		Map<String,Object> m = super.getPropertiesForJSON();

		// consider option to just store node1 id and node2 id
		m.put(N1_KEY, node1);
		m.put(N2_KEY, node2);
		
		return m;
	}
	
	/**
	 * Remove any properties from the map that may have been added by getPropertiesForJSON()
	 * @param m
	 */
	@SuppressWarnings("unchecked")
	protected void removeExtraPropertiesFromMap(Map<String,Object> m)
	{
		super.removeExtraPropertiesFromMap(m);
		
		Object v = null;
		v = m.get(N1_KEY);
		if(v != null)
		{
			String str = null;
			if(v instanceof JSONObject)
			{
				str = ((JSONObject)v).toString();
			}
			else if(v instanceof String)
			{
				str = (String)v;
			}
			
			if(str != null)
			{
				node1 = new GenericNode();
				node1.fromJSONString(str);
			}
			else if(v instanceof Node)
			{
				node1 = (Node)v;
			}
			else if(v instanceof java.util.Map)
			{
				GenericNode n1 = new GenericNode();
				n1.fromJSONObject(new JSONObject((java.util.Map<String,Object>)v));	
				node1 = n1;
			}
			
			m.remove(N1_KEY);
		}
		m.remove(N1_TYPEKEY);
		
		v = m.get(N2_KEY);
		if(v != null)
		{
			String str = null;
			if(v instanceof JSONObject)
			{
				str = ((JSONObject)v).toString();
			}
			else if(v instanceof String)
			{
				str = (String)v;
			}
			
			if(str != null)
			{
				node2 = new GenericNode();
				node2.fromJSONString(str);
			}
			else if(v instanceof Node)
			{
				node2 = (Node)v;
			}
			else if(v instanceof java.util.Map)
			{
				GenericNode n2 = new GenericNode();
				n2.fromJSONObject(new JSONObject((java.util.Map<String,Object>)v));	
				node2 = n2;
			}
			
			m.remove(N2_KEY);
		}
		m.remove(N2_TYPEKEY);
		
	}
	
	public String getLabel()
	{
		String lbl = super.getLabel();
		if(lbl == null)
		{
			lbl = getType();
		}
		
		return lbl;
	}

	@Override
	public Map<String, Object> getMinPropertyMap() 
	{
		Map<String,Object> m = super.getPropertiesForJSON();
		
		if(node1 != null)
		{
			Map<String,String> m1 = new HashMap<String,String>();
			m1.put(ID_KEY, node1.getID());
			m1.put(TYPE_KEY, node1.getType());
			m.put(N1_KEY, m1);
		}
		
		if(node2 != null)
		{
			Map<String,String> m2 = new HashMap<String,String>();
			m2.put(ID_KEY, node2.getID());
			m2.put(TYPE_KEY, node2.getType());
			m.put(N2_KEY, m2);
		}
		
		return m;
	}
}
