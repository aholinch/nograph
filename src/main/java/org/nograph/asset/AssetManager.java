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
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.nograph.NoGraph;
import org.nograph.NoGraphConfig;
import org.nograph.lucene.LuceneIndex;
import org.nograph.lucene.LuceneUtil;
import org.nograph.util.FileUtil;

/**
 * Manages the asset metadata in a lucene index and the files in a set of directories.
 * 
 * @author aholinch
 *
 */
public class AssetManager 
{
	private static final Logger logger = Logger.getLogger(AssetManager.class.getName());
	
    protected LuceneIndex assetIndex;
    protected String assetBaseDir;
 
    public static final int MAX_BYTES = 2*1024*1024;
    
    public static final String ASSET_IND_DIR  = "asset.index";
    public static final String ASSET_FILE_DIR = "asset.files";
    
    public AssetManager()
    {
    	init();
    }
    
    protected void init()
    {
		NoGraphConfig config = NoGraph.getInstance().getConfig();

		String assetIndexDir = config.getProperty(ASSET_IND_DIR);
		assetBaseDir = config.getProperty(ASSET_FILE_DIR);

		assetIndexDir = assetIndexDir.replace('\\', '/');
		assetBaseDir = assetBaseDir.replace('\\', '/');
		
		if(!assetIndexDir.endsWith("/")) assetIndexDir+="/";
		if(!assetBaseDir.endsWith("/")) assetBaseDir+="/";
		
		try
		{
			File dir = null;
			
			dir = new File(assetIndexDir);
			if(!dir.exists())dir.mkdir(); // just create last level
			dir = new File(assetBaseDir);
			if(!dir.exists())dir.mkdir(); // just create last level
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE,"Error ensuring dirs exist",ex);
		}
		assetIndex = new LuceneIndex(assetIndexDir);
    }
    
    protected String getDirectory(String uuid)
    {
    	if(uuid == null) return null;
    	
    	if(uuid.length()<6)
    	{
    		return assetBaseDir+uuid+"/";
    	}
    	
    	String out = assetBaseDir+uuid.substring(0,2)+"/"+uuid.substring(2,4)+"/"+uuid.substring(4,6)+"/";
    	return out;
    }
    
    protected String getFilenameInRepo(Asset asset)
    {
    	String uuid = asset.getUUID();
    	String ext = asset.fileExtension();
    	String dir = getDirectory(uuid);
    	String name = dir+uuid+"."+ext;
    	return name;
    }
    
    public String saveAsset(Asset asset, File file)
    {
    	String uuid = null;
    	try
    	{
	    	uuid = asset.getUUID();
	    	if(uuid == null)
	    	{
	    		uuid = getUUID();
	    		asset.setUUID(uuid);
	    	}
	    	else
	    	{
	    		deleteAsset(uuid,false);
	    	}
	    	
	    	Document doc = toDoc(asset,null);
	    	assetIndex.saveDocument(doc);
	    	
	    	if(file != null && file.exists())
	    	{
	    		File dir = new File(getDirectory(uuid));
	    		dir.mkdirs();
	    		String newfile = getFilenameInRepo(asset);
	    		FileUtil.copyFile(file.getAbsolutePath(), newfile);
	    	}
	    	assetIndex.commit();
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE,"Error saving asset",ex);
    	}
    	return uuid;
    }
    
    public Asset getAsset(String uuid)
    {
    	List<Asset> assets = findAssets(Asset.UUID+":"+uuid);
    	Asset out = null;
    	if(assets != null && assets.size() > 0)
    	{
    		out = assets.get(0);
    	}
    	return out;
    }
    
    public byte[] getAssetBytes(String uuid)
    {
    	Asset asset = getAsset(uuid);
    	String filename = getFilenameInRepo(asset);
    	File file = new File(filename);
    	if(!file.exists()) 
		{
    		logger.warning("No file exists for " + uuid + " at " + filename);
    		return null;
		}
    	
    	long size = asset.getFilesize();
    	if(size > MAX_BYTES)
    	{
    		logger.warning("File too big to return as bytes, " + size + " > " + MAX_BYTES);
    		return null;
    	}
    	
    	FileInputStream fis = null;
    	byte[] out = null;
    	
    	try
    	{
    		fis = new FileInputStream(file);
    		out = new byte[(int)size];
    		int tot = 0;
    		int numRead = -1;
    		int len = 16*1024;
    		while(tot < size && numRead > 0)
    		{
    			numRead = fis.read(out, tot, len);
    			if(numRead > 0)
    			{
    				tot+=numRead;
    			}
    		}
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE,"Error getting bytes",ex);
    	}
    	finally
    	{
    		FileUtil.close(fis);
    	}
    	
    	return out;
    }
    
    public InputStream getAssetStream(String uuid)
    {
    	Asset asset = getAsset(uuid);
    	String filename = getFilenameInRepo(asset);
    	File file = new File(filename);
    	if(!file.exists()) 
		{
    		logger.warning("No file exists for " + uuid + " at " + filename);
    		return null;
		}
    	
    	FileInputStream fis = null;
    	try
    	{
    		fis = new FileInputStream(file);
    	}
    	catch(Exception ex)
    	{
    		// shouldn't happen because we already checked exists
    		logger.log(Level.WARNING,"Error getting file",ex);
    	}
    	
    	return fis;
    }
    
    public void deleteAsset(String uuid, boolean deleteFile)
    {
    	try
    	{
    		logger.info("Deleting asset with uuid " + uuid);
    		
    		assetIndex.deleteDocument(Asset.UUID, uuid);
    		assetIndex.commit();
    		
    		if(deleteFile)
    		{
	    		File dir = new File(getDirectory(uuid));
	    		File[] matchingFiles = dir.listFiles(new FilenameFilter() {
	    		    public boolean accept(File dir, String name) {
	    		        return name.startsWith("uuid");
	    		    }
	    		});
	    		
	    		if(matchingFiles != null && matchingFiles.length>0)
	    		{
	    			for(int i=0; i<matchingFiles.length; i++)
	    			{
	    				logger.info("Deleting file " + matchingFiles[i].getAbsolutePath());
	    				matchingFiles[i].delete();
	    			}
	    		}
    		}
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE,"Error deleting asset",ex);
    	}
    }
    
    public List<Asset> findAssets(String luceneQuery)
    {
    	List<Asset> assets = null;
    	
    	try
    	{
    		List<Document> docs = assetIndex.search(luceneQuery);
    		if(docs != null)
    		{
    			int size = docs.size();
    			assets = new ArrayList<Asset>(size);
    			Document doc = null;
    			Asset asset = null;
    			for(int i=0; i<size; i++)
    			{
    				doc = docs.get(i);
    				asset = fromDoc(doc);
    				assets.add(asset);
    			}
    		}
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING,"Error searching for assets",ex);
    	}
    	return assets;
    }
    
    public List<Asset> findAssets(String property, Object value)
    {
    	return findAssets(String.valueOf(property)+":"+String.valueOf(value));
    }
        
    public String getUUID()
    {
    	String str = UUID.randomUUID().toString();
    	str = str.replaceAll("-","");
    	str = str.toLowerCase();
    	return str;
    }
    
    protected Asset fromDoc(Document doc)
    {
    	Map<String,Object> map = LuceneUtil.fromDoc(doc);
    	Asset a = new Asset();
    	a.setPropertyMap(map);
    	return a;
    }
    
    protected Document toDoc(Asset asset, Document doc)
    {
		if(asset == null) return null;
		if(doc == null) doc = new Document();
		
		Map<String,Object> map = asset.getPropertyMap();
		
		LuceneUtil.toDoc(map, doc, true);
		
		return doc;
    }
}
