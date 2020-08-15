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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Some utilities for working with files.
 * 
 * @author aholinch
 *
 */
public class FileUtil 
{
	private static final Logger logger = Logger.getLogger(FileUtil.class.getName());
	
	public static final int BUFFER_SIZE = 4096;
	
	public static final String GENERIC_TEXT = "text/plain";
	public static final String GENERIC_BINARY = "application/octet-stream";
	
	/**
	 * Internal copy for this util.
	 */
	private static Map<String,String> mimeMap = null;
	private static final String mimeSync = "mutex";
	
	/**
	 * Attempts to locate the file on the file system and will then later check the classpath.
	 * @param file
	 * @return
	 */
    public static InputStream getInputStream(String file)
    {
    	InputStream is = null;
    	
    	try
    	{
    		is = new FileInputStream(file);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.FINE, "Not able to open file", ex);
    	}
    	
    	if(is == null)
    	{
        	try
        	{
        		is = FileUtil.class.getResourceAsStream(file);
        	}
        	catch(Exception ex)
        	{
        		logger.log(Level.FINE, "Not able to open stream", ex);
        	}    		
    	}

    	if(is == null)
    	{
        	try
        	{
        		is = ClassLoader.getSystemResourceAsStream(file);
        	}
        	catch(Exception ex)
        	{
        		logger.log(Level.FINE, "Not able to open stream", ex);
        	}    		
    	}
    	
    	if(is == null)
    	{
    		logger.info("No stream found for " + file);
    	}

    	return is;
    }
    
    public static String convertStreamToString(InputStream is, String encoding)
    {
    	String out = null;
    
    	InputStreamReader isr = null;
    	BufferedReader br = null;
    	
    	try
    	{
    		
    		isr = new InputStreamReader(is,encoding);
    		br = new BufferedReader(isr,BUFFER_SIZE);
    		
    		String line = null;
    		StringBuffer sb = new StringBuffer(4*BUFFER_SIZE);
    		
    		line = br.readLine();
    		while(line != null)
    		{
    			sb.append(line).append("\n");
        		line = br.readLine();
    		}
    		
    		out = sb.toString();
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error getting string", ex);
    	}
    	finally
    	{
    		close(isr);
    		close(br);
    	}
    	
    	return out;
    }
    
    public static String getStringFromFile(String file)
    {
    	return getStringFromFile(file,"UTF-8");
    }
    
    public static String getStringFromFile(String file, String encoding)
    {
    	InputStream is = null;
    	
    	is = getInputStream(file);
    	
    	String out = null;
    	
    	try
    	{
    		out = convertStreamToString(is,encoding);
    	}
    	finally
    	{
    		close(is);
    	}
    	
    	return out;
    }
    
    /**
     * If it's not null, close it.
     * 
     * @param is
     */
    public static void close(InputStream is)
    {
    	if(is != null)
    	{
    		try{is.close();}catch(Exception ex){}; // we don't care about exception
    	}
    }
    
    /**
     * If it's not null, close it.
     * 
     * @param r
     */
    public static void close(Reader r)
    {
    	if(r != null)
    	{
    		try{r.close();}catch(Exception ex){}; // we don't care about exception
    	}
    }
    
    /**
     * If it's not null, close it.
     * 
     * @param is
     */
    public static void close(OutputStream os)
    {
    	if(os != null)
    	{
    		try{os.flush();}catch(Exception ex){}; // we don't care about exception
    		try{os.close();}catch(Exception ex){}; // we don't care about exception
    	}
    }
    
    /**
     * If it's not null, close it.
     * 
     * @param w
     */
    public static void close(Writer w)
    {
    	if(w != null)
    	{
    		try{w.flush();}catch(Exception ex){}; // we don't care about exception
    		try{w.close();}catch(Exception ex){}; // we don't care about exception
    	}
    }
    
    /**
     * Pass filename or extension and get a mimetype.
     * 
     * @param filename
     * @return
     */
    public static String getMimeType(String filename)
    {
    	if(filename == null || filename.trim().length() == 0) return null;
    	
    	if(mimeMap == null)
    	{
    		synchronized(mimeSync)
    		{
    			if(mimeMap == null) // yes, check again
    			{
    				mimeMap = buildMimeTypeMap();
    			}
    		}
    	}
    	
    	String type = null;
    	
    	filename = filename.trim().toLowerCase();
    	int ind = filename.lastIndexOf('.');
    	if(ind > 0)
    	{
    		filename = filename.substring(ind);
    	}
    	else if(ind < 0)
    	{
    		filename = "."+filename;
    	} // if ind == 0 means the filename starts with the last . and that is what we want
    	
    	type = mimeMap.get(filename);
    	
    	if(type == null)
    	{
    		type = GENERIC_BINARY;
    	}
    	
    	return type;
    }
    
    /**
     * Return a map of file extension to mimetype for several key types.  Based on an incomplete list from Mozilla.
     * 
     * @return
     */
    public static Map<String,String> buildMimeTypeMap()
    {
    	Map<String,String> m = new HashMap<String,String>();
    	
    	m.put(".abw","application/x-abiword");
    	m.put(".arc","application/x-freearc");
    	m.put(".avi","video/x-msvideo");
    	m.put(".azw","application/vnd.amazon.ebook");
    	m.put(".bin","application/octet-stream");
    	m.put(".bmp","image/bmp");
    	m.put(".bz","application/x-bzip");
    	m.put(".bz2","application/x-bzip2");
    	m.put(".csh","application/x-csh");
    	m.put(".css","text/css");
    	m.put(".csv","text/csv");
    	m.put(".doc","application/msword");
    	m.put(".docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    	m.put(".eot","application/vnd.ms-fontobject");
    	m.put(".epub","application/epub+zip");
    	m.put(".gz","application/gzip");
    	m.put(".gif","image/gif");
    	m.put(".htm","text/html");
    	m.put(".html","text/html");
    	m.put(".ico","image/vnd.microsoft.icon");
    	m.put(".ics","text/calendar");
    	m.put(".jar","application/java-archive");
    	m.put(".jpeg","image/jpeg");
    	m.put(".jpg","image/jpeg");
    	m.put(".js","text/javascript");
    	m.put(".json","application/json");
    	m.put(".jsonld","application/ld+json");
    	m.put(".mid","audio/midi");
    	m.put(".midi","audio/midi");
    	m.put(".mjs","text/javascript");
    	m.put(".mp3","audio/mpeg");
    	m.put(".mp4","video/mpeg");
    	m.put(".mpeg","video/mpeg");
    	m.put(".mpkg","application/vnd.apple.installer+xml");
    	m.put(".odp","application/vnd.oasis.opendocument.presentation");
    	m.put(".ods","application/vnd.oasis.opendocument.spreadsheet");
    	m.put(".odt","application/vnd.oasis.opendocument.text");
    	m.put(".oga","audio/ogg");
    	m.put(".ogv","video/ogg");
    	m.put(".ogx","application/ogg");
    	m.put(".otf","font/otf");
    	m.put(".png","image/png");
    	m.put(".pdf","application/pdf");
    	m.put(".php","appliction/php");
    	m.put(".ppt","application/vnd.ms-powerpoint");
    	m.put(".pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation");
    	m.put(".rar","application/x-rar-compressed");
    	m.put(".rtf","application/rtf");
    	m.put(".sh","application/x-sh");
    	m.put(".svg","image/svg+xml");
    	m.put(".swf","application/x-shockwave-flash");
    	m.put(".tar","application/x-tar");
    	m.put(".tif","image/tiff");
    	m.put(".tiff","image/tiff");
    	m.put(".ts","video/mp2t");
    	m.put(".ttf","font/ttf");
    	m.put(".txt","text/plain");
    	m.put(".vsd","application/vnd.visio");
    	m.put(".wav","audio/wav");
    	m.put(".weba","audio/webm");
    	m.put(".webm","video/webm");
    	m.put(".webp","image/webp");
    	m.put(".woff","font/woff");
    	m.put(".woff2","font/woff2");
    	m.put(".xhtml","application/xhtml+xml");
    	m.put(".xls","application/vnd.ms-excel");
    	m.put(".xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    	m.put(".xml","text/xml");
    	m.put(".xul","application/vnd.mozilla.xul+xml");
    	m.put(".zip","application/zip");
    	m.put(".3gp","video/3gpp");
    	m.put(".3g2","video/3gpp2");
    	m.put(".7z","application/x-7z-compressed");

    	return m;
    }
    
    public static String getExt(File f)
    {
    	return getExt(f.getName());
    }
    
    public static String getExt(String file)
    {
    	if(file == null) return null;
    	int ind = file.indexOf('.');
    	if(ind <0) return null;
    	
    	file = file.substring(ind+1);
    	return file;
    }
    
    public static void copyFile(String src, String dst)
    {
    	try
    	{
    		Path p1 = Paths.get(src);
    		Path p2 = Paths.get(dst);
    		Files.copy(p1, p2);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING,"Error copying file",ex);
    	}
    }
}
