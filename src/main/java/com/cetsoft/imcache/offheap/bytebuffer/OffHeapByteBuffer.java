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
* Date   : Sep 22, 2013
*/
package com.cetsoft.imcache.offheap.bytebuffer;

import java.nio.BufferOverflowException;
import java.util.concurrent.atomic.AtomicInteger;

import com.cetsoft.imcache.concurrent.lock.StripedReadWriteLock;

/**
 * The Class OffHeapByteBuffer.
 */
public class OffHeapByteBuffer implements OffHeapStore{
	
	/** The capacity. */
	private volatile long capacity;
	
	/** The direct byte buffer. */
	private DirectByteBuffer directByteBuffer;
	
	/** The read write lock. */
	private StripedReadWriteLock readWriteLock;
	
	/** The Constant DEFAULT_CONCURRENCY_LEVEL. */
	public final static int DEFAULT_CONCURRENCY_LEVEL = 4;
	
	/** The Constant POINTER_SIZE. */
	private final static int POINTER_SIZE = 5;
	
	/** The offset. */
	private AtomicInteger offset = new AtomicInteger(0);
	
	/** The used memory. */
	private AtomicInteger usedMemory = new AtomicInteger(0);
	
	/** The dirty memory. */
	private AtomicInteger dirtyMemory = new AtomicInteger(0);
	
	/** The Constant USED. */
	private final static byte USED = 1;
	
	/** The Constant FREE. */
	private final static byte FREE = 0;
	
	/** The Constant DIRTY. */
	private final static byte DIRTY = -1;
	
	/**
	 * Instantiates a new off heap byte buffer.
	 *
	 * @param capacity the capacity
	 */
	public OffHeapByteBuffer(int capacity){
		this(capacity, DEFAULT_CONCURRENCY_LEVEL);
	}
	
	/**
	 * Instantiates a new off heap byte buffer.
	 *
	 * @param capacity the capacity
	 * @param concurrencyLevel the concurrency level
	 */
	public OffHeapByteBuffer(int capacity, int concurrencyLevel){
		this.capacity = capacity;
		directByteBuffer = new DirectByteBuffer(capacity);
		readWriteLock = new StripedReadWriteLock(concurrencyLevel);
	}

	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.offheap.bytebuffer.OffHeapStore#retrieve(com.cetsoft.imcache.offheap.bytebuffer.Pointer)
	 */
	public byte[] retrieve(Pointer pointer) {
		readWriteLock.readLock(pointer.getPosition());
		try{
			byte [] header = new byte[POINTER_SIZE];
			directByteBuffer.get(pointer.getPosition(), header, 0, POINTER_SIZE);
			int length = header(header);
			byte [] payload = new  byte[length];
			directByteBuffer.get(pointer.getPosition()+POINTER_SIZE, payload, 0, length);
			pointer.setAccessTime(System.currentTimeMillis());
			return payload;
		}finally{
			readWriteLock.readUnlock(pointer.getPosition());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.offheap.bytebuffer.OffHeapStore#remove(com.cetsoft.imcache.offheap.bytebuffer.Pointer)
	 */
	public byte[] remove(Pointer pointer) {
		readWriteLock.writeLock(pointer.getPosition());
		try{
			byte [] payload = retrieve(pointer);
			markAsDirty(pointer.getPosition());
			return payload;
		}finally{
			readWriteLock.writeUnlock(pointer.getPosition());
		}
	}

	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.offheap.bytebuffer.OffHeapStore#store(byte[])
	 */
	public Pointer store(byte[] payload) {
		Allocation allocation = allocate(payload);
		Pointer pointer = store(allocation, payload);
		return pointer;
	}
	
	/**
	 * Stores the payload by the help of allocation.
	 *
	 * @param allocation the allocation
	 * @param payload the payload
	 * @return the pointer
	 */
	public Pointer store(Allocation allocation, byte[] payload) {
		usedMemory.addAndGet(allocation.getLength());
		Pointer pointer = new Pointer(allocation.getOffset(), this);
		byte [] header = header(payload.length);
		directByteBuffer.put(allocation.getOffset(), header, 0, POINTER_SIZE);
		directByteBuffer.put(allocation.getOffset()+POINTER_SIZE, payload, 0, allocation.getLength()-POINTER_SIZE);
		return pointer;
	}
	
	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.offheap.bytebuffer.OffHeapStore#update(com.cetsoft.imcache.offheap.bytebuffer.Pointer, byte[])
	 */
	public Pointer update(Pointer pointer, byte[] payload) {
		readWriteLock.writeLock(pointer.getPosition());
		try{
			byte[] exPayload = retrieve(pointer);
			if(exPayload.length>=payload.length){
				// Note that updated payload should be always less than exPayload,
				// otherwise new location is allocated.
				dirtyMemory.addAndGet(exPayload.length-payload.length);
				Allocation allocation = new Allocation(pointer.getPosition(), payload.length+POINTER_SIZE);
				return store(allocation,payload);
			}else{
				dirtyMemory.addAndGet(exPayload.length+POINTER_SIZE);
				markAsDirty(pointer.getPosition());
				return store(payload);
			}
		}finally{
			readWriteLock.writeUnlock(pointer.getPosition());
		}	
	}
	
	/**
	 * Calculates the header bytes from the length.
	 *
	 * @param length the length
	 * @return the byte[]
	 */
	protected byte[] header(int length){
		byte[] header = new byte[5];
		header[0] = USED;
		header[1] = (byte) ((length >>> 24) & 0xFF);
		header[2] = (byte) ((length >>> 16) & 0xFF);
		header[3] = (byte) ((length >>> 8) & 0xFF);
		header[4] = (byte) ((length >>> 0) & 0xFF);
		return header;
	}
	
	/**
	 * Calculates the header length from the bytes.
	 *
	 * @param header the header
	 * @return the int
	 */
	protected int header(byte [] header){
		if(header[0]==DIRTY){
			throw new OffHeapByteBufferException("Object is dirty!");
		}
		else if(header[0]==FREE){
			throw new OffHeapByteBufferException("Object is free!");
		}
		else if(header[0]!=USED){
			throw new OffHeapByteBufferException("Wrong header!");
		}
		int length = ((header[1]) << 24) | ((header[2] & 0xff) << 16) | ((header[3] & 0xff) << 8) | ((header[4] & 0xff));
		return length;
	}
	
	/**
	 * Mark as dirty.
	 *
	 * @param offset the offset
	 */
	protected void markAsDirty(int offset){
		byte[] dirtyMark = {DIRTY};
		directByteBuffer.put(offset, dirtyMark, 0, 1);
	}

	/**
	 * Allocates memory for the payload.
	 *
	 * @param payload the payload
	 * @return the allocation
	 */
	protected Allocation allocate(byte[] payload) {
		int payloadLength = payload.length+POINTER_SIZE;
		int allocationOffset = offset.addAndGet(payloadLength);
		if(this.capacity<allocationOffset){
			throw new BufferOverflowException();
		}
		Allocation allocation = new Allocation(allocationOffset-payloadLength, payloadLength);
		return allocation;
	}
	
	/**
	 * Frees the buffer.
	 */
	public void free(){
		directByteBuffer.free();
	}
	
	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.offheap.bytebuffer.OffHeapStore#dirtyMemory()
	 */
	public long dirtyMemory() {
		return dirtyMemory.get();
	}

	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.offheap.bytebuffer.OffHeapStore#usedMemory()
	 */
	public long usedMemory() {
		return usedMemory.get();
	}

	/* (non-Javadoc)
	 * @see com.cetsoft.imcache.offheap.bytebuffer.OffHeapStore#freeMemory()
	 */
	public long freeMemory() {
		return capacity-(dirtyMemory()+usedMemory());
	}
	
	/**
	 * The Class Allocation.
	 */
	private static class Allocation {
		
		/** The offset. */
		private int offset;
		
		/** The length. */
		private int length;
		
		/**
		 * Instantiates a new allocation.
		 *
		 * @param offset the offset
		 * @param length the length
		 */
		public Allocation(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}
		
		/**
		 * Gets the offset.
		 *
		 * @return the offset
		 */
		public int getOffset() {
			return offset;
		}
		
		/**
		 * Gets the length.
		 *
		 * @return the length
		 */
		public int getLength() {
			return length;
		}
	}
}
