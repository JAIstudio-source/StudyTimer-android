import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class CropMain {
    public static void main(String[] args) throws Exception {
        String in = args[0];
        String out = args[1];
        int x = Integer.parseInt(args[2]);
        int y = Integer.parseInt(args[3]);
        int w = Integer.parseInt(args[4]);
        int h = Integer.parseInt(args[5]);
        BufferedImage img = ImageIO.read(new File(in));
        BufferedImage c = img.getSubimage(x, y, w, h);
        ImageIO.write(c, "png", new File(out));
        System.out.println("crop " + out);
    }
}
