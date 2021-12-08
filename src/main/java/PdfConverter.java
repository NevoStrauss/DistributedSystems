
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.fit.pdfdom.PDFDomTree;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;


public class PdfConverter {

  public static String handleInput(String line) {
    String[] splitted = line.split("\t");
    String action = splitted[0];
    String fileUrl = splitted[1];

    try {
      String path = "work";
      path = DownloadFile(fileUrl);
      if (!path.equals("work")) {
        switch (action) {
          case "ToImage":
            return convertToImage(path, "src/output/");
          case "ToHTML":
            return convertToHTML(path, "src/output/html.html");
          case "ToText":
            return convertToText(path, "src/output/pdf_as_text.txt");
          default:
            //do nothing
            break;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  private static String DownloadFile(String filePath) throws IOException {
    String ret = "src/output/pdf.pdf";
    URL url = new URL(filePath);
    InputStream is = url.openStream();
    ReadableByteChannel channel = Channels.newChannel(url.openStream());
    FileOutputStream fo = new FileOutputStream(new File(ret));
    fo.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
    fo.close();
    channel.close();
    return ret;
  }

  private static String convertToImage(String inputPath, String outputPath) throws IOException {
    PDDocument document = PDDocument.load(new File(inputPath));
    PDFRenderer pdfRenderer = new PDFRenderer(document);

    for (int page = 0; page < 1 /*document.getNumberOfPages() */; ++page) {
      BufferedImage bim = pdfRenderer.renderImageWithDPI(
        page, 300, ImageType.RGB);
      String ret = String.format("src/output/pdf-%d.%s", page + 1, "jpg");
      ImageIOUtil.writeImage(
        bim, ret, 300);
    }
    document.close();
    return outputPath;
  }

  private static String convertToText(String inputPath, String outputPath) throws IOException {
    System.out.println(inputPath);
    PDDocument document = PDDocument.load(new File(inputPath));
    AccessPermission ap = document.getCurrentAccessPermission();
    if (!ap.canExtractContent())
      throw new IOException("Dont have permissions");

    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);

    for (int p = 1; p < 2 /*document.getNumberOfPages()*/; ++p) {
      stripper.setStartPage(p);
      stripper.setEndPage(p);
      String text = stripper.getText(document);

      try {
        PrintWriter pw = new PrintWriter(outputPath);
        pw.print(text);
        pw.close();
      } catch (FileNotFoundException e) {
        System.out.println("File not found");
      }
    }
    return outputPath;
  }

  private static String convertToHTML(String inputPath, String outputPath) throws IOException, ParserConfigurationException {
    PDDocument doc = PDDocument.load(new File(inputPath));
    AccessPermission ap = doc.getCurrentAccessPermission();
    if (!ap.canExtractContent()) {
      throw new IOException("Dont have permissions");
    }
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);
    stripper.setStartPage(1);
    stripper.setEndPage(1);
    String text = stripper.getText(doc);
    PrintWriter pw = new PrintWriter(outputPath);
    PDFDomTree p = new PDFDomTree();
    p.setEndPage(1);
    p.writeText(doc, pw);
    doc.close();
    return outputPath;
  }

}