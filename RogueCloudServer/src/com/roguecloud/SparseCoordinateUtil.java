/*
 * Copyright 2019 IBM Corporation
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
*/

package com.roguecloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.roguecloud.WorldGenFromFile.Entry;
import com.roguecloud.utils.SimpleMap;

/** Given a map with a small number of blocks, with each block containing a set of contiguous tiles, this utility
 * will find a random point in the blocks.
 *
 * Of note:
 * - All tiles have an equal chance of being selected.
 * - Memory requirements and CPU requirements are significantly reduced versus a naive implementation
 * - Immutable
 **/
public class SparseCoordinateUtil {

	private final List<Row> rows = new ArrayList<>();
	private final int totalSize;
	private final Random rand = new Random();
	
	public SparseCoordinateUtil(SimpleMap<Entry> eMap, String ch) {
		
		int totalSize = 0;
		
		for(int currRow = 0; currRow < eMap.getYSize(); currRow++) {

			List<HorizontalBlock> blocks = new ArrayList<>();
			int currBlockStartX = -1;
			
			for(int x = 0; x < eMap.getXSize(); x++) {
				
				
				Entry e = eMap.getTile(x,  currRow);
				if(e != null && e.getMapping() != null && e.getMapping().getLetter().equals(ch)) {
					
					if(currBlockStartX == -1) {
						currBlockStartX = x;
					}
					
				} else if(currBlockStartX != -1) {
					blocks.add(new HorizontalBlock(currBlockStartX, x-1));
					currBlockStartX = -1;
				}				
				
			}
			
			if(currBlockStartX != -1) {
				blocks.add(new HorizontalBlock(currBlockStartX, eMap.getXSize()-1 ));
				currBlockStartX = -1;
			}
			
			if(blocks.size() > 0) {
				Row newRow = new Row(currRow, blocks);
				this.rows.add(newRow);
				totalSize += newRow.xSize;
			}
		}
		
		this.totalSize = totalSize;
	}
	
	public int getTotalSize() {
		return totalSize;
	}
	
	public Position generateRandomCoordinate() {
		
		Row match = null;
		{
			int point = rand.nextInt(totalSize);
			int total = 0;
			// Which row does the point correspond to
			for(Row r : rows) {
				total += r.xSize;
				if(point < total) {
					match = r;
					break;
				}
			}
			
			if(match == null) {
				match = rows.get(rows.size()-1);
			}
		}
		
		HorizontalBlock blockMatch = null;
		{
			int point = rand.nextInt(match.xSize);
			int total = 0;
			
			// Which block does the point correspond to
			for(HorizontalBlock b : match.getBlocks()) {
				total += b.getSize();
				if(point < total) {
					blockMatch = b;
					break;
				}
			}
			
			if(blockMatch == null) {
				blockMatch = match.getBlocks().get(match.getBlocks().size()-1);
			}
		}
		

		int x = rand.nextInt(blockMatch.getSize()) + blockMatch.getStartX();
		
		return new Position(x, match.getY());
	}
	
	
	
	private static class Row {
		final int y;
		final int xSize; // # number of tiles in the row
		
		private final List<HorizontalBlock> blocks;
		
		public Row(int y, List<HorizontalBlock> blocks) {
			this.y = y;
			this.blocks = blocks;
			
			int xSize = 0;
			for(HorizontalBlock b : blocks) {
				xSize += (b.endX-b.startX)+1;
			}
			
			this.xSize = xSize;
		}
		
		public List<HorizontalBlock> getBlocks() {
			return blocks;
		}
		
		public int getY() {
			return y;
		}
	
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			sb.append(""+y+": ");
			
			for(HorizontalBlock hb : blocks) {
				sb.append(hb.toString()+" ");
				
			}
			
			return sb.toString().trim();
		}
	}
	
	private static class HorizontalBlock {
		final int startX;
		final int endX; // inclusive
		
		public HorizontalBlock(int startX, int endX) {
			this.startX = startX;
			this.endX = endX;
		}
		
		
		public int getStartX() {
			return startX;
		}
		
		@SuppressWarnings("unused")
		public int getEndX() {
			return endX;
		}
		
		public int getSize() {
			return (endX-startX)+1;
		}
		
		@Override
		public String toString() {
			return "["+startX+" to "+" "+endX+"]";
		}
	}
}


