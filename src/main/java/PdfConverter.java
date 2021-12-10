import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

class PdfConverter {

  public static String handleInput(String line) {
    try {
      String[] splitted = line.split("\t");
      String command = splitted[0];
      String pdfUrl = splitted[1];
      String path = DownloadFile(pdfUrl);
      switch (command) {
        case "ToImage":
          return convertToImage(path);
        case "ToHTML":
          return convertToHTML(path);
        case "ToText":
          return convertToText(path);
        default:
          //do nothing
          break;
      }

    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "bad url";
  }

  private static String DownloadFile(String filePath) throws IOException{
    System.setProperty("http.agent", "Chrome");

    String pdfPath = "src/downloads/download.pdf";
    URL url = new URL(filePath);
    HttpURLConnection http = (HttpURLConnection)url.openConnection();
    BufferedInputStream in = new BufferedInputStream(http.getInputStream());
    FileOutputStream fos = new FileOutputStream(pdfPath);
    BufferedOutputStream bout = new BufferedOutputStream(fos);
    byte[] buffer = new byte[1024];
    int read = 0;
    while ((read = in.read(buffer, 0, 1024)) >= 0){
      bout.write(buffer, 0, read);
    }
    bout.close();
    in.close();
    System.out.println("Download pdf complete");
    return pdfPath;
  }




  private static String convertToImage(String inputPath) throws IOException {
    File file = new File(inputPath);
    PDDocument document = PDDocument.load(file);
    PDFRenderer renderer = new PDFRenderer(document);
    BufferedImage image = renderer.renderImage(0);
    String path = "src/output/image.png";
    ImageIO.write(image, "png", new File("src/output/image.png"));
    document.close();
    return path;
  }

  private static String convertToText(String inputPath) throws IOException {
    File file = new File(inputPath);
    PDDocument document = PDDocument.load(file);
    PDFTextStripper pdfStripper = new PDFTextStripper();
    pdfStripper.setStartPage(1);
    pdfStripper.setEndPage(1);
    String path = "src/output/text.txt";
    String text = pdfStripper.getText(document);
    try (PrintWriter fileDest = new PrintWriter(path)) {
      fileDest.println(text);
    }
    document.close();
    return path;
  }

  private static String convertToHTML(String inputPath) throws IOException {
    File file = new File(inputPath);
    PDDocument document = PDDocument.load(file);
    PDFTextStripper pdfStripper = new PDFTextStripper();
    pdfStripper.setStartPage(1);
    pdfStripper.setEndPage(1);
    String path = "src/output/htmlAsText.txt";
    String text = pdfStripper.getText(document);
    try (PrintWriter fileDest = new PrintWriter(path)) {
      fileDest.println("<div>");
      fileDest.println(text);
      fileDest.println("</div>");
    }
    document.close();
    return path;
  }

}

