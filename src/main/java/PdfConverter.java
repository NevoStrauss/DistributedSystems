import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

class PdfConverter {

  public static String handleInput(String line) {
    try {
      String[] splitted = line.split("\t");
      String command = splitted[0];
      String pdfUrl = splitted[1];

      File pdfFile = DownloadFile(pdfUrl);       //todo - 1
//      String path = DownloadFile(pdfUrl);       //todo - 1

      String tmpQ = "https://sqs.us-east-1.amazonaws.com/497378375097/tmpQ";     //todo - delete
      SQS sqs = new SQS();  //todo - delete
      sqs.sendMessage("path : hey ho", tmpQ);//todo - delete


      switch (command) {
        case "ToImage":
//          return convertToImage(path);
        case "ToHTML":
//          return convertToHTML(path);
        case "ToText":
//          return convertToText(path);
          return convertToText(pdfFile);
        default:
          //do nothing
          break;
      }

    }
    catch (Exception e) {
      e.printStackTrace();
      return "bad url " + e.getMessage().toString() + "another info " + e;
    }
    return "bad url ";
  }

//  private static String DownloadFile(String filePath) throws IOException{
//    System.setProperty("http.agent", "Chrome");
//
//    String pdfPath = "./" + UUID.randomUUID().toString() + ".pdf";
////    String pdfPath = "downloads/download" + UUID.randomUUID().toString() + ".pdf";
////    String pdfPath = "src/downloads/download.pdf";
//    URL url = new URL(filePath);
//    HttpURLConnection http = (HttpURLConnection)url.openConnection();
//    BufferedInputStream in = new BufferedInputStream(http.getInputStream());
//    FileOutputStream fos = new FileOutputStream(pdfPath);
//    BufferedOutputStream bout = new BufferedOutputStream(fos);
//    byte[] buffer = new byte[1024];
//    int read = 0;
//    while ((read = in.read(buffer, 0, 1024)) >= 0){
//      bout.write(buffer, 0, read);
//    }
//    bout.close();
//    in.close();
//    return pdfPath;
//  }

  private static File DownloadFile(String filePath) throws IOException{

    File pdf = new File(filePath.substring(filePath.lastIndexOf('/') + 1));
    URL url = new URL(filePath);
    HttpURLConnection http = (HttpURLConnection)url.openConnection();
    http.addRequestProperty("user-agent", "Mozilla/4.0");
    InputStream in = http.getInputStream();
    Files.copy(in, Paths.get(pdf.getPath(), new String[0]), new java.nio.file.CopyOption[0]);
    return pdf;
  }




  private static String convertToImage(String inputPath) throws IOException {
    File file = new File(inputPath);
    PDDocument document = PDDocument.load(file);
    PDFRenderer renderer = new PDFRenderer(document);
    BufferedImage image = renderer.renderImage(0);
    String fileName = UUID.randomUUID().toString() + ".png";
    String path = "./" + fileName;
//    String path = "output/image" + fileName;
//    String path = "src/output/image.png";
    ImageIO.write(image, "png", new File(path));
    document.close();
    return path;
  }

//  public static String convertToText(String inputPath) throws IOException {
  public static String convertToText(File file) throws IOException {
//    System.out.println("hey ho");
//    File file = new File(inputPath);
    PDDocument document = PDDocument.load(file);
    PDFTextStripper pdfStripper = new PDFTextStripper();
    pdfStripper.setStartPage(1);
    pdfStripper.setEndPage(1);
    String fileName = UUID.randomUUID().toString() + ".txt";
    String path = "./" + fileName;
//    String path = "output/text" + fileName;
//    String path = "src/output/text.txt";
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
    String fileName = UUID.randomUUID().toString() + ".txt";
    String path = "./" + fileName;
//    String path = "output/htmlAsText" + fileName;
//    String path = "src/output/htmlAsText.txt";
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

