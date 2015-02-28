package ee.joonasvali.stamps.ui;

import ee.joonasvali.stamps.code.Util;
import ee.joonasvali.stamps.meta.Metadata;
import ee.joonasvali.stamps.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Joonas Vali
 */
public class PaintingUI extends JPanel {

  public static Logger log = LoggerFactory.getLogger(PaintingUI.class);

  private final ExecutorService generalGeneratorExecutor = Executors.newSingleThreadExecutor();
  private final ProgressListener progressListener;
  private final PaintingController controller;
  private volatile BufferedImage lastImage;

  public PaintingUI(PaintingController controller, ProgressListener listener) {
    this.progressListener = listener;
    this.controller = controller;
    initEmpty();
    this.add(new JLabel(new ImageIcon(lastImage)));
  }

  private void initEmpty() {
    log.debug("Start initializing PaintingUI");
    lastImage = new BufferedImage(getPrefs().getWidth(), getPrefs().getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D) lastImage.getGraphics();
    g.setColor(Color.LIGHT_GRAY);
    int max = (int) (Runtime.getRuntime().maxMemory() / (1024 * 1024));
    String rating = "OK";
    if (max < 800) {
      rating = "low";
    } else if (max < 1000) {
      rating = "could use more";
    }

    int i = 35;
    int processors = Runtime.getRuntime().availableProcessors();
    g.drawString(Metadata.INSTANCE.getName() + " " + Metadata.INSTANCE.getVersion(), 50, i);
    g.drawString("Image size set to " + getPrefs().getWidth() + " : " + getPrefs().getHeight(), 50, i + 15);
    g.drawString("Total memory available to Java VM: " + max + " MB " + "(" + rating + ")", 50, i + 30);
    g.drawString("Number of processors available to Java VM: " + processors, 50, i + 45);
    if(AppProperties.getInstance().isLazyLoading()) {
      g.drawString("Using lazy loading for stamps (Slower but conserves memory).", 50, i + 60);
      i += 15;
    }
    g.drawString("Press \"Generate\" to generate your first image.", 50, i + 60);
    log.debug("Stop initializing PaintingUI");
  }

  public BufferedImage getLastImage() {
    return lastImage;
  }

  public void setRetainColors(boolean retainColors) {
    controller.setRetainColors(retainColors);
  }

  public void setRetainStamps(boolean retainStamps) {
    controller.setRetainStamps(retainStamps);
  }

  public void setRetainSpine(boolean retainSpine) {
    controller.setRetainSpine(retainSpine);
  }

  public void generate(final Runnable after) {
    generalGeneratorExecutor.execute(() -> {
      controller.clearCaches();
      lastImage = controller.generateImage(progressListener);
      SwingUtilities.invokeLater(after);
    });
  }

  public void commitImage() {
    Util.assertEDT();
    PaintingUI.this.removeAll();
    PaintingUI.this.add(new JLabel(new ImageIcon(lastImage)));
  }

  public Preferences getPrefs() {
    return controller.getPrefs();
  }
}
