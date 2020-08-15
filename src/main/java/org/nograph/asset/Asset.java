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
package org.nograph.asset;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.nograph.impl.BasePropertyHolder;

/**
 * Holds the metadata for a file or binary object like images, documents, etc.
 * 
 * @author aholinch
 *
 */
public class Asset extends BasePropertyHolder
{
	
	public static final String UUID        = "uuid";
	public static final String FILENAME    = "filename";
	public static final String TITLE       = "title";
	public static final String DESCRIPTION = "description";
	public static final String CONTENT     = "content";
	public static final String TAG         = "tag";
	public static final String FILESIZE    = "filesize";
	public static final String DATE        = "assetdate";
	
    public Asset()
    {
    	
    }
    
    public Asset(File f)
    {
    	setFileInfo(f);
    }
    
    public void setFileInfo(File f)
    {
    	String str = f.getName();
    	setFilename(str);
    	
    	if(getTitle()==null)
    	{
	    	int ind = str.lastIndexOf('.');
	    	if(ind > -1)
	    	{
	    		str = str.substring(0,ind);
	    	}
	    	setTitle(str);
    	}
    	setFilesize(f.length());
    	setDate(new java.util.Date(f.lastModified()));    	
    }
    
    public String getID()
    {
    	return getUUID();
    }
    
    public String getUUID()
    {
    	return getString(UUID);
    }
    
    public void setUUID(String str)
    {
    	setProperty(UUID,str);
    }
    
    public String fileExtension()
    {
    	String str = getFilename();
    	if(str != null)
    	{
    		int ind = str.lastIndexOf('.');
    		if(ind > -1)
    		{
    			str = str.substring(ind+1);
    		}
    	}
    	return str;
    }
    public String getFilename()
    {
    	return getString(FILENAME);
    }
    
    public void setFilename(String str)
    {
    	setProperty(FILENAME,str);
    }
    
    public String getTitle()
    {
    	return getString(TITLE);
    }
    
    public void setTitle(String str)
    {
    	setProperty(TITLE,str);
    }
    
    public String getDescription()
    {
    	return getString(DESCRIPTION);
    }
    
    public void setDescription(String str)
    {
    	setProperty(DESCRIPTION,str);
    }
    
    public String getContent()
    {
    	return getString(CONTENT);
    }
    
    public void setContent(String str)
    {
    	setProperty(CONTENT,str);
    }
    
    public String getTag()
    {
    	return getString(TAG);
    }
    
    public void setTag(String str)
    {
    	addProperty(TAG,str);
    }
    
    public void addTag(String str)
    {
    	addProperty(TAG,str);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String> getTags()
    {
    	List gen = getProperties(TAG);
    	return gen;
    }
    
    public Long getFilesize()
    {
    	return getLong(FILESIZE);
    }
    
    public void setFilesize(long size)
    {
    	setProperty(FILESIZE,size);
    }
    
    public Date getDate()
    {
    	return getDate(DATE);
    }
    
    public void setDate(Date date)
    {
    	setProperty(DATE,date);
    }
}
