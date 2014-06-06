package jpdf.decode;

/*
 * $Id: Predictor.java,v 1.2 2007/12/20 18:33:33 rbair Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

import java.nio.ByteBuffer;
import java.io.IOException;

import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.parser.ParserException;

/**
 * The abstract superclass of various predictor objects that undo well-known
 * prediction algorithms.
 */
public abstract class Predictor {
    /** well known algorithms */
    public static final int TIFF = 0;
    public static final int PNG = 1;
    
    /** the algorithm to use */
    private int algorithm;
    
    /** the number of colors per sample */
    private int colors = 1;
    
    /** the number of bits per color component */
    private int bpc = 8;
    
    /** the number of columns per row */
    private int columns = 1;
    
    /** 
     * Create an instance of a predictor.  Use <code>getPredictor()</code>
     * instead of this.
     */
    protected Predictor(int algorithm) {
        this.algorithm = algorithm;
    }
    
    /**
     * Actually perform this algorithm on decoded image data.
     * Subclasses must implement this method
     */
    public abstract ByteBuffer unpredict(ByteBuffer imageData)
        throws IOException;
    
    /**
     * Get an instance of a predictor
     *
     * @param params the filter parameters
     * @throws ParserException 
     */
    public static Predictor getPredictor(PdfDictionary params)
        throws IOException, ParserException
    {
        // get the algorithm (required)
        PdfObject algorithmObj = params.get("Predictor");
        if (algorithmObj == null) {
            // no predictor
            return null;
        }
        int algorithm = Integer.parseInt(((PdfNumber) algorithmObj).toString());
        System.out.println("algorithm: " + algorithm);
    
        // create the predictor object
        Predictor predictor = null;
        switch (algorithm) {
            case 1:
                // no predictor
                return null;
            case 2:
                throw new ParserException("Tiff Predictor not supported");
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                predictor = new PNGPredictor();
                break;
            default:
                throw new ParserException("Unknown predictor: " + algorithm);
        }
        
        // read the colors (optional)
        /*
        PDFObject colorsObj = params.getDictRef("Colors");
        if (colorsObj != null) {
            predictor.setColors(colorsObj.getIntValue());
        }
        
        // read the bits per component (optional)
        PDFObject bpcObj = params.getDictRef("BitsPerComponent");
        if (bpcObj != null) {
            predictor.setBitsPerComponent(bpcObj.getIntValue());
        }
        */
        
        // read the columns (optional)
        PdfObject columnsObj = params.get("Columns");
        if (columnsObj != null) {
            predictor.setColumns(Integer.parseInt(((PdfNumber) columnsObj).toString()));
        }
        
        // all set
        return predictor;
    }
    
    /**
     * Get the algorithm in use
     *
     * @return one of the known algorithm types
     */
    public int getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Get the number of colors per sample
     */
    public int getColors() {
        return colors;
    }
    
    /**
     * Set the number of colors per sample
     */
    protected void setColors(int colors) {
        this.colors = colors;
    }
    
    /**
     * Get the number of bits per color component
     */
    public int getBitsPerComponent() {
        return bpc;
    }
    
    /**
     * Set the number of bits per color component
     */
    public void setBitsPerComponent(int bpc) {
        this.bpc = bpc;
    }
    
    /**
     * Get the number of columns
     */
    public int getColumns() {
        return columns;
    }
    
    /**
     * Set the number of columns
     */
    public void setColumns(int columns) {
        this.columns = columns;
    }
}