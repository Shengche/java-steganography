package Steganography.Logic;

import Steganography.Modals.AlertBox;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GifSteganography extends BaseSteganography{

    private BufferedImage[] frames;
    private IIOMetadata[] metadatas;
    private int delayMS;
    private int i=0,j=0,k=0;

    // Constructor
    public GifSteganography(File input, boolean isEncrypted, boolean isCompressed) throws IOException{
        this.isEncrypted = isEncrypted;
        this.isCompressed = isCompressed;
        this.frames = Metadata.getFrames(input);
        this.metadatas = Metadata.getMetadatas(input);
        this.delayMS = Metadata.getDelayMS(input);
        this.pixelsPerByte = 8;
    }
    public GifSteganography(File input) throws IOException{ this(input, false, false); }

    private void writeHeader(byte[] header){
        for(byte b : header)
            hideByte(b);
    }

    public byte[] getHeader(){
        reset();
        int b;
        List<Byte> header = new ArrayList<>();
        do{
            b = revealByte();
            header.add((byte) b);
        }while(b != (byte) '!');
        return Helpers.toByteArray(header);
    }

    public void encode(byte [] message, File output){
        try{
            this.writeHeader(this.setHeader(message));
            for(byte b : message)
                hideByte(b);
            ImageOutputStream ios = new FileImageOutputStream(output);
            ColorModel cm = this.frames[0].getColorModel();
            ImageTypeSpecifier imageType = new ImageTypeSpecifier(cm, cm.createCompatibleSampleModel(1, 1));
            GifSequenceWriter writer = new GifSequenceWriter(ios, imageType, this.delayMS, true);
            for(int x=0; x<this.frames.length; x++)
              writer.writeToSequence(this.frames[x], this.metadatas[x]);
            writer.close();
            ios.close();
            System.out.println("Message hidden inside "+ output.getName());
        }catch(IOException e) {
            e.printStackTrace();
            AlertBox.error("Error while encoding", e.getMessage());
        }
    }
    public void encode(File doc, File output){
        try{
            this.writeHeader(this.setHeader(doc));
            FileInputStream fis = new FileInputStream(doc);
            byte[] buffer = new byte[1024];
            int read = 0;
            while( ( read = fis.read( buffer ) ) > 0 ){
                for(byte b : buffer) {
                    hideByte(b);
                }
            }
            ImageOutputStream ios = new FileImageOutputStream(output);
            ColorModel cm = this.frames[0].getColorModel();
            ImageTypeSpecifier imageType = new ImageTypeSpecifier(cm, cm.createCompatibleSampleModel(1, 1));
            GifSequenceWriter writer = new GifSequenceWriter(ios, imageType, this.delayMS, true);
            for(int x=0; x<this.frames.length; x++)
              writer.writeToSequence(this.frames[x], this.metadatas[x]);
            writer.close();
            ios.close();
        }catch(IOException e) {
            e.printStackTrace();
            AlertBox.error("Error while encoding", e.getMessage());
        }
    }

    public void decode(File file){
        try{
            reset();
            this.setSecretInfo(new HiddenData(this.getHeader()));
            int pos = 0;
            int b;
            FileOutputStream fos = new FileOutputStream(file);
            do{
                b = revealByte();
                fos.write((byte)b);
                pos++;
            }while(pos<secretInfo.length);
            fos.close();
            System.out.println("Secret file saved to "+ file.getName());
        }catch(IOException e) {
            e.printStackTrace();
            AlertBox.error("Error while decoding", e.getMessage());
        }
    }

    private int embed(int pixel, char c){
        String before = String.format("%8s", Integer.toBinaryString(pixel)).replace(' ','0');
        String after = before.substring(0,7)+c;
        return Integer.parseInt(after,2);
    }

    private void hideByte(byte b){
        int[] pixel = new int[4];
        String currentByte;
        for(int l=0; l<8; l++){
            WritableRaster raster = this.frames[k].getRaster();
            raster.getPixel(j,i,pixel);
            currentByte = String.format("%8s",Integer.toBinaryString(b)).replace(' ', '0');
            currentByte = currentByte.substring(currentByte.length()-8, currentByte.length());
            pixel[0] = embed(pixel[0], currentByte.charAt(l));
            raster.setPixel(j,i,pixel);
            increment();
        }
    }

    private byte revealByte(){
        int pixel[] = new int[4];
        int b;
        String currentByte;
        StringBuilder bit = new StringBuilder();
        for(int l=0; l<8; l++){
            Raster raster = this.frames[k].getRaster();
            raster.getPixel(j,i,pixel);
            currentByte = String.format("%8s", Integer.toBinaryString(pixel[0])).replace(" ", "0");
            currentByte = currentByte.substring(currentByte.length()-8, currentByte.length());
            bit.append(currentByte.charAt(7));
            increment();
        }
        b = Integer.parseInt(bit.toString(),2);
        return (byte) b;
    }

    private void increment(){
        this.j++;
        if(this.j == this.frames[this.k].getWidth()){ this.j=0;this.i++; }
        if(this.i == this.frames[this.k].getHeight()){ this.j=0;this.i=0;this.k++; }
    }

    private void reset(){ this.i=0;this.j=0;this.k=0; }

}