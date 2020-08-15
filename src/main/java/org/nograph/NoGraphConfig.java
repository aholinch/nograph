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

import java.io.FileWriter;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.nograph.util.FileUtil;

/**
 * Configuration information.
 * 
 * @author aholinch
 *
 */
public class NoGraphConfig 
{
    protected Properties props = null;
    
    public static final String PROP_GMCLASS = "graphman.class";
    public static String CONFIG_FILE = "conf/nograph.config";
    
    public static final String DEFAULT_NAME = "default";
    
    public NoGraphConfig()
    {
    	this(true);
    }
    
    public NoGraphConfig(boolean initialize)
    {
    	props = new Properties();
    	
    	if(initialize)
    	{
    		init();
    	}
    }
    
    protected void init()
    {
    	InputStream is = null;
    	try
    	{
    		is = FileUtil.getInputStream(CONFIG_FILE);
    		if(is != null)
    		{
    			props.load(is);
    		}
    		else
    		{
    			Logger.getLogger(NoGraphConfig.class.getName()).info("No props parsed");
    		}
    	}
    	catch(Exception ex)
    	{
    		Logger.getLogger(NoGraphConfig.class.getName()).log(Level.WARNING,"Error parsing config",ex);
    	}
    	finally
    	{
    		FileUtil.close(is);
    	}
    }
    
    public void write()
    {
    	write(CONFIG_FILE);
    }
    
    public void write(String filename)
    {
        FileWriter fw = null;
        try
        {
        	fw = new FileWriter(filename);
        	props.store(fw, "NoGraph Configuration");
        }
        catch(Exception ex)
    	{
    		Logger.getLogger(NoGraphConfig.class.getName()).log(Level.WARNING,"Error writing config",ex);
    	}
    	finally
    	{
    		FileUtil.close(fw);
    	}
    }
    
    public String getGraphManagerClassName()
    {
    	return getProperty(PROP_GMCLASS);
    }
    
    public void setGraphManagerClassName(String str)
    {
    	setProperty(PROP_GMCLASS,str);
    }
    
    public String getProperty(String key)
    {
    	return props.getProperty(key);
    }
    
    public void setProperty(String key, String val)
    {
    	props.setProperty(key, val);
    }
    
    public int getIntProperty(String key)
    {
    	int num = 0;
    	try
    	{
    		num = Integer.parseInt(props.getProperty(key).trim());
    	}
    	catch(Exception ex)
    	{
    		// you'll just have to wonder
    	}
    	return num;
    }
    
    public boolean getBoolProperty(String key)
    {
    	boolean flag = false;
    	try
    	{
    		String str = props.getProperty(key).trim();
    		if(str != null)
    		{
    			str = str.trim();
    			if(str.length()>0)
    			{
    				str = str.toLowerCase();
    				char c = str.charAt(0);
    				if(c == 't' || c == 'y' || c == '1')
    				{
    					flag = true;
    				}
    			}
    		}
    	}
    	catch(Exception ex)
    	{
    		// you'll just have to wonder
    	}
    	return flag;
    }
    
    public void setProperty(String key, Object obj)
    {
    	if(obj != null)
    	{
    		props.setProperty(key, obj.toString());
    	}
    	else
    	{
    		props.remove(key);
    	}
    }
}
