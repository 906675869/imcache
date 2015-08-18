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
* Date   : Sep 28, 2013
*/
package com.cetsoft.imcache.cache.search;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class ConcurrentQueryExecuter.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ConcurrentQueryExecuter<K,V> extends SimpleQueryExecuter<K, V>{

	/**
	 * Instantiates a new concurrent query executer.
	 */
	public ConcurrentQueryExecuter() {
		indexes = new ConcurrentHashMap<Attribute, Map<Object,K>>();
	}
	
	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.cache.search.SimpleQueryExecuter#addIndex(com.cetsoft.imcache.cache.search.CacheIndex)
	 */
	public void addIndex(CacheIndex index) {
		indexes.put(index.getIndex(), new ConcurrentHashMap<Object, K>());
	}
}
