import javax.imageio.ImageIO;
import java.io.File;

public class Dim {
    public static void main(String[] a) throws Exception {
        var img = ImageIO.read(new File(a[0]));
        System.out.println(img.getWidth() + "x" + img.getHeight());
    }
}
