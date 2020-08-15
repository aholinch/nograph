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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nograph.util.json.JSONable;

/**
 * Makes getting and setting property values easy.
 * 
 * @author aholinch
 *
 */
public interface PropertyHolder extends ID, Type, JSONable
{
	public String getLabel();
	
	public void setLabel(String val);
	
	public String getStoredValue();
	public void setStoredValue(String val);
	
    public Object getProperty(String key);
    
    public void setProperty(String key, Object val);
    
    /**
     * Will create a list for the key, if not already existing, and add the value to it.
     * 
     * @param key
     * @param val
     */
    public void addProperty(String key, Object val);
    
    /**
     * Returns the values for this key as a list, even if only one is set.
     * 
     * @param key
     * @return
     */
    public List<Object> getProperties(String key);
    
    public void removeProperty(String key);
    
    public List<String> getPropertyNames();
    
    public Map<String,Object> getPropertyMap();
    
    public Map<String,Object> getMinPropertyMap();
    
    public void setPropertyMap(Map<String,Object> m);
    
    public String getString(String key);
    
    public Double getDouble(String key);
    
    public Long getLong(String key);
    
    public Integer getInteger(String key);
    
    public Boolean getBoolean(String key);
    
    public Date getDate(String key);
}
