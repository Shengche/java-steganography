import java.io.File;

public class Helpers{
    public static String getFileExtension(String path) {
        if(path.lastIndexOf(".") != -1 && path.lastIndexOf(".") != 0)
            return path.substring(path.lastIndexOf(".")+1);
        else return "";
    }
    public static String getFileExtension(File file) {
        String filename = file.getName();
        try {
            return filename.substring(filename.lastIndexOf(".")+1);
        } catch (Exception e) {
            return "";
        }
    }    
    public static String setOutputFilename(String extension){
        String res = "secret";
        if (extension.equals(""))
            return res;
        return res+='.'+extension;
    }
}