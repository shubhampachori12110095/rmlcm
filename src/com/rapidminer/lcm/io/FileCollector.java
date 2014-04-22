/*
	This file is part of jLCM
	
	Copyright 2013 Martin Kirchgessner, Vincent Leroy, Alexandre Termier, Sihem Amer-Yahia, Marie-Christine Rousset, Université Joseph Fourier and CNRS

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0
	 
	or see the LICENSE.txt file joined with this program.

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/


package com.rapidminer.lcm.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;


/**
 * a thread-unsafe PatternsCollector that write to the path provided at instanciation
 * @see MultiThreadedFileCollector
 */
public class FileCollector implements PatternsCollector {
	
	// this should be profiled and tuned !
	protected static final int BUFFER_CAPACITY = 4096;
	
	protected long collected = 0;
	protected long collectedLength = 0;
	protected FileOutputStream stream;
	protected FileChannel channel;
	protected ByteBuffer buffer;
	protected static final Charset charset = Charset.forName("ASCII");
	
	public FileCollector(final String path) throws IOException {
		File file = new File(path);
		
		if (file.exists()) {
			System.err.println("Warning : overwriting output file "+path);
		}
		
		stream = new FileOutputStream(file, false);
		channel = stream.getChannel();
		
		buffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY);
		buffer.clear();
	}
	
	public void collect(final int support, final int[] pattern) {
		putInt(support);
		safePut((byte) '\t'); // putChar('\t') would append TWO bytes, but in ASCII we need only one
		
		boolean addSeparator = false;
		for (int item : pattern) {
			if (addSeparator) {
				safePut((byte) ' ');
			} else {
				addSeparator = true;
			}
			
			putInt(item);
		}
		
		safePut((byte) '\n');
		this.collected++;
		this.collectedLength += pattern.length;
	}
	
	protected void putInt(final int i) {
		try {
			byte[] asBytes = Integer.toString(i).getBytes(charset);
			buffer.put(asBytes);
		} catch (BufferOverflowException e) {
			flush();
			putInt(i);
		}
	}
	
	protected void safePut(final byte b) {
		try {
			buffer.put(b);
		} catch (BufferOverflowException e) {
			flush();
			safePut(b);
		}
	}
	
	protected void flush() {
		try {
			buffer.flip();
			channel.write(buffer);
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	public long close() {
		try {
			flush();
			channel.close();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		
		return this.collected;
	}

	public int getAveragePatternLength() {
		if (this.collected == 0) {
			return 0;
		} else {
			return (int) (this.collectedLength / this.collected);
		}
	}
	
	/**
	 * @return how many patterns have been written so far
	 */
	public long getCollected() {
		return collected;
	}
	
	/**
	 * @return sum of collected patterns' lengths
	 */
	public long getCollectedLength() {
		return collectedLength;
	}
}
