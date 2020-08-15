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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nograph.PropertyHolder;
import org.nograph.util.DateUtil;
import org.nograph.util.json.JSONObject;
import org.nograph.util.json.JSONable;

public class BasePropertyHolder implements PropertyHolder, JSONable
{
    protected String id;
    protected String type;
    protected Map<String,Object> map = null;
    
    public static final String ID_KEY = "id";
    public static final String TYPE_KEY = "type";
    public static final String LABEL_KEY = "label";
    public static final String SV_KEY = "storedvalue";
    
    public BasePropertyHolder()
    {
        map = new HashMap<String,Object>();	
    }
    
	@Override
	public String getID() 
	{
		return id;
	}

	@Override
	public void setID(String str) 
	{
		id = str;
	}

	@Override
	public Long getLongID()
	{
		Long out = null;
		try{out = Long.parseLong(id);}catch(Exception ex){};
		return out;
	}

	@Override
	public void setLongID(Long num) 
	{
		id = String.valueOf(num);
	}

	@Override
	public String getType() 
	{
		return type;
	}

	@Override
	public void setType(String val) 
	{
		type = val;
	}
	
	@Override
	public String getLabel() 
	{
		return getString(LABEL_KEY);
	}

	@Override
	public void setLabel(String val) 
	{
		setProperty(LABEL_KEY,val);
	}


	@Override
	public Object getProperty(String key) 
	{
		return map.get(key);
	}

	@Override
	public void setProperty(String key, Object val) 
	{
		map.put(key, val);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addProperty(String key, Object val) 
	{
		Object val1 = map.get(key);
		if(val1 == null)
		{
			List<Object> list = new ArrayList<Object>();
			list.add(val);
			map.put(key, list);
		}
		else
		{
			if(val1 instanceof List)
			{
				((List)val1).add(val);
			}
			else
			{
				List<Object> list = new ArrayList<Object>();
				list.add(val1);
				list.add(val);
				map.put(key, list);				
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<Object> getProperties(String key) 
	{
		Object val = map.get(key);
		if(val instanceof List)
		{
			return (List)val;
		}
		
		List<Object> l = new ArrayList<Object>();
		l.add(val);
		
		return l;
	}

	@Override
	public void removeProperty(String key) 
	{
		map.remove(key);
	}

	@Override
	public List<String> getPropertyNames() 
	{
		return new ArrayList<String>(map.keySet());
	}

	@Override
	public Map<String, Object> getPropertyMap() 
	{
		Map<String,Object> out = new HashMap<String,Object>(map);
		
		return out;
	}

    public void setPropertyMap(Map<String,Object> m)
    {
    	map = null;
    	if(m != null)
    	{
    		map = new HashMap<String,Object>(m);
    	}
    }
    
	@Override
	public String getString(String key) 
	{
		Object val = map.get(key);
		if(val == null) return null;
		
		if(val instanceof String) return (String)val;
		
		return String.valueOf(val);
	}

	protected Number getNumber(String key)
	{
		Object val = map.get(key);
		if(val == null) return null;
		if(val instanceof Number)
		{
			return (Number)val;
		}
		return null;
	}
	
	@Override
	public Double getDouble(String key) 
	{
		Number n = getNumber(key);
		if(n != null)
		{
			return n.doubleValue();
		}
		
		Double d = null;
		String str = getString(key);
		if(str != null)
		{
			try{d = Double.parseDouble(str.trim());}catch(Exception ex){};
		}
		
		return d;
	}

	@Override
	public Long getLong(String key) {
		Number n = getNumber(key);
		if(n != null)
		{
			return n.longValue();
		}
		
		Long l = null;
		String str = getString(key);
		if(str != null)
		{
			try{l = Long.parseLong(str.trim());}catch(Exception ex){};
		}
		
		return l;
	}

	@Override
	public Integer getInteger(String key) {
		Number n = getNumber(key);
		if(n != null)
		{
			return n.intValue();
		}
		
		Integer i = null;
		String str = getString(key);
		if(str != null)
		{
			try{i = Integer.parseInt(str.trim());}catch(Exception ex){};
		}
		
		return i;
	}

	@Override
	public Date getDate(String key) 
	{
		Object val = map.get(key);
		if(val == null) return null;
		
		if(val instanceof java.util.Date)
		{
			return (java.util.Date)val;
		}
		
		if(val instanceof java.sql.Date)
		{
			return new java.util.Date(((java.sql.Date)val).getTime());
		}
		
		Long t = getLong(key);
		if(t != null)
		{
			return new java.util.Date(t);
		}
		
		// we can try parsing common date formats here????
		try
		{
			return DateUtil.parseISODate(String.valueOf(val));
		}
		catch(Exception ex)
		{
			// date parse issue, worth sharing?
		}
		return null;
	}
	
	@Override
	public Boolean getBoolean(String key)
    {
		Object val = map.get(key);
		if(val == null) return null;
		
		if(val instanceof Boolean)
		{
			return (Boolean)val;
		}
		
		Boolean flag = null;
		String str = String.valueOf(val).toLowerCase();
		if(str.length()>0)
		{
			char c = str.charAt(0);
			if(c=='t'||c=='1'||c=='y')
			{
				flag = true;
			}
			else
			{
				flag = false;
			}
		}
		
		return flag;
	}

	protected Map<String,Object> getPropertiesForJSON()
	{
		Map<String,Object> m = getPropertyMap();
		
		if(id != null)m.put(ID_KEY, id);
		m.put(TYPE_KEY, type);
		
		return m;
	}
	
	@Override
	public String toJSONString() 
	{
		Map<String,Object> m = getPropertiesForJSON();
		
		JSONObject obj = new JSONObject(m);
		
		return obj.toString();
	}

	/**
	 * Remove any properties from the map that may have been added by getPropertiesForJSON()
	 * @param m
	 */
	protected void removeExtraPropertiesFromMap(Map<String,Object> m)
	{
		Object v = null;
		v = m.get(ID_KEY);
		if(v != null)
		{
			id = String.valueOf(v);
			m.remove(ID_KEY);
		}
		v = m.get(TYPE_KEY);
		if(v != null)
		{
			type = String.valueOf(v);
			m.remove(TYPE_KEY);
		}
	}
	
	@Override
	public void fromJSONString(String str) 
	{
		JSONObject obj = new JSONObject(str);
		
		fromJSONObject(obj);
	}
	
	protected void fromJSONObject(JSONObject obj)
	{
		Map<String,Object> m = obj.toMap();
		
		removeExtraPropertiesFromMap(m);
		
		setPropertyMap(m);		
	}
	
	@Override
	public void fromJSONString(String str,String key) 
	{
		JSONObject obj = new JSONObject(str);
		fromJSONObject(obj.getJSONObject(key));
	}
	
	public String toString()
	{
		return toJSONString();
	}

	@Override
	public String getStoredValue()
	{
		Object obj = map.get(SV_KEY);
		
		if(obj == null) return null;
		
		return String.valueOf(obj);
	}

	@Override
	public void setStoredValue(String val) 
	{
		map.put(SV_KEY, val);
	}

	@Override
	public Map<String, Object> getMinPropertyMap() 
	{
		return getPropertyMap();
	}
}
