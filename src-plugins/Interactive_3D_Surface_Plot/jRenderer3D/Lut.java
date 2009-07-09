package jRenderer3D;
import ij.ImagePlus;
import ij.LookUpTable;

import java.awt.Color;
import java.awt.image.IndexColorModel;

class Lut {
	
	int [] colors;
	int [] origColors;
	
	Lut() {
		colors = new int[256];
		origColors = new int[256];
		
	}

	void setLut(int lutNr) {
		
		switch (lutNr) {
		case JRenderer3D.LUT_ORIGINAL:
			readOriginalColorLut();
			break;
		case JRenderer3D.LUT_GRAY:	 
			gray();
			break;
		case JRenderer3D.LUT_ORANGE: 
			orange();
			break;
		case JRenderer3D.LUT_BLACK: 
			black();
			break;
		case JRenderer3D.LUT_BLUE: 
			blue();
			break;
		case JRenderer3D.LUT_SPECTRUM:  
			spectrum();
			break;
		case JRenderer3D.LUT_FIRE:  
			fire();
			break;
		case JRenderer3D.LUT_THERMAL:  
			thermal();
			break;		
		}
	}
	
	
	protected int getLutColor(SurfacePlotData col) {
		int l = (int) (col.lum + 128);
		return colors[l];
	}
	
	protected void readLut(ImagePlus imp) {
		LookUpTable lut_ = imp.createLut();
		int mapSize = 0;
		java.awt.image.ColorModel cm = lut_.getColorModel();
		byte[] rLUT,gLUT,bLUT;
		if (cm instanceof IndexColorModel) {
			IndexColorModel m = (IndexColorModel)cm;
			mapSize = m.getMapSize();
			rLUT = new byte[mapSize];
			gLUT = new byte[mapSize];
			bLUT = new byte[mapSize];
			m.getReds(rLUT);
			m.getGreens(gLUT);
			m.getBlues(bLUT);
			
			for (int i=0; i<256; i++) {
				byte r = rLUT[i];
				byte g = gLUT[i];
				byte b = bLUT[i];
				
				colors[i] = origColors[i] = 0xff000000  | ((int)(r&0xFF) << 16) | ((int)(g&0xFF) <<8) | (int)(b&0xFF);
			}
		} 
		else {
			for (int i=0; i<256; i++) {
				colors[i] = origColors[i] = 0xff000000  | (i << 16) | (i <<8) | i;
			}
		}
	}
	
	private final int [] fireTable = { 0,0,31,0,0,31,0,0,33,0,0,35,0,0,37,0,0,39,0,0,41,0,0,43,0,0,45,0,0,47,0,0,49,0,0,52,0,0,54,0,0,57,0,0,59,0,0,62,
			0,0,64,0,0,67,0,0,70,0,0,73,0,0,76,0,0,79,0,0,82,0,0,85,0,0,88,0,0,92,2,0,96,3,0,99,5,0,102,7,0,105,10,0,108,13,0,112,
			15,0,116,17,0,119,20,0,122,22,0,126,25,0,130,28,0,134,31,0,138,33,0,141,35,0,145,38,0,149,41,0,152,43,0,156,46,0,160,49,0,164,52,0,168,55,0,171,
			58,0,175,61,0,178,64,0,181,67,0,184,70,0,188,73,0,191,76,0,195,78,0,198,81,0,202,85,0,205,88,0,209,91,0,212,94,0,216,98,0,218,101,0,220,104,0,221,
			107,0,222,110,0,223,113,0,224,116,0,225,119,0,226,122,0,225,126,0,224,129,0,222,133,0,219,136,0,217,140,0,214,143,0,212,146,0,209,148,0,206,150,0,202,153,0,198,
			155,0,193,158,0,189,160,0,185,162,0,181,163,0,177,165,0,173,166,0,168,168,0,163,170,0,159,171,0,154,173,0,151,174,0,146,176,0,142,178,0,137,179,0,133,181,0,129,
			182,0,125,184,0,120,186,0,116,188,0,111,189,0,107,191,0,103,193,0,98,195,0,94,196,1,89,198,3,85,200,5,80,202,8,76,204,10,71,205,12,67,207,15,63,209,18,58,
			210,21,54,212,24,49,213,27,45,215,31,40,217,34,36,218,37,31,220,40,27,222,44,22,224,48,17,226,51,12,227,54,8,229,58,5,231,61,4,233,65,3,234,68,2,236,72,1,
			238,75,0,240,79,0,241,82,0,243,85,0,245,89,0,247,92,0,249,95,0,250,99,0,251,102,0,252,105,0,253,107,0,253,110,0,253,112,0,254,115,0,255,117,0,255,119,0,
			255,122,0,255,125,0,255,127,0,255,129,0,255,131,0,255,134,0,255,136,0,255,138,0,255,140,0,255,142,0,255,145,0,255,147,0,255,149,0,255,151,0,255,153,0,255,155,0,
			255,157,0,255,159,0,255,161,0,255,163,0,255,166,0,255,168,0,255,169,0,255,171,0,255,173,0,255,176,0,255,178,0,255,180,0,255,182,0,255,184,0,255,186,0,255,189,0,
			255,191,0,255,193,0,255,195,0,255,197,0,255,199,0,255,201,0,255,203,0,255,205,0,255,208,0,255,210,0,255,212,0,255,213,0,255,215,0,255,217,0,255,219,0,255,220,0,
			255,222,0,255,224,0,255,226,0,255,228,0,255,230,0,255,232,1,255,234,3,255,236,6,255,238,10,255,239,14,255,241,18,255,243,22,255,244,27,255,246,31,255,248,37,255,248,43,
			255,249,50,255,250,58,255,251,66,255,252,74,255,253,81,255,254,88,255,255,95,255,255,102,255,255,108,255,255,115,255,255,122,255,255,129,255,255,136,255,255,142,255,255,148,255,255,154,
			255,255,161,255,255,167,255,255,174,255,255,180,255,255,185,255,255,192,255,255,198,255,255,204,255,255,210,255,255,215,255,255,221,255,255,225,255,255,228,255,255,231,255,255,234,255,255,236,
			255,255,239,255,255,242,255,255,244,255,255,247,255,255,249,255,255,251,255,255,253,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255
	};
	
	private final int [] tempTable = { 70,0,115,70,0,115,70,0,116,70,0,118,70,0,120,70,0,122,70,0,124,70,0,126,70,0,128,70,0,131,70,0,133,70,0,136,70,0,139,70,0,141,70,0,144,70,0,147,
			70,0,151,70,0,154,70,0,157,70,0,160,70,0,164,70,0,167,70,0,170,70,0,174,70,0,177,70,0,181,70,0,184,70,0,188,70,0,194,70,0,200,70,0,206,70,0,211,
			70,0,217,70,0,222,70,0,227,70,0,232,70,0,236,70,0,240,70,0,244,69,0,248,69,0,251,68,0,253,67,2,255,66,5,255,64,9,255,63,13,255,61,17,255,59,22,255,
			57,27,255,55,32,255,53,38,255,51,44,255,48,50,255,45,57,255,43,63,255,40,70,255,37,77,255,34,84,255,31,91,255,28,98,255,26,106,255,23,113,254,20,121,253,17,128,252,
			14,136,251,12,144,250,9,152,248,6,160,247,4,168,246,2,176,245,0,183,243,0,191,242,0,198,241,0,205,240,0,212,239,0,218,238,0,224,237,0,230,236,0,235,235,0,240,235,
			0,245,235,0,249,234,0,253,234,1,255,234,4,255,234,7,255,234,11,255,235,16,255,236,21,255,237,27,255,238,32,255,239,39,255,240,45,255,241,52,255,243,60,255,244,68,255,246,
			76,255,247,84,255,249,92,255,250,101,255,252,109,255,253,117,255,254,126,255,254,134,255,254,143,255,254,152,255,254,160,255,254,168,255,254,176,255,254,184,255,254,192,255,254,199,255,254,
			206,255,254,213,255,254,219,255,254,225,255,254,231,255,254,236,255,254,240,255,254,244,255,254,247,255,254,250,255,254,252,254,254,253,254,254,254,254,254,254,254,252,253,254,249,252,254,246,
			251,254,243,249,254,239,246,254,236,243,254,231,240,254,227,237,254,223,233,254,218,228,254,213,223,255,208,219,255,203,214,255,198,208,255,192,203,255,187,196,255,181,190,255,175,184,255,169,
			178,255,163,171,255,157,165,255,151,158,255,145,151,255,138,144,255,132,138,255,126,129,255,118,120,255,110,112,255,102,103,255,94,95,255,87,87,255,79,79,255,72,71,255,65,64,255,58,
			57,255,51,51,255,45,45,255,38,39,255,32,35,255,27,30,255,22,27,255,17,24,255,13,21,255,8,20,255,5,19,255,2,19,255,1,21,255,1,23,255,1,27,255,1,32,255,1,
			37,255,1,44,255,1,51,255,1,59,255,1,68,255,1,77,255,1,86,255,1,97,255,3,107,255,5,118,255,8,125,255,10,131,255,12,137,255,14,144,255,16,150,255,17,156,255,19,
			162,255,21,168,255,23,174,255,25,180,255,26,185,255,28,191,255,30,197,254,31,202,254,33,207,252,34,212,252,36,217,250,37,222,249,38,227,248,39,231,246,40,235,245,41,238,243,42,
			242,241,43,245,239,43,248,237,43,251,235,44,253,233,44,255,229,44,255,226,44,255,222,43,255,218,43,255,213,42,255,208,42,255,203,41,255,198,40,255,192,40,255,187,39,255,181,38,
			255,175,37,255,169,36,255,162,34,255,156,33,255,149,32,255,143,31,255,136,30,255,129,28,255,122,27,255,116,25,255,109,24,255,102,23,255,95,21,255,89,20,255,82,19,255,76,17,
			255,70,16,255,63,15,255,57,13,255,51,12,255,45,11,255,40,10,255,35,9,255,29,7,255,25,6,255,20,6,255,16,5,255,12,4,255,8,3,255,5,3,255,2,2,255,2,2
	};
	
	
	void readOriginalColorLut() {
		for (int i=0; i<256; i++) {
			colors[i] = origColors[i];
		}
	}
	
	void spectrum() {
		Color c;
		for (int i=0; i<256; i++) {
			c = Color.getHSBColor(i/255f, 1f, 1f);
			int r = c.getRed();
			int g = c.getGreen();
			int b = c.getBlue();
			colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
		}
	}
	
	void gray() {
		for (int i=0; i<256; i++) {
			colors[i] = 0xff000000  | (i << 16) | (i <<8) | i;
		}
	}
	
	void fire() {
		for (int i=0; i<256; i++) {
			int r = fireTable[3*i];
			int g = fireTable[3*i+1];
			int b = fireTable[3*i+2];
			colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
		}
	}
	
	void thermal() {
		for (int i=0; i<256; i++) {
			int r = tempTable[3*i];
			int g = tempTable[3*i+1];
			int b = tempTable[3*i+2];
			colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
		}
	}
	
	void orange() {
		for (int i=0; i<256; i++) {
			int r = 255;
			int g = 184;
			int b = 0;
			colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
		}
	}
	
	void blue() {
		for (int i=0; i<256; i++) {
			int r = 75;
			int g = 75;
			int b = 255;
			colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
		}
	}
	
	void black() {
		for (int i=0; i<256; i++) {
			colors[i] = 0xff000000;
		}
	}
}