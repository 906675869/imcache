/*
* Copyright (C) 2013 Cetsoft, http://www.cetsoft.com
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Library General Public
* License as published by the Free Software Foundation; either
* version 2 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Library General Public License for more details.
*
* You should have received a copy of the GNU Library General Public
* License along with this library; if not, write to the Free
* Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
* 
* Author : Yusuf Aytas
* Date   : Nov 8, 2013
*/
package com.cetsoft.imcache.cache.search.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class GTFilter is used to retrieve items
 * greater than the given value.
 */
public class GTFilter extends ArithmeticFilter {

	/**
	 * Instantiates a new gT filter.
	 *
	 * @param attributeName the attribute name
	 * @param value the value
	 */
	public GTFilter(String attributeName, Object value) {
		super(attributeName, value);
	}

	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.cache.search.filter.Filter#filter(com.cetsoft.imcache.cache.search.index.CacheIndex)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Object> filter(List<Object> objects) {
		List<Object> result = new ArrayList<Object>();
		for (Object object : objects) {
			Comparable objectValue = (Comparable)getAttributeValue(object);
			if(objectValue.compareTo(value)>0){
				result.add(object);
			}
		}
		return result;
	}

}
