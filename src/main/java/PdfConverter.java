import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

class PdfConverter {

  public static String handleInput(String line) {
    try {
      String[] splitted = line.split("\t");
      String command = splitted[0];
      String pdfUrl = splitted[1];
      File pdfFile = DownloadFile(pdfUrl);       //todo - 1

      switch (command) {
        case "ToImage":
          return convertToImage(pdfFile);
        case "ToHTML":
          return convertToHTML(pdfFile);
        case "ToText":
          return convertToText(pdfFile);
        default:
          //do nothing
          break;
      }

    }
    catch (Exception e) {
      e.printStackTrace();
      return "bad url " + e.getMessage() + "another info " + e;
    }
    return "bad url ";
  }


  private static File DownloadFile(String filePath) throws IOException{

    File pdf = new File(filePath.substring(filePath.lastIndexOf('/') + 1));
    URL url = new URL(filePath);
    HttpURLConnection http = (HttpURLConnection)url.openConnection();
    http.addRequestProperty("user-agent", "Mozilla/4.0");
    InputStream in = http.getInputStream();
    Files.copy(in, Paths.get(pdf.getPath(), new String[0]), new java.nio.file.CopyOption[0]);
    return pdf;
  }

  private static String convertToImage(File file) throws IOException {
    PDDocument document = PDDocument.load(file);
    PDFRenderer renderer = new PDFRenderer(document);
    BufferedImage image = renderer.renderImage(0);
    String fileName = UUID.randomUUID() + ".png";
    String path = "./" + fileName;
    ImageIO.write(image, "png", new File(path));
    document.close();
    return path;
  }

  public static String convertToText(File file) throws IOException {
    PDDocument document = PDDocument.load(file);
    PDFTextStripper pdfStripper = new PDFTextStripper();
    pdfStripper.setStartPage(1);
    pdfStripper.setEndPage(1);
    String fileName = UUID.randomUUID() + ".txt";
    String path = "./" + fileName;
    String text = pdfStripper.getText(document);
    try (PrintWriter fileDest = new PrintWriter(path)) {
      fileDest.println(text);
    }
    document.close();
    return path;
  }

  private static String convertToHTML(File file) throws IOException {
    PDDocument document = PDDocument.load(file);
    PDFTextStripper pdfStripper = new PDFTextStripper();
    pdfStripper.setStartPage(1);
    pdfStripper.setEndPage(1);
    String fileName = UUID.randomUUID() + ".txt";
    String path = "./" + fileName;

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

