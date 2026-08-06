package android.graphics;

public class BitmapFactory {
    public static Bitmap decodeResource(android.content.res.Resources res, int id) {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.File("/storage/internal_new/project/StudyTimer/app/src/main/res/drawable/mylogo.png"));
            return img == null ? null : new Bitmap(img);
        } catch (Exception e) {
            return null;
        }
    }
}
