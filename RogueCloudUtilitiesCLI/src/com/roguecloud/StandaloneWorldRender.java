package com.roguecloud;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.roguecloud.WorldGeneration.GenerateWorldResult;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.resources.Resources;

/** A standalone CLI utility that will render a given section of the map to a .PNG file. */
public class StandaloneWorldRender {

	public static void main(String[] args) throws IOException {
		
		if(args.length != 5) {
			System.out.println("* Required arguments \"path to Rogue Cloud universe directory\"  \"x coord\" \"y coord\" \"width\" \"height\" ");
			System.out.println("  Example: C:\\Rogue-Cloud\\Git\\RogueCloudServer\\WebContent\\universe\\ 0 0 100 100");
			return;
		}
		
		int xCoordParam = Integer.parseInt(args[1]);
		int yCoordParam = Integer.parseInt(args[2]);
		int widthParam  = Integer.parseInt(args[3]);
		int heightParam  = Integer.parseInt(args[4]);
		
		// Generate the map.txt from the PNG
		PngMain.main(args);
		
		
		File outputFolder = new File(args[0]);
			
		
	    File outputfile = new File(outputFolder, "preview.png");
	    		
		UniqueIdGenerator uig = new UniqueIdGenerator();
		ServerInstance si = new ServerInstance();
		
		GenerateWorldResult  gwr = WorldGeneration.generateDefaultWorld(si, uig, null);
		
		RCArrayMap m = gwr.getNewMap();

		int xOffsetInTiles = xCoordParam;
		int yOffsetInTiles = yCoordParam;
		
		int widthInTiles = widthParam; // m.getXSize();
		int heightInTiles = heightParam; // m.getYSize();
		
		BufferedImage outputImage = new BufferedImage(widthInTiles*32, heightInTiles*32, BufferedImage.TYPE_INT_ARGB);
		Graphics g = outputImage.getGraphics();
		
		ImageCache cache = new ImageCache();
		
		System.out.println();
		
		for(int y = 0; y < heightInTiles; y++) {
		
			for(int x = 0; x < widthInTiles; x++) {
				
				Tile t = m.getTile(x + xOffsetInTiles, y + yOffsetInTiles);
				TileType[] ttArr = t.getTileTypeLayersForBrowserPresentation(0);

				if(ttArr != null && ttArr.length > 0) {
					
					for(int z = ttArr.length-1; z>= 0; z--) {

						TileType tt = ttArr[z];
						
						BufferedImage bi = cache.getTile(tt.getNumber(), tt.getRotation());
						
						g.drawImage(bi, x*32, y*32, null);
						
						bi.getGraphics().dispose();
						
					}
					
				}
				
			}
			System.out.print("#");

			int percent = (int)((100*y)/heightInTiles);
			if( (percent+1) % 10 == 0 && percent > 0) {
				if(percent+1 == 50) {
					System.out.println();
				} else {
					System.out.print(" ");	
				}
				
			}
		}
		System.out.println();
		
		System.out.println("* Writing file to "+outputfile.getPath());

	    ImageIO.write(outputImage, "png", outputfile);
		
	}


	/** Cache the loaded images, so that we only need to load them once. */
	private static class ImageCache {
		
		private final Map<Integer, CacheEntry> imageMap = new HashMap<>();

		
		public BufferedImage getTile(int tileNumber, int rotation) throws IOException {
			
			int key = tileNumber*1000+rotation;
			CacheEntry ce = imageMap.get(key);
			if(ce != null) { return ce.bi; } 			
			
			BufferedImage img;
			img = ImageIO.read(new ByteArrayInputStream(Resources.getInstance().getFile("tiles/"+tileNumber+".png")));
			
			BufferedImage outputImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D) outputImage.getGraphics();
			g2d.rotate(Math.toRadians(rotation), 16, 16);
			g2d.drawImage(img, 0, 0, null);

			ce = new CacheEntry(outputImage);
			imageMap.put(key, ce);
			
			return outputImage;		

		}

		/** Wrapper for image */
		class CacheEntry {
			BufferedImage bi;

			public CacheEntry(BufferedImage bi) {
				this.bi = bi;
			}			
		}
	}
	
	
}
