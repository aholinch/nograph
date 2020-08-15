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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Some utility functions for working with dates.
 * 
 * @author aholinch
 *
 */
public class DateUtil 
{
	/**
	 * Trust me, life is better if you are in a single timezone, so we set a default.
	 */
    public static void setDefaultTimeZone()
    {
    	java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("GMT"));
    }
    
    public static Date getDate(int year, int month, int day)
    {
    	return getDate(year,month,day,0,0,0,0);
    }

    public static Date getDate(int year, int month, int day, int hour, int minute)
    {
    	return getDate(year,month,day,hour,minute,0,0);
    }

    public static Date getDate(int year, int month, int day, int hour, int minute, double sec)
    {
    	int s = (int)sec;
    	sec -= s;
    	int m = (int)(1000.0d*sec);
    	
    	return getDate(year,month,day,hour,minute,s,m);
    }
    
    public static Date getDate(int year, int month, int day, int hour, int minute, int sec, int milli)
    {
    	setDefaultTimeZone();
    	java.sql.Timestamp ts = null;
    	
    	GregorianCalendar gc = new GregorianCalendar();
    	
    	gc.set(Calendar.YEAR, year);
    	gc.set(Calendar.MONTH, month-1); // 0 - based
    	gc.set(Calendar.DAY_OF_MONTH, day);
    	gc.set(Calendar.HOUR_OF_DAY, hour);
    	gc.set(Calendar.MINUTE, minute);
    	gc.set(Calendar.SECOND, sec);
    	gc.set(Calendar.MILLISECOND, milli);
    	
    	ts = new java.sql.Timestamp(gc.getTimeInMillis());
    	
    	return ts;
    }

    protected static String ss(int num)
    {
    	String s = String.valueOf(num);
    	if(num < 10) s = "0"+s;
    	return s;
    }

    protected static String sss(int num)
    {
    	String s = String.valueOf(num);
    	if(num < 100) s = "0"+s; // 0nn
    	if(num < 10) s= "0"+s; // should result in 00n
    	return s;
    }

    public static String getISODate(Date d)
    {
    	setDefaultTimeZone();
    	
    	GregorianCalendar gc = new GregorianCalendar();
    	gc.setTime(d);
    	
    	int n = 0;
    	String str = null;
    	
    	n = gc.get(Calendar.YEAR);
    	str = String.valueOf(n)+"-";
    	
    	n = gc.get(Calendar.MONTH)+1;
    	str += ss(n)+"-";
    	
    	n = gc.get(Calendar.DAY_OF_MONTH);
    	str += ss(n)+"T";
    	
    	n = gc.get(Calendar.HOUR_OF_DAY);
    	str += ss(n)+":";
    	
    	n = gc.get(Calendar.MINUTE);
    	str += ss(n)+":";
    	
    	n = gc.get(Calendar.SECOND);
    	str += ss(n)+".";
    	
    	n = gc.get(Calendar.MILLISECOND);
    	str += sss(n)+"Z";
    	
    	return str;
    }
    

    /**
     * Not universally valid, intended to parse iso date strings produced by getISODate().
     * 
     * @param str
     * @return
     */
    @SuppressWarnings("unused")
	public static Date parseISODate(String str)
    {
    	if(str == null) return null;
    	str = str.trim();
    	if(str.length()<9) return null;
    	
    	setDefaultTimeZone();
    	
    	Date d = null;
    	
    	int year = 0;
    	int month = 0;
    	int day = 0;
    	int hour = 0;
    	int minute = 0;
    	int sec = 0;
    	int milli = 0;
    	
    	String sa[] = str.split("T");
    	String s1 = null;
    	String sa2[] = null;
    	
    	s1 = sa[0];
    	if(s1.length()>8)
    	{
    		sa2 = s1.split("-");
    		if(sa2.length>2)
    		{
    			year = Integer.parseInt(sa2[0].trim());
    			month = Integer.parseInt(sa2[1].trim());
    			day = Integer.parseInt(sa2[2].trim());
    		}
    	}
    	//2014-06-01T03:02:13.552+00:00
    	if(sa.length>1)
    	{
    		s1 = sa[1].trim();
    		String tz = null;
    		
    		int mult = 1;
    		
    		int ind = s1.indexOf('+');
    		if(ind > -1)
    		{
    			tz = s1.substring(ind+1);
    			s1 = s1.substring(0, ind);
    		}
    		
    		ind = s1.indexOf('-');
    		if(ind > -1)
    		{
    			mult = -1;
    			tz = s1.substring(ind+1);
    			s1 = s1.substring(0, ind);    			
    		}
    		
    		if(s1.endsWith("Z") || s1.endsWith("z"))
    		{
    			tz = null;
    			s1 = s1.substring(0,s1.length()-1).trim();
    		}
    		
    		sa2 = s1.split(":");
    		if(sa2.length>2)
    		{
    			hour = Integer.parseInt(sa2[0].trim());
    			minute = Integer.parseInt(sa2[1].trim());
    			
    			double s = Double.parseDouble(sa2[2].trim());
    			sec = (int)s;
    			s -= sec;
    			milli = (int)(1000.001d*s);
    		}
    		
    		// TODO tz and mult were not used
    	}
    	
    	d = getDate(year,month,day,hour,minute,sec,milli);
    	
    	return d;
    }
}
