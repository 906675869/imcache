/*
 * Copyright (C) 2015 Cetsoft, http://www.cetsoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Author : Yusuf Aytas
 * Date   : Aug 18, 2015
 */
package com.cetsoft.imcache.cache;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cetsoft.imcache.cache.search.filter.Filter;

public class CollectionItemTest {
    
    @Mock
    Filter filter;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void filter() {
        Item item = new Item(2);
        List<Item> items = new ArrayList<Item>();
        items.add(item);
        CollectionItem<Item> collectionItem = new CollectionItem<Item>(items);
        doReturn(items).when(filter).filter(anyList());
        Collection<Item> filteredItems = collectionItem.filter(filter);
        assertTrue(filteredItems.contains(item));
    }
    
    @SuppressWarnings("unused")
    private static class Item {
        int value;
        
        public Item(int value) {
            this.value = value;
        }
    }
}
